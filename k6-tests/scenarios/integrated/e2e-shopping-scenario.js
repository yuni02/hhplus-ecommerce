import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId, generateProductId } from '../../utils/config.js';

const errorRate = new Rate('errors');
const completeFlowSuccess = new Counter('complete_flow_success');
const completeFlowFailure = new Counter('complete_flow_failure');
const flowDuration = new Trend('complete_flow_duration');

export let options = {
    scenarios: {
        shopping_flow: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 20 },   // Ramp up
                { duration: '5m', target: 20 },   // Steady state
                { duration: '2m', target: 50 },   // Increase load
                { duration: '5m', target: 50 },   // Peak load
                { duration: '2m', target: 0 },    // Ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000', 'p(99)<4000'],
        http_req_failed: ['rate<0.1'],
        errors: ['rate<0.1'],
        complete_flow_duration: ['p(95)<10000'],
    },
};

export default function () {
    const userId = generateUserId();
    const flowStartTime = Date.now();
    let flowSuccess = true;

    // Step 1: Check user balance
    group('Check Balance', function () {
        const balanceRes = http.get(
            `${BASE_URL}/api/users/balance?userId=${userId}`,
            { headers: DEFAULT_HEADERS }
        );
        
        const balanceCheck = check(balanceRes, {
            'balance retrieved': (r) => r.status === 200 || r.status === 400,
        });
        
        if (!balanceCheck) {
            flowSuccess = false;
            errorRate.add(1);
        }
        
        sleep(1);
    });

    // Step 2: Charge balance if needed
    group('Charge Balance', function () {
        const chargeAmount = 100000; // Charge 100,000 points
        const chargePayload = JSON.stringify({
            userId: userId,
            amount: chargeAmount,
        });

        const chargeRes = http.post(
            `${BASE_URL}/api/users/balance/charge`,
            chargePayload,
            { headers: DEFAULT_HEADERS }
        );

        const chargeCheck = check(chargeRes, {
            'balance charged': (r) => r.status === 200,
        });

        if (!chargeCheck) {
            console.log(`Failed to charge balance for user ${userId}`);
            flowSuccess = false;
        }
        
        sleep(2);
    });

    // Step 3: Browse products
    group('Browse Products', function () {
        // View popular products
        const popularRes = http.get(
            `${BASE_URL}/api/products/popular`,
            { headers: DEFAULT_HEADERS }
        );

        check(popularRes, {
            'popular products loaded': (r) => r.status === 200,
        });

        // View specific product details
        const productId = generateProductId();
        const productRes = http.get(
            `${BASE_URL}/api/products/${productId}`,
            { headers: DEFAULT_HEADERS }
        );

        check(productRes, {
            'product details loaded': (r) => r.status === 200 || r.status === 404,
        });
        
        sleep(3);
    });

    // Step 4: Check user coupons
    let userCouponId = null;
    group('Check Coupons', function () {
        const couponsRes = http.get(
            `${BASE_URL}/api/coupons/users/${userId}`,
            { headers: DEFAULT_HEADERS }
        );

        if (couponsRes.status === 200) {
            try {
                const coupons = JSON.parse(couponsRes.body);
                if (Array.isArray(coupons) && coupons.length > 0) {
                    // Find unused coupon
                    const unusedCoupon = coupons.find(c => c.status === 'ISSUED');
                    if (unusedCoupon) {
                        userCouponId = unusedCoupon.userCouponId;
                    }
                }
            } catch (e) {
                console.log('Failed to parse coupons response');
            }
        }
        
        sleep(1);
    });

    // Step 5: Create order
    group('Create Order', function () {
        const orderItems = [
            { productId: 1, quantity: 2 },
            { productId: 2, quantity: 1 },
            { productId: 3, quantity: 3 }
        ];

        const orderPayload = JSON.stringify({
            userId: userId,
            orderItems: orderItems,
            userCouponId: userCouponId
        });

        const orderRes = http.post(
            `${BASE_URL}/api/orders`,
            orderPayload,
            { headers: DEFAULT_HEADERS, timeout: '10s' }
        );

        const orderCheck = check(orderRes, {
            'order created': (r) => r.status === 200,
            'order has ID': (r) => {
                if (r.status !== 200) return false;
                try {
                    const order = JSON.parse(r.body);
                    return order.orderId !== undefined;
                } catch (e) {
                    return false;
                }
            }
        });

        if (!orderCheck) {
            flowSuccess = false;
            errorRate.add(1);
            
            if (orderRes.status === 400) {
                const body = JSON.parse(orderRes.body);
                console.log(`Order failed for user ${userId}: ${body.message || body.error}`);
            }
        }
        
        sleep(2);
    });

    const flowEndTime = Date.now();
    const duration = flowEndTime - flowStartTime;
    flowDuration.add(duration);

    if (flowSuccess) {
        completeFlowSuccess.add(1);
    } else {
        completeFlowFailure.add(1);
    }

    sleep(Math.random() * 5 + 5); // 5-10 seconds before next flow
}
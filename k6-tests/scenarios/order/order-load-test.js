import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId, generateProductId } from '../../utils/config.js';

const errorRate = new Rate('errors');
const orderSuccess = new Counter('successful_orders');
const orderFailure = new Counter('failed_orders');
const orderDuration = new Trend('order_duration');

export let options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 15 },  // Ramp up to 15 users
                { duration: '30s', target: 25 },  // Increase to 25 users
                { duration: '15s', target: 0 },   // Ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        http_req_failed: ['rate<0.1'],
        errors: ['rate<0.1'],
        http_reqs: ['rate>30'],
        order_duration: ['p(95)<1500'],
    },
};

function chargeBalance(userId, amount) {
    const MAX_CHARGE_AMOUNT = 1000000; // 1회 최대 충전 금액 100만원
    let remainingAmount = amount;
    let chargeSuccess = true;
    let retryCount = 0;
    const MAX_RETRIES = 3;
    
    while (remainingAmount > 0 && chargeSuccess) {
        const chargeAmount = Math.min(remainingAmount, MAX_CHARGE_AMOUNT);
        const chargePayload = JSON.stringify({
            userId: userId,
            amount: chargeAmount
        });

        const response = http.post(
            `${BASE_URL}/api/users/balance/charge`,
            chargePayload,
            { headers: DEFAULT_HEADERS, timeout: '5s' }
        );

        if (response.status === 200) {
            remainingAmount -= chargeAmount;
            retryCount = 0; // 성공 시 재시도 카운트 리셋
        } else if (response.status === 500 && retryCount < MAX_RETRIES) {
            // 서버 오류 시 재시도 (동시성 충돌 등)
            retryCount++;
            sleep(0.1 * retryCount); // 점진적 대기
            continue;
        } else {
            chargeSuccess = false;
            // 충전 실패 시 상세 로그는 제거 (너무 많은 로그 방지)
            break;
        }
    }

    return chargeSuccess && remainingAmount === 0;
}

function getBalance(userId) {
    const response = http.get(
        `${BASE_URL}/api/users/balance?userId=${userId}`,
        { headers: DEFAULT_HEADERS, timeout: '5s' }
    );
    
    if (response.status === 200) {
        try {
            const body = JSON.parse(response.body);
            return body.balance || 0;
        } catch (e) {
            return 0;
        }
    } else if (response.status === 400 || response.status === 404) {
        // 사용자가 없거나 잔액이 없는 경우 0 반환
        return 0;
    }
    return 0;
}

function createOrder() {
    const userId = generateUserId();
    
    // Check current balance
    const currentBalance = getBalance(userId);
    
    // Calculate required amount (최대 상품가격 * 수량 고려)
    const requiredAmount = 10000000; // 1000만원 (충분한 금액)
    
    if (currentBalance < requiredAmount) {
        const chargeAmount = requiredAmount - currentBalance;
        const charged = chargeBalance(userId, chargeAmount);
        
        if (!charged) {
            console.log(`Failed to charge balance for user ${userId}. Current: ${currentBalance}, Required: ${requiredAmount}`);
        }
        
        // 충전 후 잔액 다시 확인
        const newBalance = getBalance(userId);
        if (newBalance < requiredAmount) {
            console.log(`Balance still insufficient after charge attempt. User ${userId}: ${newBalance}`);
        }
    }
    
    const orderItems = [];
    
    // Generate 1-3 order items
    const itemCount = Math.floor(Math.random() * 3) + 1;
    for (let i = 0; i < itemCount; i++) {
        orderItems.push({
            productId: generateProductId(),
            quantity: Math.floor(Math.random() * 5) + 1
        });
    }

    const payload = JSON.stringify({
        userId: userId,
        orderItems: orderItems,
        userCouponId: Math.random() > 0.7 ? Math.floor(Math.random() * 10) + 1 : null // 30% chance to use coupon
    });

    const params = {
        headers: DEFAULT_HEADERS,
        timeout: '10s',
    };

    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/orders`, payload, params);
    const duration = Date.now() - startTime;
    
    orderDuration.add(duration);

    return { response, userId, duration };
}

export default function () {
    const { response, userId, duration } = createOrder();

    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'order created successfully': (r) => {
            if (r.status !== 200) {
                console.log(`Order response status ${r.status}: ${r.body}`);
                return false;
            }
            try {
                const body = JSON.parse(r.body);
                // 주문 응답 형식이 다를 수 있음 - id 또는 orderId
                const hasOrderId = (body.orderId !== undefined) || (body.id !== undefined);
                const orderStatus = body.status || body.orderStatus;
                const isValidStatus = orderStatus === 'PENDING' || orderStatus === 'COMPLETED';
                
                if (!hasOrderId || !isValidStatus) {
                    console.log(`Order response issue - orderId: ${body.orderId || body.id}, status: ${orderStatus}, body: ${JSON.stringify(body)}`);
                }
                return hasOrderId && isValidStatus;
            } catch (e) {
                console.log(`Failed to parse order response: ${r.body}`);
                return false;
            }
        },
        'response time < 2s': () => duration < 2000,
    });

    if (success) {
        orderSuccess.add(1);
    } else {
        orderFailure.add(1);
        errorRate.add(1);
        
        if (response.status === 400) {
            const body = JSON.parse(response.body);
            console.log(`Order failed for user ${userId}: ${body.message || body.error}`);
        } else if (response.status >= 500) {
            console.log(`Server error for user ${userId}: ${response.status}`);
        }
    }

    sleep(Math.random() * 3 + 2); // 2-5 seconds between orders
}
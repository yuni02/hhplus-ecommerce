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
                { duration: '2m', target: 30 },   // Ramp up to 30 users
                { duration: '5m', target: 30 },   // Stay at 30 users
                { duration: '2m', target: 60 },   // Ramp up to 60 users
                { duration: '5m', target: 60 },   // Stay at 60 users
                { duration: '2m', target: 0 },    // Ramp down
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

function createOrder() {
    const userId = generateUserId();
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
            if (r.status !== 200) return false;
            try {
                const body = JSON.parse(r.body);
                return body.orderId !== undefined && body.status === 'PENDING';
            } catch (e) {
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
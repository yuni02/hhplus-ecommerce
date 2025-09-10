import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId, generateProductId } from '../../utils/config.js';

const errorRate = new Rate('errors');
const timeouts = new Counter('timeouts');
const stockErrors = new Counter('stock_errors');
const balanceErrors = new Counter('balance_errors');

export let options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 50 },    // Quick ramp up
                { duration: '2m', target: 100 },   // Increase load
                { duration: '2m', target: 200 },   // High load
                { duration: '3m', target: 300 },   // Very high load
                { duration: '3m', target: 300 },   // Sustain stress
                { duration: '2m', target: 0 },     // Ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000', 'p(99)<5000'],
        http_req_failed: ['rate<0.2'],
        errors: ['rate<0.2'],
        timeouts: ['count<100'],
    },
};

export default function () {
    const userId = generateUserId();
    
    // Stress test with potentially conflicting orders (same products)
    const popularProductIds = [1, 2, 3, 4, 5]; // Popular products likely to cause contention
    const orderItems = [];
    
    const itemCount = Math.floor(Math.random() * 5) + 1;
    for (let i = 0; i < itemCount; i++) {
        const productId = Math.random() > 0.3 
            ? popularProductIds[Math.floor(Math.random() * popularProductIds.length)]
            : generateProductId();
        
        orderItems.push({
            productId: productId,
            quantity: Math.floor(Math.random() * 10) + 1 // Higher quantities for stress
        });
    }

    const payload = JSON.stringify({
        userId: userId,
        orderItems: orderItems,
        userCouponId: null
    });

    const params = {
        headers: DEFAULT_HEADERS,
        timeout: '15s',
    };

    const response = http.post(`${BASE_URL}/api/orders`, payload, params);

    const success = check(response, {
        'request completed': (r) => r.status !== 0,
        'no server error': (r) => r.status < 500,
    });

    if (response.status === 0) {
        timeouts.add(1);
        console.log(`Timeout for user ${userId}`);
    } else if (response.status === 400) {
        const body = JSON.parse(response.body);
        const message = body.message || body.error || '';
        
        if (message.includes('재고') || message.includes('stock')) {
            stockErrors.add(1);
        } else if (message.includes('잔액') || message.includes('balance')) {
            balanceErrors.add(1);
        }
    } else if (response.status >= 500) {
        errorRate.add(1);
        console.log(`Server error: ${response.status}`);
    }

    sleep(Math.random() * 1 + 0.5); // 0.5-1.5 seconds between requests
}
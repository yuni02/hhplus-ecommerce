import http from 'k6/http';
import { check } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId, generateProductId } from '../../utils/config.js';

const errorRate = new Rate('errors');
const peakOrders = new Counter('peak_orders');
const responseTime = new Trend('peak_response_time');

export let options = {
    scenarios: {
        peak_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30 },    // Warm up
                { duration: '10s', target: 300 },   // Sudden spike (Black Friday simulation)
                { duration: '1m', target: 300 },    // Maintain peak
                { duration: '10s', target: 50 },    // Quick drop
                { duration: '30s', target: 50 },    // Normal load
                { duration: '5s', target: 500 },    // Extreme spike (flash sale)
                { duration: '30s', target: 500 },   // Maintain extreme peak
                { duration: '30s', target: 0 },     // End
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<5000', 'p(99)<10000'],
        http_req_failed: ['rate<0.3'],
        errors: ['rate<0.3'],
    },
};

export default function () {
    const userId = generateUserId();
    
    // Simulate flash sale - everyone ordering the same hot items
    const flashSaleProducts = [1, 2, 3]; // Limited hot products
    const orderItems = [{
        productId: flashSaleProducts[Math.floor(Math.random() * flashSaleProducts.length)],
        quantity: Math.floor(Math.random() * 3) + 1
    }];

    const payload = JSON.stringify({
        userId: userId,
        orderItems: orderItems,
        userCouponId: Math.floor(Math.random() * 5) + 1 // Everyone trying to use coupons
    });

    const params = {
        headers: DEFAULT_HEADERS,
        timeout: '10s',
    };

    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/orders`, payload, params);
    const duration = Date.now() - startTime;
    
    responseTime.add(duration);
    peakOrders.add(1);

    const success = check(response, {
        'handled request': (r) => r.status !== 0,
        'acceptable response': (r) => r.status === 200 || r.status === 400,
    });

    if (!success) {
        errorRate.add(1);
        console.log(`Peak failure - Status: ${response.status}, Duration: ${duration}ms`);
    }
}
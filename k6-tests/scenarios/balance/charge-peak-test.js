import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId } from '../../utils/config.js';

const errorRate = new Rate('errors');

export let options = {
    scenarios: {
        peak_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },    // Warm up
                { duration: '10s', target: 500 },   // Sudden spike to 500 users
                { duration: '1m', target: 500 },    // Maintain peak load
                { duration: '10s', target: 50 },    // Quick drop
                { duration: '30s', target: 50 },    // Normal load
                { duration: '10s', target: 1000 },  // Extreme spike
                { duration: '30s', target: 1000 },  // Maintain extreme load
                { duration: '30s', target: 0 },     // Ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000', 'p(99)<5000'],
        http_req_failed: ['rate<0.2'],
        errors: ['rate<0.2'],
    },
};

export default function () {
    const userId = generateUserId();
    const amount = Math.floor(Math.random() * 50000) + 10000;

    const payload = JSON.stringify({
        userId: userId,
        amount: amount,
    });

    const params = {
        headers: DEFAULT_HEADERS,
        timeout: '5s',
    };

    const response = http.post(`${BASE_URL}/api/users/balance/charge`, payload, params);

    const success = check(response, {
        'request completed': (r) => r.status !== 0,
        'no server error': (r) => r.status < 500,
    });

    errorRate.add(!success);

    if (!success) {
        console.log(`Peak test failure - User: ${userId}, Status: ${response.status}`);
    }
}
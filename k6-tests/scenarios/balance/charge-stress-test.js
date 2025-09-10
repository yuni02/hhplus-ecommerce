import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId } from '../../utils/config.js';

const errorRate = new Rate('errors');

export let options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 100 },  // Ramp up to 100 users
                { duration: '3m', target: 200 },  // Ramp up to 200 users
                { duration: '3m', target: 300 },  // Ramp up to 300 users
                { duration: '3m', target: 400 },  // Ramp up to 400 users
                { duration: '3m', target: 500 },  // Ramp up to 500 users (stress point)
                { duration: '5m', target: 500 },  // Stay at 500 users
                { duration: '3m', target: 0 },    // Ramp down to 0 users
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        http_req_failed: ['rate<0.1'],
        errors: ['rate<0.1'],
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
        timeout: '10s',
    };

    const response = http.post(`${BASE_URL}/api/users/balance/charge`, payload, params);

    const success = check(response, {
        'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
        'no timeout': (r) => r.status !== 0,
    });

    errorRate.add(!success);

    if (response.status === 0) {
        console.log(`Timeout for user ${userId}`);
    } else if (response.status >= 500) {
        console.log(`Server error for user ${userId}: ${response.status}`);
    }

    sleep(Math.random() * 0.5 + 0.5); // 0.5-1 second between requests
}
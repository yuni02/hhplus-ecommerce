import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, THRESHOLDS, generateUserId } from '../../utils/config.js';

const errorRate = new Rate('errors');

export let options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 50 },   // Ramp up to 50 users
                { duration: '5m', target: 50 },   // Stay at 50 users  
                { duration: '2m', target: 100 },  // Ramp up to 100 users
                { duration: '5m', target: 100 },  // Stay at 100 users
                { duration: '2m', target: 0 },    // Ramp down to 0 users
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.05'],
        errors: ['rate<0.05'],
        http_reqs: ['rate>50'],
    },
};

export default function () {
    // VU 번호를 기반으로 고유한 사용자 ID 생성 (중복 방지)
    const userId = 1001 + (__VU % 1000);  // VU 1 -> user 1001, VU 2 -> user 1002, ...
    const amount = Math.floor(Math.random() * 50000) + 10000; // 10,000 ~ 60,000

    const payload = JSON.stringify({
        userId: userId,
        amount: amount,
    });

    const params = {
        headers: DEFAULT_HEADERS,
    };

    const response = http.post(`${BASE_URL}/api/users/balance/charge`, payload, params);

    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response has balanceAfterCharge': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.balanceAfterCharge !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    errorRate.add(!success);

    if (!success) {
        console.log(`Failed request for user ${userId}: ${response.status} - ${response.body}`);
    }

    sleep(Math.random() * 2 + 1); // 1-3 seconds between requests
}
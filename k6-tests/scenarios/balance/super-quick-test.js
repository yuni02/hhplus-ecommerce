import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS } from '../../utils/config.js';

const errorRate = new Rate('errors');

export let options = {
    scenarios: {
        super_quick: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 10 },  // 15초 동안 10명으로 증가
                { duration: '30s', target: 20 },  // 30초 동안 20명 유지
                { duration: '15s', target: 0 },   // 15초 동안 0명으로 감소
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        http_req_failed: ['rate<0.05'],
        errors: ['rate<0.05'],
    },
};

export default function () {
    const userId = 1001 + (__VU % 200);
    const amount = Math.floor(Math.random() * 15000) + 5000;

    const response = http.post(`${BASE_URL}/api/users/balance/charge`, 
        JSON.stringify({ userId, amount }), {
        headers: DEFAULT_HEADERS,
    });

    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'has balanceAfterCharge': (r) => {
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
        console.log(`❌ User ${userId}: ${response.status} - ${response.body?.substr(0, 100)}`);
    }

    sleep(Math.random() * 1 + 1); // 1-2초 대기
}
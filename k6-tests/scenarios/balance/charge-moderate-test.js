import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS } from '../../utils/config.js';

const errorRate = new Rate('errors');

export let options = {
    scenarios: {
        moderate_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },  // 0→10명 (점진적 증가)
                { duration: '2m', target: 10 },   // 10명 유지
                { duration: '30s', target: 25 },  // 10→25명
                { duration: '2m', target: 25 },   // 25명 유지  
                { duration: '30s', target: 0 },   // 감소
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<800', 'p(99)<1500'],  // 더 현실적 목표
        http_req_failed: ['rate<0.05'],
        errors: ['rate<0.05'],
        http_reqs: ['rate>15'],  // 25명 × 0.6 req/s = 15 req/s
    },
};

export default function () {
    // VU별 고유 사용자 ID
    const userId = 1001 + (__VU % 500);  // 500명 중 순환
    const amount = Math.floor(Math.random() * 20000) + 5000; // 5K-25K

    const payload = JSON.stringify({
        userId: userId,
        amount: amount,
    });

    const response = http.post(`${BASE_URL}/api/users/balance/charge`, payload, {
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
        console.log(`❌ User ${userId}: ${response.status} - ${response.body}`);
    } else {
        console.log(`✅ User ${userId}: Balance charged successfully`);
    }

    sleep(Math.random() * 1 + 1.5); // 1.5-2.5초 대기 (더 현실적)
}
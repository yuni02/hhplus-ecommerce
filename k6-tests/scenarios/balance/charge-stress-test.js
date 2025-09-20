import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS } from '../../utils/config.js';

const errorRate = new Rate('errors');

export let options = {
    scenarios: {
        realistic_stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 20 },  // 0→20명 (15초)
                { duration: '30s', target: 40 },  // 20→40명 (30초)
                { duration: '15s', target: 0 },   // 감소 (15초)
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1500', 'p(99)<3000'],
        http_req_failed: ['rate<0.15'],  // 15% 실패율까지 허용
        errors: ['rate<0.15'],
    },
};

export default function () {
    // 충분한 사용자 풀로 중복 최소화
    const userId = 1001 + (__VU % 2000);  // 2000명 풀
    const amount = Math.floor(Math.random() * 30000) + 5000;

    const response = http.post(`${BASE_URL}/api/users/balance/charge`, 
        JSON.stringify({ userId, amount }), {
        headers: DEFAULT_HEADERS,
        timeout: '15s',  // 타임아웃 증가
    });

    const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'no server error': (r) => r.status < 500,
    });

    errorRate.add(!success);

    if (response.status >= 500) {
        console.log(`🔥 Server overload for user ${userId}: ${response.status}`);
    } else if (response.status === 200) {
        console.log(`✅ User ${userId}: Success`);
    }

    sleep(Math.random() * 1.5 + 1); // 1-2.5초 대기
}
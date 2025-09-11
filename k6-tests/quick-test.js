import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export let options = {
    stages: [
        { duration: '30s', target: 10 },   // 10명으로 증가
        { duration: '1m', target: 10 },    // 1분간 유지
        { duration: '30s', target: 0 },    // 0명으로 감소
    ],
};

export default function () {
    const userId = 1001 + (__VU % 100);
    const amount = Math.floor(Math.random() * 10000) + 5000;

    const payload = JSON.stringify({
        userId: userId,
        amount: amount,
    });

    const response = http.post('http://localhost:8083/api/users/balance/charge', payload, {
        headers: { 'Content-Type': 'application/json' },
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
    sleep(Math.random() * 2 + 1);
}
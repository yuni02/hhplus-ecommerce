import http from 'k6/http';
import { check } from 'k6';

export let options = {
    vus: 5,
    duration: '30s',
};

export default function () {
    const userId = 1001 + __VU;
    const amount = 5000;

    const response = http.post('http://localhost:8083/api/users/balance/charge', 
        JSON.stringify({ userId, amount }), {
        headers: { 'Content-Type': 'application/json' },
    });

    check(response, {
        'status is 200': (r) => r.status === 200,
    });
}
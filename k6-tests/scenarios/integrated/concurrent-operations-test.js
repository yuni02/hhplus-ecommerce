import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS } from '../../utils/config.js';
import { SharedArray } from 'k6/data';

const errorRate = new Rate('errors');
const deadlockCounter = new Counter('potential_deadlocks');
const raceConditionCounter = new Counter('race_conditions');

// Shared test data for concurrent operations
const testUsers = new SharedArray('users', function () {
    const users = [];
    for (let i = 1; i <= 100; i++) {
        users.push(i);
    }
    return users;
});

const testProducts = new SharedArray('products', function () {
    return [1, 2, 3, 4, 5]; // Limited products to increase contention
});

export let options = {
    scenarios: {
        // Multiple scenarios running concurrently to create contention
        balance_operations: {
            executor: 'constant-vus',
            vus: 10,
            duration: '20s',
            exec: 'balanceOperations',
        },
        order_operations: {
            executor: 'constant-vus',
            vus: 15,
            duration: '20s',
            exec: 'orderOperations',
            startTime: '10s',
        },
        coupon_operations: {
            executor: 'constant-vus',
            vus: 8,
            duration: '20s',
            exec: 'couponOperations',
            startTime: '20s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000'],
        errors: ['rate<0.2'],
        potential_deadlocks: ['count<10'],
    },
};

// Scenario 1: Concurrent balance operations
export function balanceOperations() {
    const userId = testUsers[Math.floor(Math.random() * testUsers.length)];
    
    group('Concurrent Balance Operations', function () {
        // Multiple threads trying to charge/use balance for same user
        const operations = [
            () => chargeBalance(userId, 50000),
            () => useBalance(userId, 30000),
            () => chargeBalance(userId, 20000),
        ];
        
        const operation = operations[Math.floor(Math.random() * operations.length)];
        operation();
    });
    
    sleep(0.5);
}

// Scenario 2: Concurrent order operations
export function orderOperations() {
    const userId = testUsers[Math.floor(Math.random() * testUsers.length)];
    const productId = testProducts[Math.floor(Math.random() * testProducts.length)];
    
    group('Concurrent Order Operations', function () {
        // Multiple users ordering same products simultaneously
        const orderPayload = JSON.stringify({
            userId: userId,
            orderItems: [
                { productId: productId, quantity: Math.floor(Math.random() * 5) + 1 }
            ],
            userCouponId: null
        });
        
        const response = http.post(
            `${BASE_URL}/api/orders`,
            orderPayload,
            { headers: DEFAULT_HEADERS, timeout: '10s' }
        );
        
        const success = check(response, {
            'order processed': (r) => r.status === 200 || r.status === 400,
            'no timeout': (r) => r.status !== 0,
        });
        
        if (!success) {
            errorRate.add(1);
            if (response.status === 0) {
                deadlockCounter.add(1);
                console.log(`Potential deadlock detected for user ${userId}`);
            }
        }
        
        // Check for race conditions
        if (response.status === 400) {
            const body = JSON.parse(response.body);
            if (body.message && body.message.includes('재고')) {
                raceConditionCounter.add(1);
            }
        }
    });
    
    sleep(0.3);
}

// Scenario 3: Concurrent coupon operations
export function couponOperations() {
    const userId = testUsers[Math.floor(Math.random() * testUsers.length)];
    const couponId = Math.floor(Math.random() * 5) + 1;
    
    group('Concurrent Coupon Operations', function () {
        // Multiple users trying to issue same limited coupon
        const response = http.post(
            `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`,
            null,
            { headers: DEFAULT_HEADERS }
        );
        
        check(response, {
            'coupon request processed': (r) => 
                r.status === 200 || r.status === 202 || r.status === 400,
        });
    });
    
    sleep(1);
}

// Helper functions
function chargeBalance(userId, amount) {
    const payload = JSON.stringify({ userId, amount });
    const response = http.post(
        `${BASE_URL}/api/users/balance/charge`,
        payload,
        { headers: DEFAULT_HEADERS }
    );
    
    check(response, {
        'balance charged': (r) => r.status === 200,
    });
    
    return response;
}

function useBalance(userId, amount) {
    // Simulate balance usage through order
    const orderPayload = JSON.stringify({
        userId: userId,
        orderItems: [{ productId: 1, quantity: 1 }],
        userCouponId: null
    });
    
    const response = http.post(
        `${BASE_URL}/api/orders`,
        orderPayload,
        { headers: DEFAULT_HEADERS }
    );
    
    return response;
}
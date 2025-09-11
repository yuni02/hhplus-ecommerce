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
    for (let i = 1001; i <= 1100; i++) { // 실제 존재하는 사용자 ID 범위로 변경
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
        // 금액을 100만원 이하로 조정
        const operations = [
            () => chargeBalance(userId, 500000),  // 50만원
            () => useBalance(userId, 100000),     // 10만원 상당 주문
            () => chargeBalance(userId, 200000),  // 20만원
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
        // 주문 전 잔액 확인 및 충전
        const balanceRes = http.get(
            `${BASE_URL}/api/users/balance?userId=${userId}`,
            { headers: DEFAULT_HEADERS, timeout: '5s' }
        );
        
        let currentBalance = 0;
        if (balanceRes.status === 200) {
            try {
                const body = JSON.parse(balanceRes.body);
                currentBalance = body.balance || 0;
            } catch (e) {}
        }
        
        // 잔액이 500만원 미만이면 충전
        if (currentBalance < 5000000) {
            const chargeAmount = Math.min(1000000, 5000000 - currentBalance);
            chargeBalance(userId, chargeAmount);
            sleep(0.1); // 충전 완료 대기
        }
        
        // Multiple users ordering same products simultaneously
        const orderPayload = JSON.stringify({
            userId: userId,
            orderItems: [
                { productId: productId, quantity: Math.floor(Math.random() * 3) + 1 } // 수량 줄임 (1-3개)
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
            try {
                const body = JSON.parse(response.body);
                if (body.message && body.message.includes('재고')) {
                    raceConditionCounter.add(1);
                }
            } catch (e) {}
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
    const MAX_CHARGE_AMOUNT = 1000000; // 1회 최대 충전 금액 100만원
    let remainingAmount = Math.min(amount, MAX_CHARGE_AMOUNT); // 100만원 이하로 제한
    let chargeSuccess = true;
    let retryCount = 0;
    const MAX_RETRIES = 3;
    
    while (remainingAmount > 0 && chargeSuccess && retryCount <= MAX_RETRIES) {
        const chargeAmount = Math.min(remainingAmount, MAX_CHARGE_AMOUNT);
        const payload = JSON.stringify({ userId, amount: chargeAmount });
        
        const response = http.post(
            `${BASE_URL}/api/users/balance/charge`,
            payload,
            { headers: DEFAULT_HEADERS, timeout: '10s' }
        );
        
        if (response.status === 200) {
            remainingAmount -= chargeAmount;
            retryCount = 0; // 성공 시 재시도 카운트 리셋
        } else if (response.status === 500 && retryCount < MAX_RETRIES) {
            // 서버 오류 시 재시도 (동시성 충돌 등)
            retryCount++;
            sleep(0.1 * retryCount); // 점진적 대기
            continue;
        } else {
            chargeSuccess = false;
            break;
        }
    }
    
    const success = check(chargeSuccess, {
        'balance charged': () => chargeSuccess && remainingAmount === 0,
    });
    
    return { success: chargeSuccess, remainingAmount };
}

function useBalance(userId, amount) {
    // 먼저 충분한 잔액이 있는지 확인하고 없으면 충전
    const balanceRes = http.get(
        `${BASE_URL}/api/users/balance?userId=${userId}`,
        { headers: DEFAULT_HEADERS, timeout: '5s' }
    );
    
    let currentBalance = 0;
    if (balanceRes.status === 200) {
        try {
            const body = JSON.parse(balanceRes.body);
            currentBalance = body.balance || 0;
        } catch (e) {}
    }
    
    // 잔액이 부족하면 충전
    if (currentBalance < amount) {
        const chargeAmount = Math.min(1000000, amount - currentBalance + 100000); // 여유분 추가
        chargeBalance(userId, chargeAmount);
    }
    
    // 소액 주문으로 잔액 사용 시뮬레이션
    const orderPayload = JSON.stringify({
        userId: userId,
        orderItems: [{ productId: 20, quantity: 1 }], // 가장 저렴한 상품 (Power Bank 5.9만원)
        userCouponId: null
    });
    
    const response = http.post(
        `${BASE_URL}/api/orders`,
        orderPayload,
        { headers: DEFAULT_HEADERS, timeout: '10s' }
    );
    
    check(response, {
        'balance used (order created)': (r) => r.status === 200 || r.status === 400,
    });
    
    return response;
}
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId, generateProductId } from '../../utils/config.js';

const errorRate = new Rate('errors');
const memoryLeakIndicator = new Trend('response_time_degradation');
const successfulTransactions = new Counter('successful_transactions');
const failedTransactions = new Counter('failed_transactions');

export let options = {
    scenarios: {
        endurance_test: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30m', // 30 minutes for demo, typically 2-24 hours
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.05'],
        errors: ['rate<0.05'],
        response_time_degradation: ['avg<100'], // Response time shouldn't degrade over time
    },
};

let baselineResponseTime = null;
let requestCount = 0;

export default function () {
    requestCount++;
    
    // Rotate through different operations to simulate real usage
    const operations = [
        checkBalance,
        chargeBalance,
        browseProducts,
        viewPopularProducts,
        createOrder,
        checkCoupons,
    ];
    
    const operation = operations[requestCount % operations.length];
    const startTime = Date.now();
    const success = operation();
    const responseTime = Date.now() - startTime;
    
    // Track response time degradation (potential memory leak indicator)
    if (requestCount === 100) {
        // Set baseline after warm-up (100 requests)
        baselineResponseTime = responseTime;
    } else if (baselineResponseTime && requestCount > 100) {
        const degradation = responseTime - baselineResponseTime;
        memoryLeakIndicator.add(degradation);
        
        // Log if significant degradation detected
        if (degradation > 500 && requestCount % 1000 === 0) {
            console.log(`Performance degradation detected after ${requestCount} requests: +${degradation}ms`);
        }
    }
    
    if (success) {
        successfulTransactions.add(1);
    } else {
        failedTransactions.add(1);
        errorRate.add(1);
    }
    
    sleep(Math.random() * 2 + 1); // 1-3 seconds between operations
}

function checkBalance() {
    const userId = generateUserId();
    const response = http.get(
        `${BASE_URL}/api/users/balance?userId=${userId}`,
        { headers: DEFAULT_HEADERS }
    );
    
    return check(response, {
        'balance check successful': (r) => r.status === 200 || r.status === 400,
    });
}

function chargeBalance() {
    const userId = generateUserId();
    const amount = Math.floor(Math.random() * 50000) + 10000;
    
    const payload = JSON.stringify({ userId, amount });
    const response = http.post(
        `${BASE_URL}/api/users/balance/charge`,
        payload,
        { headers: DEFAULT_HEADERS }
    );
    
    return check(response, {
        'charge successful': (r) => r.status === 200,
    });
}

function browseProducts() {
    const productId = generateProductId();
    const response = http.get(
        `${BASE_URL}/api/products/${productId}`,
        { headers: DEFAULT_HEADERS }
    );
    
    return check(response, {
        'product loaded': (r) => r.status === 200 || r.status === 404,
    });
}

function viewPopularProducts() {
    const response = http.get(
        `${BASE_URL}/api/products/popular`,
        { headers: DEFAULT_HEADERS }
    );
    
    return check(response, {
        'popular products loaded': (r) => r.status === 200,
    });
}

function createOrder() {
    const userId = generateUserId();
    const orderItems = [];
    const itemCount = Math.floor(Math.random() * 3) + 1;
    
    for (let i = 0; i < itemCount; i++) {
        orderItems.push({
            productId: generateProductId(),
            quantity: Math.floor(Math.random() * 3) + 1
        });
    }
    
    const payload = JSON.stringify({
        userId: userId,
        orderItems: orderItems,
        userCouponId: null
    });
    
    const response = http.post(
        `${BASE_URL}/api/orders`,
        payload,
        { headers: DEFAULT_HEADERS, timeout: '10s' }
    );
    
    return check(response, {
        'order processed': (r) => r.status === 200 || r.status === 400,
    });
}

function checkCoupons() {
    const userId = generateUserId();
    const response = http.get(
        `${BASE_URL}/api/coupons/users/${userId}`,
        { headers: DEFAULT_HEADERS }
    );
    
    return check(response, {
        'coupons retrieved': (r) => r.status === 200,
    });
}

export function handleSummary(data) {
    console.log('\n=== Endurance Test Summary ===\n');
    console.log(`Total Requests: ${requestCount}`);
    console.log(`Duration: ${options.scenarios.endurance_test.duration}`);
    console.log(`Virtual Users: ${options.scenarios.endurance_test.vus}`);
    
    if (baselineResponseTime) {
        const avgDegradation = data.metrics.response_time_degradation?.values?.avg || 0;
        console.log(`\nPerformance Degradation: ${avgDegradation.toFixed(2)}ms`);
        
        if (avgDegradation > 100) {
            console.log('⚠️  WARNING: Significant performance degradation detected!');
            console.log('   Possible causes: Memory leak, connection pool exhaustion, cache issues');
        } else {
            console.log('✅ Performance remained stable during the test');
        }
    }
    
    return {
        'stdout': JSON.stringify(data, null, 2),
    };
}
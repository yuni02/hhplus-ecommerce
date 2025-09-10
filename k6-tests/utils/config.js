export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';

export const THRESHOLDS = {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.1'],
    http_reqs: ['rate>10']
};

export const DEFAULT_HEADERS = {
    'Content-Type': 'application/json',
};

export function generateUserId() {
    // 실제 존재하는 사용자 ID 범위: 1001-2000 (1000명)
    return Math.floor(Math.random() * 1000) + 1001;
}

export function generateProductId() {
    // 실제 존재하는 상품 ID 범위: 1-20
    return Math.floor(Math.random() * 20) + 1;
}

export function generateCouponId() {
    // 실제 존재하는 쿠폰 ID 범위: 1-10
    return Math.floor(Math.random() * 10) + 1;
}

export function handleResponse(response, expectedStatus = 200) {
    if (response.status !== expectedStatus) {
        console.error(`Request failed: ${response.status} - ${response.body}`);
    }
    return response;
}
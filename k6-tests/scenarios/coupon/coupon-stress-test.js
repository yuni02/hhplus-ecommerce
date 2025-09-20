import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId } from '../../utils/config.js';

const errorRate = new Rate('errors');
const couponSuccess = new Counter('coupon_success');
const couponFailed = new Counter('coupon_failed');
const issueDuration = new Trend('issue_duration');
const statusCheckDuration = new Trend('status_check_duration');

export let options = {
    scenarios: {
        // 극한 스트레스 테스트
        extreme_stress: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 2000,
            stages: [
                { duration: '30s', target: 100 },   // 초당 100개 요청
                { duration: '1m', target: 500 },    // 초당 500개 요청
                { duration: '30s', target: 1000 },  // 초당 1000개 요청 (극한)
                { duration: '30s', target: 100 },   // 다시 감소
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<3000', 'p(99)<10000'],
        http_req_failed: ['rate<0.8'], // 스트레스 테스트이므로 80% 미만
        'http_req_duration{name:IssueCoupon}': ['p(95)<2000'],
        'http_req_duration{name:CheckStatus}': ['p(95)<1000'],
    },
};

// 쿠폰 발급 및 상태 확인 플로우
function couponIssueFlow(couponId, userId) {
    const results = {};
    
    // 1. 쿠폰 발급 요청
    group('Issue Coupon', function() {
        const startTime = Date.now();
        
        const issueRes = http.post(
            `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`,
            null,
            { 
                headers: DEFAULT_HEADERS,
                timeout: '15s',
                tags: { name: 'IssueCoupon' }
            }
        );
        
        const duration = Date.now() - startTime;
        issueDuration.add(duration);
        
        results.issueStatus = issueRes.status;
        results.issueBody = issueRes.body;
        results.issueDuration = duration;
        
        check(issueRes, {
            'issue request accepted': (r) => r.status === 200 || r.status === 202 || r.status === 400,
        });
    });
    
    // 2. 대기열에 등록된 경우 상태 확인
    if (results.issueStatus === 202) {
        sleep(Math.random() * 3 + 2); // 2-5초 대기
        
        group('Check Status', function() {
            const startTime = Date.now();
            
            const statusRes = http.get(
                `${BASE_URL}/api/coupons/${couponId}/issue/status?userId=${userId}`,
                { 
                    headers: DEFAULT_HEADERS,
                    timeout: '10s',
                    tags: { name: 'CheckStatus' }
                }
            );
            
            const duration = Date.now() - startTime;
            statusCheckDuration.add(duration);
            
            results.statusCheckResult = statusRes.status;
            results.statusCheckBody = statusRes.body;
            
            check(statusRes, {
                'status check successful': (r) => r.status === 200,
            });
            
            // 아직 처리 중이면 한 번 더 확인
            if (statusRes.status === 200) {
                try {
                    const body = JSON.parse(statusRes.body);
                    if (body.status === 'PROCESSING') {
                        sleep(3);
                        
                        const finalStatusRes = http.get(
                            `${BASE_URL}/api/coupons/${couponId}/issue/status?userId=${userId}`,
                            { 
                                headers: DEFAULT_HEADERS,
                                timeout: '10s',
                                tags: { name: 'CheckStatus' }
                            }
                        );
                        
                        results.finalStatus = finalStatusRes.status;
                        results.finalBody = finalStatusRes.body;
                    }
                } catch (e) {
                    // JSON 파싱 실패
                }
            }
        });
    }
    
    return results;
}

export default function () {
    const userId = generateUserId();
    const couponId = Math.floor(Math.random() * 3) + 1; // 1-3번 쿠폰 랜덤 선택
    
    const results = couponIssueFlow(couponId, userId);
    
    // 결과 분석
    if (results.issueStatus === 200 || 
        (results.issueStatus === 202 && results.finalBody && results.finalBody.includes('SUCCESS'))) {
        couponSuccess.add(1);
    } else if (results.issueStatus === 400 || 
               (results.finalBody && results.finalBody.includes('FAILED'))) {
        couponFailed.add(1);
        
        // 실패 원인 로깅 (샘플링)
        if (Math.random() < 0.01) { // 1% 샘플링
            console.log(`Coupon ${couponId} failed for user ${userId}: ${results.issueBody || results.finalBody}`);
        }
    } else {
        errorRate.add(1);
    }
    
    sleep(Math.random() * 2); // 0-2초 랜덤 대기
}
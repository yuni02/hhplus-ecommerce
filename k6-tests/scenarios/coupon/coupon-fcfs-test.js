import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend, Gauge } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS, generateUserId } from '../../utils/config.js';

// Custom metrics
const errorRate = new Rate('errors');
const couponIssueSuccess = new Counter('coupon_issue_success');
const couponIssueFailed = new Counter('coupon_issue_failed');
const couponIssueQueued = new Counter('coupon_issue_queued');
const issueDuration = new Trend('coupon_issue_duration');
const queuePosition = new Gauge('queue_position');

export let options = {
    scenarios: {
        // 시나리오 1: 순간 폭발적인 트래픽 (이벤트 시작)
        spike_test: {
            executor: 'shared-iterations',
            vus: 1000,           // 1000명 동시 접속
            iterations: 1000,    // 총 1000번 시도
            maxDuration: '30s',  // 최대 30초
        },
        
        // 시나리오 2: 점진적 증가 테스트
        // ramp_test: {
        //     executor: 'ramping-vus',
        //     startVUs: 0,
        //     stages: [
        //         { duration: '10s', target: 100 },  // 10초 동안 100명까지
        //         { duration: '20s', target: 500 },  // 20초 동안 500명까지
        //         { duration: '30s', target: 1000 }, // 30초 동안 1000명까지
        //         { duration: '20s', target: 0 },    // 20초 동안 0명으로
        //     ],
        // },
        
        // 시나리오 3: 일정 부하 유지
        // constant_load: {
        //     executor: 'constant-vus',
        //     vus: 200,
        //     duration: '2m',
        // },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000', 'p(99)<5000'], // 응답시간
        http_req_failed: ['rate<0.5'],                   // 실패율 50% 미만
        errors: ['rate<0.5'],                             // 에러율 50% 미만
        coupon_issue_duration: ['p(95)<3000'],           // 쿠폰 발급 시간
    },
};

// 쿠폰 발급 요청
function issueCoupon(couponId, userId) {
    const startTime = Date.now();
    
    const response = http.post(
        `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`,
        null,
        { 
            headers: DEFAULT_HEADERS,
            timeout: '10s',
            tags: { name: 'IssueCoupon' }
        }
    );
    
    const duration = Date.now() - startTime;
    issueDuration.add(duration);
    
    return { response, duration };
}

// 발급 상태 확인
function checkIssueStatus(couponId, userId) {
    const response = http.get(
        `${BASE_URL}/api/coupons/${couponId}/issue/status?userId=${userId}`,
        { 
            headers: DEFAULT_HEADERS,
            timeout: '5s',
            tags: { name: 'CheckStatus' }
        }
    );
    
    return response;
}

// 메인 테스트 함수
export default function () {
    const userId = generateUserId();
    const couponId = 1; // 테스트할 쿠폰 ID (선착순 100명 쿠폰이라고 가정)
    
    // 1. 쿠폰 발급 요청
    const { response, duration } = issueCoupon(couponId, userId);
    
    // 응답 분석
    let issueStatus = 'UNKNOWN';
    let message = '';
    let position = null;
    
    if (response.status === 200) {
        // 즉시 발급 성공 (거의 발생하지 않음)
        couponIssueSuccess.add(1);
        issueStatus = 'SUCCESS';
        message = '쿠폰 발급 성공';
    } else if (response.status === 202) {
        // 대기열에 등록됨
        try {
            const body = JSON.parse(response.body);
            position = body.queuePosition;
            queuePosition.add(position || 0);
            couponIssueQueued.add(1);
            issueStatus = 'QUEUED';
            message = `대기열 ${position}번째`;
            
            // 대기열에 있다면 상태 확인
            sleep(2); // 2초 대기
            
            const statusResponse = checkIssueStatus(couponId, userId);
            if (statusResponse.status === 200) {
                const statusBody = JSON.parse(statusResponse.body);
                if (statusBody.status === 'SUCCESS') {
                    couponIssueSuccess.add(1);
                    issueStatus = 'SUCCESS';
                    message = '쿠폰 발급 성공 (대기열 처리)';
                } else if (statusBody.status === 'FAILED') {
                    couponIssueFailed.add(1);
                    issueStatus = 'FAILED';
                    message = statusBody.message || '쿠폰 소진';
                }
            }
        } catch (e) {
            console.log(`Failed to parse queue response: ${response.body}`);
        }
    } else if (response.status === 400) {
        // 발급 실패 (조건 미충족, 이미 발급 등)
        couponIssueFailed.add(1);
        errorRate.add(1);
        issueStatus = 'FAILED';
        try {
            const body = JSON.parse(response.body);
            message = body.message || '발급 실패';
        } catch (e) {
            message = '발급 실패';
        }
    } else {
        // 기타 에러
        couponIssueFailed.add(1);
        errorRate.add(1);
        issueStatus = 'ERROR';
        message = `HTTP ${response.status}`;
    }
    
    // 체크 수행
    const success = check(response, {
        'coupon issue accepted': (r) => r.status === 200 || r.status === 202,
        'response time < 2s': () => duration < 2000,
    });
    
    if (!success) {
        errorRate.add(1);
    }
    
    // 로그 출력 (선택적)
    if (__VU <= 10 || !success) { // 처음 10명 또는 실패한 경우만 로그
        console.log(`User ${userId}: ${issueStatus} - ${message} (${duration}ms)`);
    }
    
    // 다음 요청까지 짧은 대기
    sleep(Math.random() * 0.5);
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    const totalRequests = data.metrics.coupon_issue_success?.values?.count || 0 +
                         data.metrics.coupon_issue_failed?.values?.count || 0 +
                         data.metrics.coupon_issue_queued?.values?.count || 0;
    
    const successCount = data.metrics.coupon_issue_success?.values?.count || 0;
    const failedCount = data.metrics.coupon_issue_failed?.values?.count || 0;
    const queuedCount = data.metrics.coupon_issue_queued?.values?.count || 0;
    
    console.log('\n========================================');
    console.log('선착순 쿠폰 발급 테스트 결과');
    console.log('========================================');
    console.log(`총 요청 수: ${totalRequests}`);
    console.log(`발급 성공: ${successCount}`);
    console.log(`발급 실패: ${failedCount}`);
    console.log(`대기열 등록: ${queuedCount}`);
    console.log(`성공률: ${(successCount / totalRequests * 100).toFixed(2)}%`);
    console.log('========================================\n');
    
    return {
        'summary.json': JSON.stringify(data),
    };
}
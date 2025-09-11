import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL, DEFAULT_HEADERS } from '../../utils/config.js';

const duplicateIssues = new Counter('duplicate_issues');
const successfulIssues = new Counter('successful_issues');
const failedIssues = new Counter('failed_issues');

export let options = {
    scenarios: {
        // 동시성 테스트 - 같은 사용자가 동시에 여러 번 요청
        same_user_concurrent: {
            executor: 'per-vu-iterations',
            vus: 100,         // 100명의 사용자
            iterations: 10,   // 각 사용자가 10번씩 시도
            maxDuration: '30s',
        },
        
        // 레이스 컨디션 테스트 - 마지막 쿠폰을 여러 명이 동시에 요청
        last_coupon_race: {
            executor: 'shared-iterations',
            vus: 500,           // 500명이 동시에
            iterations: 500,    // 총 500번 요청 (쿠폰은 100개만 있다고 가정)
            maxDuration: '20s',
            startTime: '35s',  // 첫 번째 시나리오 후 실행
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        duplicate_issues: ['count<10'], // 중복 발급은 10건 미만이어야 함
    },
};

// 같은 사용자 중복 요청 테스트
function testSameUserConcurrent() {
    const userId = 1000 + __VU; // VU별로 고유한 사용자 ID
    const couponId = 2; // 테스트용 쿠폰 ID
    
    const responses = [];
    
    // 동시에 3개 요청 보내기 (배치)
    const batch = [
        ['POST', `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`, null, { headers: DEFAULT_HEADERS }],
        ['POST', `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`, null, { headers: DEFAULT_HEADERS }],
        ['POST', `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`, null, { headers: DEFAULT_HEADERS }],
    ];
    
    const batchResponses = http.batch(batch);
    
    // 응답 분석
    let successCount = 0;
    let queuedCount = 0;
    let failedCount = 0;
    
    batchResponses.forEach(response => {
        if (response.status === 200) {
            successCount++;
        } else if (response.status === 202) {
            queuedCount++;
        } else if (response.status === 400) {
            try {
                const body = JSON.parse(response.body);
                if (body.message && body.message.includes('이미 발급')) {
                    failedCount++;
                }
            } catch (e) {
                failedCount++;
            }
        }
    });
    
    // 중복 발급 체크
    if (successCount > 1 || queuedCount > 1) {
        duplicateIssues.add(1);
        console.log(`WARNING: User ${userId} got multiple acceptances! Success: ${successCount}, Queued: ${queuedCount}`);
    }
    
    if (successCount > 0 || queuedCount > 0) {
        successfulIssues.add(1);
    } else {
        failedIssues.add(1);
    }
    
    check(batchResponses[0], {
        'no duplicate issues': () => (successCount <= 1 && queuedCount <= 1),
    });
}

// 마지막 쿠폰 경쟁 테스트
function testLastCouponRace() {
    const userId = 2000 + __VU + __ITER * 1000; // 고유한 사용자 ID
    const couponId = 3; // 한정 수량 쿠폰
    
    const response = http.post(
        `${BASE_URL}/api/coupons/${couponId}/issue?userId=${userId}`,
        null,
        { 
            headers: DEFAULT_HEADERS,
            timeout: '10s',
        }
    );
    
    if (response.status === 200 || response.status === 202) {
        successfulIssues.add(1);
    } else {
        failedIssues.add(1);
    }
    
    check(response, {
        'request handled': (r) => r.status < 500,
    });
}

export default function () {
    if (__ENV.scenario === 'same_user_concurrent') {
        testSameUserConcurrent();
    } else {
        testLastCouponRace();
    }
}

export function handleSummary(data) {
    const duplicates = data.metrics.duplicate_issues?.values?.count || 0;
    const successful = data.metrics.successful_issues?.values?.count || 0;
    const failed = data.metrics.failed_issues?.values?.count || 0;
    
    console.log('\n========================================');
    console.log('동시성 및 레이스 컨디션 테스트 결과');
    console.log('========================================');
    console.log(`성공한 발급: ${successful}`);
    console.log(`실패한 발급: ${failed}`);
    console.log(`중복 발급 감지: ${duplicates}`);
    
    if (duplicates > 0) {
        console.log('⚠️  경고: 중복 발급이 발생했습니다!');
    } else {
        console.log('✅ 중복 발급 없음 - 동시성 제어 정상');
    }
    console.log('========================================\n');
    
    return {
        'race-condition-summary.json': JSON.stringify(data),
    };
}
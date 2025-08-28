package kr.hhplus.be.server.shared.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 동기 이벤트 처리기
 * 이벤트 요청-응답을 동기적으로 처리하기 위한 메커니즘
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronousEventProcessor {
    
    private final ApplicationEventPublisher eventPublisher;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    
    /**
     * 동기 이벤트 발행 및 응답 대기
     */
    public <T> T publishAndWaitForResponse(Object event, String requestId, Class<T> responseType, int timeoutSeconds) {
        log.debug("동기 이벤트 발행 및 응답 대기 - requestId: {}", requestId);
        
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        try {
            // 이벤트 발행
            eventPublisher.publishEvent(event);
            
            // 응답 대기 (타임아웃 설정)
            Object response = future.get(timeoutSeconds, TimeUnit.SECONDS);
            
            if (responseType.isInstance(response)) {
                log.debug("동기 이벤트 응답 수신 완료 - requestId: {}", requestId);
                return responseType.cast(response);
            } else {
                throw new IllegalStateException("응답 타입이 일치하지 않습니다: " + response.getClass());
            }
            
        } catch (TimeoutException e) {
            log.error("동기 이벤트 응답 타임아웃 - requestId: {}", requestId);
            throw new RuntimeException("이벤트 응답 타임아웃", e);
        } catch (Exception e) {
            log.error("동기 이벤트 처리 중 예외 발생 - requestId: {}", requestId, e);
            throw new RuntimeException("이벤트 처리 실패", e);
        } finally {
            pendingRequests.remove(requestId);
        }
    }
    
    /**
     * 응답 이벤트 처리
     */
    public void handleResponse(String requestId, Object responseEvent) {
        log.debug("응답 이벤트 처리 - requestId: {}", requestId);
        
        CompletableFuture<Object> future = pendingRequests.get(requestId);
        if (future != null) {
            future.complete(responseEvent);
        } else {
            log.warn("대기 중인 요청을 찾을 수 없습니다 - requestId: {}", requestId);
        }
    }
}
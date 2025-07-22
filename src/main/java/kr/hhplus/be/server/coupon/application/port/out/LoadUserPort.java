package kr.hhplus.be.server.coupon.application.port.out;

/**
 * 사용자 조회 Outgoing Port
 */
public interface LoadUserPort {
    
    /**
     * 사용자 존재 여부 확인
     */
    boolean existsById(Long userId);
} 
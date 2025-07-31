package kr.hhplus.be.server.order.application.port.out;

import java.util.Optional;

import kr.hhplus.be.server.user.domain.User;

/**
 * 사용자 조회 Outgoing Port
 */
public interface LoadUserPort {
    
    /**
     * 사용자 존재 여부 확인
     */
    boolean existsById(Long userId);

    /**
     * 사용자 조회
     */
    Optional<User> loadUserById(Long userId);
} 
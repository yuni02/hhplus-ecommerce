package kr.hhplus.be.server.user.application.port.out;

import java.util.Optional;

import lombok.Data;

/**
 * 사용자 조회 Outgoing Port
 */
public interface LoadUserPort {
    
    /**
     * 사용자 ID로 조회
     */
    Optional<UserInfo> loadUserById(Long userId);

    /**
     * 사용자 존재 여부 확인
     */
    boolean existsById(Long userId);
    
    /**
     * 사용자 정보
     */
    @Data
    class UserInfo {
        private final Long id;
        private final Long userId;
        private final String username;
        private final String status;

        public UserInfo(Long id, Long userId, String username, String status) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.status = status;
        }
    }
    

} 
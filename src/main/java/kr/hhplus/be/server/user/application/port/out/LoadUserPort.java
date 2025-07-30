package kr.hhplus.be.server.user.application.port.out;

import java.util.Optional;
import java.math.BigDecimal;

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
    class UserInfo {
        private final Long id;
        private final Long userId;
        private final String username;
        private final BigDecimal amount;
        private final String status;
        
        public UserInfo(Long id, Long userId, String username, BigDecimal amount, String status) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.amount = amount;
            this.status = status;
        }
        
        public Long getId() {
            return id;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public String getStatus() {
            return status;
        }
    }
} 
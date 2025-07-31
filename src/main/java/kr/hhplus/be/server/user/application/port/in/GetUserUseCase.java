package kr.hhplus.be.server.user.application.port.in;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 사용자 조회 Incoming Port (Use Case)
 */
public interface GetUserUseCase {
    
    /**
     * 사용자 조회 실행
     */
    Optional<GetUserResult> getUser(GetUserCommand command);
    
    /**
     * 사용자 조회 명령
     */
    class GetUserCommand {
        private final Long userId;
        
        public GetUserCommand(Long userId) {
            this.userId = userId;
        }
        
        public Long getUserId() {
            return userId;
        }
    }
    
    /**
     * 사용자 조회 결과
     */
    @Data
    class GetUserResult {
        private final Long id;
        private final Long userId;
        private final String username;
        private final String status;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
        
        public GetUserResult(Long id, Long userId, String username, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
} 
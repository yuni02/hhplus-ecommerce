package kr.hhplus.be.server.user.application.port.in;

import java.time.LocalDateTime;
import java.util.Optional;

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
    class GetUserResult {
        private final Long id;
        private final String name;
        private final String email;
        private final String phoneNumber;
        private final String status;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
        
        public GetUserResult(Long id, String name, String email, String phoneNumber,
                           String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public String getStatus() {
            return status;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
} 
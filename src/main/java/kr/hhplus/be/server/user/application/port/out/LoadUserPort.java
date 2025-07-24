package kr.hhplus.be.server.user.application.port.out;

import java.util.Optional;

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
        private final String name;
        private final String email;
        private final String phoneNumber;
        private final String status;
        
        public UserInfo(Long id, String name, String email, String phoneNumber, String status) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.status = status;
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
    }
} 
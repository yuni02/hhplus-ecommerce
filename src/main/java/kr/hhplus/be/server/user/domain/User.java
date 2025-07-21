package kr.hhplus.be.server.user.domain;

import kr.hhplus.be.server.shared.domain.BaseEntity;

public class User extends BaseEntity {

    private String name;
    private String email;
    private UserStatus status = UserStatus.ACTIVE;

    public User() {}

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, DELETED
    }
} 
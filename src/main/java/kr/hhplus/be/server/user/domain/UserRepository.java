package kr.hhplus.be.server.user.domain;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByIdAndStatus(Long id, User.UserStatus status);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    User save(User user);
    boolean existsById(Long id);
} 
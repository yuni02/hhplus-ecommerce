package kr.hhplus.be.server.user.adapter.out.persistence;

import kr.hhplus.be.server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 엔티티 JPA Repository
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {

    /**
     * 사용자명으로 사용자 조회
     */
    Optional<User> findByUsername(String username);

    /**
     * 활성 상태인 사용자 조회
     */
    Optional<User> findByIdAndStatus(Long userId, User.UserStatus status);

    /**
     * 사용자명으로 활성 상태인 사용자 조회
     */
    Optional<User> findByUsernameAndStatus(String username, User.UserStatus status);
} 
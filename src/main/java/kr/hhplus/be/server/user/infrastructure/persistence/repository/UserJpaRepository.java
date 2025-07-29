package kr.hhplus.be.server.user.infrastructure.persistence.repository;

import kr.hhplus.be.server.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User 엔티티 JPA Repository
 * User 도메인 전용 데이터 접근 계층
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * 전화번호로 사용자 조회
     */
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    /**
     * 이름으로 사용자 조회
     */
    List<UserEntity> findByNameContaining(String name);

    /**
     * 활성 상태의 사용자 조회
     */
    List<UserEntity> findByStatus(String status);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 전화번호 존재 여부 확인
     */
    boolean existsByPhoneNumber(String phoneNumber);
}
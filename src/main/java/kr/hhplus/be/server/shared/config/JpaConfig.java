package kr.hhplus.be.server.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

/**
 * JPA 설정
 */
@Configuration
@EnableJpaAuditing
@EnableRetry
public class JpaConfig {
} 
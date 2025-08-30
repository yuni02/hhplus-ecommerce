package kr.hhplus.be.server.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 웹 관련 설정
 */
@Configuration
public class WebConfig {

    /**
     * RestTemplate Bean
     * 외부 API 호출을 위한 HTTP 클라이언트
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

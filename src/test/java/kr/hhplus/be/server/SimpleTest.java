package kr.hhplus.be.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SimpleTest {

    @Test
    void contextLoads() {
        System.out.println("Simple test context loaded successfully!");
    }
}

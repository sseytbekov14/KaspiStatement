package com.sultan.kaspitracker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Disabled("Docker Desktop 4.75 / Testcontainers API incompatibility — see Technical Specification.md open issues")
class KaspiTrackerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring context starts successfully
        // and connects to PostgreSQL (requires docker-compose DB to be running).
    }
}

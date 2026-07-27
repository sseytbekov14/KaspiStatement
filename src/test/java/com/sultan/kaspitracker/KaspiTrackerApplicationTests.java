package com.sultan.kaspitracker;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires PostgreSQL connection. Will be configured in Milestone 4.")
class KaspiTrackerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring context starts successfully
        // and connects to PostgreSQL (requires docker-compose DB to be running).
    }
}

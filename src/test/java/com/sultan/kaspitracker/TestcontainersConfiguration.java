package com.sultan.kaspitracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spring Boot 3.1+ Testcontainers configuration.
 * Using @ServiceConnection automatically configures Spring Boot with the correct
 * database URL, username, and password provided by Testcontainers.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("kaspi_test")
                .withUsername("test_user")
                .withPassword("test_password");
    }
}

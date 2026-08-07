package com.kleos.transfers.common.test;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Spring Boot + MockMvc base that runs Flyway against PostgreSQL via Testcontainers.
 *
 * <p>Unit tests that do not extend this class stay fast and do not require Docker.
 * Integration tests are skipped when Docker is unavailable ({@code disabledWithoutDocker}).
 *
 * <p>The container is started once per JVM so all subclasses share one Postgres instance and
 * (with identical annotations) one Spring test context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("kleos_transfers")
            .withUsername("kleos")
            .withPassword("kleos");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
        }
    }
}

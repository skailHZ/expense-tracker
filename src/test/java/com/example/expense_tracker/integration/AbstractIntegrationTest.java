package com.example.expense_tracker.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Базовый класс интеграционных тестов: полный Spring-контекст, настоящая цепочка Spring Security
 * и настоящий PostgreSQL в Docker (тот же major-версии, что и в docker-compose), миграции Liquibase
 * прогоняются на нем так же, как в проде.
 *
 * Контейнер один на весь прогон тестов (singleton), потому что Spring кэширует контекст между классами.
 * Останавливать его вручную не нужно: контейнер удалит Ryuk, когда завершится JVM.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15");

    static {
        POSTGRES.start();
    }
}

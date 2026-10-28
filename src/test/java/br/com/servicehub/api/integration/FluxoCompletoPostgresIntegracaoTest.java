package br.com.servicehub.api.integration;

import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Executa o fluxo completo sobre um PostgreSQL real em container, verificando as migrações
 * Flyway e o mapeamento JPA (ddl-auto=validate) no mesmo banco usado pela aplicação.
 * Requer Docker; sem ele, o teste é ignorado (skipped) em vez de falhar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
@Testcontainers(disabledWithoutDocker = true)
class FluxoCompletoPostgresIntegracaoTest extends FluxoCompletoIntegracaoBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
}

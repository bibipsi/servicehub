package br.com.servicehub.api.integration;

import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Executa o fluxo completo sobre o H2 em memória (modo PostgreSQL), com o schema
 * criado pelas mesmas migrações Flyway da aplicação.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class FluxoCompletoH2IntegracaoTest extends FluxoCompletoIntegracaoBase {
}

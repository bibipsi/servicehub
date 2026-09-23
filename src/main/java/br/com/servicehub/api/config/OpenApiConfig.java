package br.com.servicehub.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .version("v1")
                        .description("API REST para gerenciamento do catálogo de serviços da plataforma ServiceHub.")
                        .contact(new Contact().name("Equipe ServiceHub")));
    }
}

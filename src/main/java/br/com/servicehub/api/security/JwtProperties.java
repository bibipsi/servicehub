package br.com.servicehub.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações do token JWT, lidas das propriedades {@code servicehub.jwt.*}.
 *
 * @param secret    chave HMAC usada para assinar os tokens (mínimo de 32 bytes para HS256)
 * @param expiracao tempo de validade do token
 * @param emissor   valor do claim {@code iss}, verificado na validação
 */
@ConfigurationProperties(prefix = "servicehub.jwt")
public record JwtProperties(String secret, Duration expiracao, String emissor) {
}

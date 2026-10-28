package br.com.servicehub.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Gera e valida tokens JWT assinados com HMAC-SHA256.
 * O payload contém apenas o id do usuário (sub), o papel e as datas de emissão e expiração;
 * senha, e-mail e telefone nunca são incluídos.
 */
@Service
public class JwtService {

    static final String CLAIM_PAPEL = "papel";
    private static final int TAMANHO_MINIMO_CHAVE = 32;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_CHAVE) {
            throw new IllegalStateException(
                    "A propriedade servicehub.jwt.secret (JWT_SECRET) deve ter pelo menos 32 bytes para assinar tokens HS256");
        }
        if (properties.expiracao() == null) {
            throw new IllegalStateException("A propriedade servicehub.jwt.expiracao (JWT_EXPIRATION) é obrigatória");
        }
        this.properties = properties;
        this.clock = clock;

        SecretKey chave = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build();

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        nimbusDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.emissor()));
        this.decoder = nimbusDecoder;
    }

    public String gerarToken(UsuarioAutenticado usuario) {
        Instant agora = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.emissor())
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(agora.plus(properties.expiracao()))
                .claim(CLAIM_PAPEL, usuario.getPapel().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Valida assinatura, emissor e expiração do token e retorna o id do usuário.
     *
     * @throws JwtException se o token for inválido ou estiver expirado
     */
    public Long extrairUsuarioId(String token) {
        Jwt jwt = decoder.decode(token);
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new BadJwtException("Identificador do usuário inválido no token");
        }
    }

    public Duration getExpiracao() {
        return properties.expiracao();
    }
}

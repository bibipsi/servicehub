package br.com.servicehub.api.security;

import br.com.servicehub.api.entity.PapelUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CHAVE = "chave-de-teste-com-pelo-menos-32-bytes-para-hs256";
    private static final String EMISSOR = "servicehub-api";

    private final UsuarioAutenticado usuario = new UsuarioAutenticado(42L, "maria@email.com", "$2a$10$hash",
            PapelUsuario.PROVIDER, true);

    @Test
    void deveGerarTokenValidoComIdDoUsuario() {
        JwtService jwtService = new JwtService(new JwtProperties(CHAVE, Duration.ofMinutes(15), EMISSOR));

        String token = jwtService.gerarToken(usuario);

        assertThat(jwtService.extrairUsuarioId(token)).isEqualTo(42L);
    }

    @Test
    void payloadNaoDeveConterDadosSensiveis() {
        JwtService jwtService = new JwtService(new JwtProperties(CHAVE, Duration.ofMinutes(15), EMISSOR));

        String payload = new String(Base64.getUrlDecoder().decode(jwtService.gerarToken(usuario).split("\\.")[1]),
                StandardCharsets.UTF_8);

        assertThat(payload)
                .contains("\"sub\":\"42\"", "\"papel\":\"PROVIDER\"", "\"exp\"")
                .doesNotContain("maria@email.com", "$2a$10$hash", "senha", "telefone");
    }

    @Test
    void deveRejeitarTokenExpirado() {
        JwtProperties properties = new JwtProperties(CHAVE, Duration.ofMinutes(15), EMISSOR);
        // Token emitido há 1 hora com validade de 15 minutos: expirou há 45 minutos
        Clock umaHoraAtras = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String token = new JwtService(properties, umaHoraAtras).gerarToken(usuario);

        assertThatThrownBy(() -> new JwtService(properties).extrairUsuarioId(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void deveRejeitarTokenAssinadoComOutraChave() {
        JwtService outraChave = new JwtService(new JwtProperties(CHAVE + "-diferente", Duration.ofMinutes(15), EMISSOR));
        JwtService jwtService = new JwtService(new JwtProperties(CHAVE, Duration.ofMinutes(15), EMISSOR));
        String token = outraChave.gerarToken(usuario);

        assertThatThrownBy(() -> jwtService.extrairUsuarioId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenAdulterado() {
        JwtService jwtService = new JwtService(new JwtProperties(CHAVE, Duration.ofMinutes(15), EMISSOR));
        String[] partes = jwtService.gerarToken(usuario).split("\\.");
        String payloadAdulterado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"1\",\"papel\":\"ADMIN\",\"iss\":\"" + EMISSOR + "\",\"exp\":9999999999}")
                        .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwtService.extrairUsuarioId(partes[0] + "." + payloadAdulterado + "." + partes[2]))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRecusarChaveCurtaNaInicializacao() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("curta", Duration.ofMinutes(15), EMISSOR)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}

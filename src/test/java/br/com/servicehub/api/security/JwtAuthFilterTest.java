package br.com.servicehub.api.security;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.support.AutenticacaoTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tentativas de contornar a autenticação na camada HTTP: cada token manipulado deve
 * resultar em 401 antes de chegar aos Controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthFilterTest {

    private static final String PERFIL = "/api/usuarios/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    private AutenticacaoTestHelper auth;
    private Usuario cliente;

    @BeforeEach
    void prepararUsuario() {
        auth = new AutenticacaoTestHelper(usuarioRepository, servicoRepository, passwordEncoder, jwtService);
        auth.limparBanco();
        cliente = auth.criarUsuario("cliente@servicehub.com", PapelUsuario.CLIENT);
    }

    @Test
    void tokenValidoAutentica() throws Exception {
        mockMvc.perform(get(PERFIL).header("Authorization", "Bearer " + auth.token(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cliente.getId()));
    }

    @Test
    void tokenExpiradoRetorna401() throws Exception {
        Clock umaHoraAtras = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String expirado = new JwtService(jwtProperties, umaHoraAtras).gerarToken(UsuarioAutenticado.de(cliente));

        mockMvc.perform(get(PERFIL).header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
    }

    @Test
    void tokenComPayloadAdulteradoParaAdminRetorna401() throws Exception {
        String[] partes = auth.token(cliente).split("\\.");
        String payloadAdmin = base64("{\"iss\":\"servicehub-api\",\"sub\":\"%d\",\"papel\":\"ADMIN\",\"exp\":9999999999}"
                .formatted(cliente.getId()));

        mockMvc.perform(patch("/api/usuarios/" + cliente.getId() + "/papel")
                        .header("Authorization", "Bearer " + partes[0] + "." + payloadAdmin + "." + partes[2])
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"papel\": \"ADMIN\" }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSemAssinaturaComAlgNoneRetorna401() throws Exception {
        String semAssinatura = base64("{\"alg\":\"none\"}") + "."
                + base64("{\"iss\":\"servicehub-api\",\"sub\":\"%d\",\"exp\":9999999999}".formatted(cliente.getId())) + ".";

        mockMvc.perform(get(PERFIL).header("Authorization", "Bearer " + semAssinatura))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void headerBearerVazioRetorna401() throws Exception {
        mockMvc.perform(get(PERFIL).header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioExcluidoRetorna401() throws Exception {
        String token = auth.token(cliente);
        usuarioRepository.delete(cliente);

        mockMvc.perform(get(PERFIL).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Usuário do token não existe ou está inativo"));
    }

    private static String base64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}

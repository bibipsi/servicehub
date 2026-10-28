package br.com.servicehub.api.controller;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.support.AutenticacaoTestHelper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String CADASTRO = "/api/auth/cadastro";
    private static final String LOGIN = "/api/auth/login";

    private static final String USUARIO_VALIDO = """
            {
              "nome": "Maria Souza",
              "email": "Maria@Email.com",
              "senha": "SenhaForte123",
              "telefone": "(62) 99999-0000"
            }
            """;

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

    @BeforeEach
    void limparBanco() {
        new AutenticacaoTestHelper(usuarioRepository, servicoRepository, passwordEncoder, jwtService).limparBanco();
    }

    @Test
    void deveCadastrarClienteComSenhaEmBcrypt() throws Exception {
        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/usuarios/")))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.papel").value("CLIENT"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail("maria@email.com").orElseThrow();
        assertThat(salvo.getSenhaHash()).startsWith("$2").isNotEqualTo("SenhaForte123");
        assertThat(passwordEncoder.matches("SenhaForte123", salvo.getSenhaHash())).isTrue();
    }

    @Test
    void deveIgnorarPapelEnviadoNoCadastro() throws Exception {
        String comPapel = USUARIO_VALIDO.replace("{", "{ \"papel\": \"ADMIN\", \"role\": \"ADMIN\", \"ativo\": false,");

        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON).content(comPapel))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("CLIENT"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRetornar400QuandoCadastroInvalido() throws Exception {
        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Maria\", \"email\": \"email-invalido\", \"senha\": \"123\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
    }

    @Test
    void deveRetornar400QuandoSenhaUltrapassa72BytesDoBcrypt() throws Exception {
        // 72 caracteres, mas 144 bytes em UTF-8: passa no @Size e seria rejeitada pelo BCrypt com HTTP 500
        String senhaAcentuada = "é".repeat(72);

        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_VALIDO.replace("SenhaForte123", senhaAcentuada)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    @Test
    void deveRetornar409QuandoEmailJaCadastrado() throws Exception {
        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isCreated());

        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON)
                        .content(USUARIO_VALIDO.replace("Maria@Email.com", "MARIA@email.COM")))
                .andExpect(status().isConflict());
    }

    @Test
    void deveAutenticarERetornarTokenSemDadosSensiveis() throws Exception {
        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isCreated());

        String resposta = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"maria@email.com\", \"senha\": \"SenhaForte123\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(900))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(resposta, "$.token");
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).doesNotContain("maria@email.com", "SenhaForte123", "99999");

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    void deveRetornar401ComMesmaMensagemParaSenhaErradaOuEmailInexistente() throws Exception {
        mockMvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON).content(USUARIO_VALIDO))
                .andExpect(status().isCreated());

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"maria@email.com\", \"senha\": \"SenhaErrada1\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"naoexiste@email.com\", \"senha\": \"SenhaForte123\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void usuarioInativoNaoConsegueAutenticar() throws Exception {
        AutenticacaoTestHelper auth = new AutenticacaoTestHelper(usuarioRepository, servicoRepository,
                passwordEncoder, jwtService);
        Usuario usuario = auth.criarUsuario("inativo@email.com", PapelUsuario.CLIENT);
        String tokenAntigo = auth.token(usuario);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"inativo@email.com\", \"senha\": \"%s\" }"
                                .formatted(AutenticacaoTestHelper.SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenAntigo))
                .andExpect(status().isUnauthorized());
    }
}

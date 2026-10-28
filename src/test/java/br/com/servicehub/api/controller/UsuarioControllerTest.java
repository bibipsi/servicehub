package br.com.servicehub.api.controller;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.support.AutenticacaoTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    private static final String URL = "/api/usuarios";

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

    private AutenticacaoTestHelper auth;
    private Usuario cliente;
    private Usuario outroCliente;
    private Usuario admin;

    @BeforeEach
    void prepararUsuarios() {
        auth = new AutenticacaoTestHelper(usuarioRepository, servicoRepository, passwordEncoder, jwtService);
        auth.limparBanco();
        cliente = auth.criarUsuario("cliente@servicehub.com", PapelUsuario.CLIENT);
        outroCliente = auth.criarUsuario("outro.cliente@servicehub.com", PapelUsuario.CLIENT);
        admin = auth.criarUsuario("admin@servicehub.com", PapelUsuario.ADMIN);
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get(URL + "/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void usuarioPodeConsultarOsProprioDados() throws Exception {
        mockMvc.perform(get(URL + "/" + cliente.getId()).with(auth.bearer(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@servicehub.com"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void deveRetornar403AoConsultarOuAlterarDadosDeOutroUsuario() throws Exception {
        mockMvc.perform(get(URL + "/" + outroCliente.getId()).with(auth.bearer(cliente)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(URL + "/" + outroCliente.getId()).with(auth.bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"nome\": \"Nome Alterado\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNaoConsegueAlterarOProprioPapelPeloPerfil() throws Exception {
        mockMvc.perform(put(URL + "/" + cliente.getId()).with(auth.bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"Cliente Atualizado\", \"papel\": \"ADMIN\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente Atualizado"))
                .andExpect(jsonPath("$.papel").value("CLIENT"));
    }

    @Test
    void somenteAdminListaUsuarios() throws Exception {
        mockMvc.perform(get(URL).with(auth.bearer(cliente)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL).with(auth.bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void somenteAdminAlteraPapel() throws Exception {
        String promover = "{ \"papel\": \"PROVIDER\" }";

        mockMvc.perform(patch(URL + "/" + cliente.getId() + "/papel").with(auth.bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(promover))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(URL + "/" + cliente.getId() + "/papel").with(auth.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(promover))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("PROVIDER"));
    }

    @Test
    void novoPapelValeImediatamenteParaTokenJaEmitido() throws Exception {
        String tokenEmitidoComoCliente = auth.token(cliente);
        String servico = """
                { "nome": "Aula de violão", "preco": 80.00, "duracaoMinutos": 60, "categoria": "OUTROS" }
                """;

        mockMvc.perform(post("/api/servicos").header("Authorization", "Bearer " + tokenEmitidoComoCliente)
                        .contentType(MediaType.APPLICATION_JSON).content(servico))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(URL + "/" + cliente.getId() + "/papel").with(auth.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"papel\": \"PROVIDER\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/servicos").header("Authorization", "Bearer " + tokenEmitidoComoCliente)
                        .contentType(MediaType.APPLICATION_JSON).content(servico))
                .andExpect(status().isCreated());
    }

    @Test
    void deveRetornar404QuandoAdminConsultaUsuarioInexistente() throws Exception {
        mockMvc.perform(get(URL + "/999999").with(auth.bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400QuandoPapelInvalido() throws Exception {
        mockMvc.perform(patch(URL + "/" + cliente.getId() + "/papel").with(auth.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"papel\": \"SUPERUSER\" }"))
                .andExpect(status().isBadRequest());
    }
}

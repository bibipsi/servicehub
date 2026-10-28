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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação, autorização (RBAC) e verificação de propriedade nas rotas de serviços.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServicoSegurancaTest {

    private static final String URL = "/api/servicos";

    private static final String SERVICO_VALIDO = """
            {
              "nome": "Formatação de computador",
              "descricao": "Backup, formatação e instalação do sistema",
              "preco": 180.00,
              "duracaoMinutos": 240,
              "categoria": "TECNOLOGIA"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private AutenticacaoTestHelper auth;
    private Usuario prestador;
    private Usuario outroPrestador;
    private Usuario cliente;
    private Usuario admin;

    @BeforeEach
    void prepararUsuarios() {
        auth = new AutenticacaoTestHelper(usuarioRepository, servicoRepository, passwordEncoder, jwtService);
        auth.limparBanco();
        prestador = auth.criarUsuario("prestador@servicehub.com", PapelUsuario.PROVIDER);
        outroPrestador = auth.criarUsuario("outro.prestador@servicehub.com", PapelUsuario.PROVIDER);
        cliente = auth.criarUsuario("cliente@servicehub.com", PapelUsuario.CLIENT);
        admin = auth.criarUsuario("admin@servicehub.com", PapelUsuario.ADMIN);
    }

    @Test
    void deveRetornar401AoCadastrarSemToken() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    void deveRetornar401QuandoTokenInvalido() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", "Bearer token.invalido.qualquer")
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
    }

    @Test
    void deveRetornar403QuandoClienteTentaCadastrarServico() throws Exception {
        mockMvc.perform(post(URL).with(auth.bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deveRetornar201QuandoPrestadorCadastraServico() throws Exception {
        mockMvc.perform(post(URL).with(auth.bearer(prestador))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.prestadorId").value(prestador.getId()));
    }

    @Test
    void deveIgnorarPrestadorIdEnviadoNoCorpo() throws Exception {
        String comPrestadorForjado = SERVICO_VALIDO.replace("{", "{ \"prestadorId\": " + outroPrestador.getId() + ",");

        mockMvc.perform(post(URL).with(auth.bearer(prestador))
                        .contentType(MediaType.APPLICATION_JSON).content(comPrestadorForjado))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prestadorId").value(prestador.getId()));
    }

    @Test
    void deveRetornar400QuandoDadosInvalidos() throws Exception {
        mockMvc.perform(post(URL).with(auth.bearer(prestador)).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"ab\", \"preco\": -1, \"duracaoMinutos\": 2000 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(4));
    }

    @Test
    void deveRetornar400QuandoTextoContemHtml() throws Exception {
        String comScript = SERVICO_VALIDO.replace("Formatação de computador", "<script>alert(1)</script>");

        mockMvc.perform(post(URL).with(auth.bearer(prestador))
                        .contentType(MediaType.APPLICATION_JSON).content(comScript))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O nome não pode conter os caracteres < e >"));
    }

    @Test
    void deveRetornar404QuandoServicoNaoExiste() throws Exception {
        mockMvc.perform(put(URL + "/999").with(auth.bearer(prestador))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar403QuandoPrestadorAlteraServicoDeOutro() throws Exception {
        String url = criarServico(prestador);

        mockMvc.perform(put(url).with(auth.bearer(outroPrestador))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO.replace("180.00", "1.00")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(url).with(auth.bearer(outroPrestador)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(180.00));
    }

    @Test
    void adminPodeAlterarEExcluirServicoDeQualquerPrestador() throws Exception {
        String url = criarServico(prestador);

        mockMvc.perform(put(url).with(auth.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO.replace("180.00", "200.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(200.00))
                .andExpect(jsonPath("$.prestadorId").value(prestador.getId()));

        mockMvc.perform(delete(url).with(auth.bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void consultasDoCatalogoSaoPublicas() throws Exception {
        String url = criarServico(prestador);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void rotaInexistenteComUsuarioAutenticadoRetorna404() throws Exception {
        mockMvc.perform(get("/api/rota-inexistente").with(auth.bearer(prestador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void contentTypeNaoSuportadoRetorna415() throws Exception {
        mockMvc.perform(post(URL).with(auth.bearer(prestador))
                        .contentType(MediaType.TEXT_PLAIN).content(SERVICO_VALIDO))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    private String criarServico(Usuario dono) throws Exception {
        String resposta = mockMvc.perform(post(URL).with(auth.bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(resposta, "$.id");
        return URL + "/" + id;
    }
}

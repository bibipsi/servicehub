package br.com.servicehub.api.integration;

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
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo da aplicação, com todas as camadas reais (filtros de segurança,
 * Controllers, Services, Repositories, Flyway e banco). As subclasses definem o banco usado.
 */
abstract class FluxoCompletoIntegracaoBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected HttpGraphQlTester graphQlTester;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected ServicoRepository servicoRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    private AutenticacaoTestHelper auth;

    @BeforeEach
    void limparBanco() {
        auth = new AutenticacaoTestHelper(usuarioRepository, servicoRepository, passwordEncoder, jwtService);
        auth.limparBanco();
    }

    @Test
    void deveCadastrarAutenticarCriarEConsultarServico() throws Exception {
        Usuario admin = auth.criarUsuario("admin@servicehub.com", PapelUsuario.ADMIN);

        // 1. Cadastro público: o usuário nasce como CLIENT
        String cadastro = mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "João Prestador", "email": "joao@servicehub.com", "senha": "SenhaForte123" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("CLIENT"))
                .andReturn().getResponse().getContentAsString();
        Integer prestadorId = JsonPath.read(cadastro, "$.id");

        // 2. O ADMIN promove o usuário a PROVIDER
        mockMvc.perform(patch("/api/usuarios/" + prestadorId + "/papel").with(auth.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"papel\": \"PROVIDER\" }"))
                .andExpect(status().isOk());

        // 3. Login com a senha cadastrada
        String login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"joao@servicehub.com\", \"senha\": \"SenhaForte123\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + JsonPath.read(login, "$.token");

        // 4. Criação do serviço pelo prestador autenticado
        String criado = mockMvc.perform(post("/api/servicos").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "nome": "Instalação de chuveiro",
                                  "descricao": "Instalação de chuveiro elétrico com troca de resistência",
                                  "preco": 120.00,
                                  "duracaoMinutos": 60,
                                  "categoria": "MANUTENCAO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prestadorId").value(prestadorId))
                .andReturn().getResponse().getContentAsString();
        Integer servicoId = JsonPath.read(criado, "$.id");

        // 5. Consulta pública via REST
        mockMvc.perform(get("/api/servicos").param("categoria", "MANUTENCAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Instalação de chuveiro"));

        mockMvc.perform(get("/api/servicos/" + servicoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(120.00));

        // 6. Consulta pelo endpoint alternativo GraphQL
        graphQlTester.document("{ servico(id: %d) { nome preco categoria prestadorId } }".formatted(servicoId))
                .execute()
                .path("servico.nome").entity(String.class).isEqualTo("Instalação de chuveiro")
                .path("servico.prestadorId").entity(String.class).isEqualTo(String.valueOf(prestadorId));

        // 7. Outro usuário não consegue alterar o serviço
        Usuario cliente = auth.criarUsuario("cliente@servicehub.com", PapelUsuario.CLIENT);
        mockMvc.perform(put("/api/servicos/" + servicoId).with(auth.bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                { "nome": "Alterado", "preco": 1.00, "duracaoMinutos": 1, "categoria": "OUTROS" }
                                """))
                .andExpect(status().isForbidden());

        assertThat(servicoRepository.findById(servicoId.longValue()))
                .hasValueSatisfying(servico -> assertThat(servico.getNome()).isEqualTo("Instalação de chuveiro"));
    }
}

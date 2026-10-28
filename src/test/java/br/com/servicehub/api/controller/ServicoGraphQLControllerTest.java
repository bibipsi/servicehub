package br.com.servicehub.api.controller;

import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.entity.Servico;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.support.AutenticacaoTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureHttpGraphQlTester
class ServicoGraphQLControllerTest {

    @Autowired
    private HttpGraphQlTester graphQlTester;

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

    @BeforeEach
    void prepararServicos() {
        new AutenticacaoTestHelper(usuarioRepository, servicoRepository, passwordEncoder, jwtService).limparBanco();
        salvar("Limpeza pós-obra", CategoriaServico.LIMPEZA);
        salvar("Troca de tomadas", CategoriaServico.MANUTENCAO);
    }

    @Test
    void deveListarServicosSemAutenticacao() {
        graphQlTester.document("{ servicos { id nome categoria preco ativo } }")
                .execute()
                .path("servicos").entityList(Object.class).hasSize(2)
                .path("servicos[0].ativo").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void deveFiltrarServicosPorCategoria() {
        graphQlTester.document("{ servicos(categoria: LIMPEZA) { nome } }")
                .execute()
                .path("servicos").entityList(Object.class).hasSize(1)
                .path("servicos[0].nome").entity(String.class).isEqualTo("Limpeza pós-obra");
    }

    @Test
    void deveRetornarErroNotFoundQuandoServicoNaoExiste() {
        graphQlTester.document("{ servico(id: 999999) { nome } }")
                .execute()
                .errors()
                .expect(erro -> erro.getErrorType() == ErrorType.NOT_FOUND
                        && "Serviço com id 999999 não encontrado".equals(erro.getMessage()))
                .verify()
                .path("servico").valueIsNull();
    }

    @Test
    void deveRetornar401QuandoTokenInvalidoForEnviado() throws Exception {
        mockMvc.perform(post("/graphql").header("Authorization", "Bearer invalido")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"query\": \"{ servicos { nome } }\" }"))
                .andExpect(status().isUnauthorized());
    }

    private void salvar(String nome, CategoriaServico categoria) {
        Servico servico = new Servico();
        servico.setNome(nome);
        servico.setPreco(new BigDecimal("99.90"));
        servico.setDuracaoMinutos(60);
        servico.setCategoria(categoria);
        servico.setAtivo(true);
        servicoRepository.save(servico);
    }
}

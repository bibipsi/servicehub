package br.com.servicehub.api.controller;

import br.com.servicehub.api.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServicoControllerTest {

    private static final String URL = "/api/servicos";

    private static final String SERVICO_VALIDO = """
            {
              "nome": "Instalação de ar-condicionado",
              "descricao": "Instalação de aparelho split",
              "preco": 350.00,
              "duracaoMinutos": 120,
              "categoria": "MANUTENCAO"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicoRepository servicoRepository;

    @BeforeEach
    void limparBanco() {
        servicoRepository.deleteAll();
    }

    @Test
    void deveExecutarCrudCompleto() throws Exception {
        MvcResult criado = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn();

        String location = criado.getResponse().getHeader("Location");
        String urlServico = location.substring(location.indexOf(URL));

        mockMvc.perform(get(urlServico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Instalação de ar-condicionado"));

        mockMvc.perform(get(URL).param("categoria", "MANUTENCAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put(urlServico).contentType(MediaType.APPLICATION_JSON)
                        .content(SERVICO_VALIDO.replace("350.00", "420.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(420.00));

        mockMvc.perform(patch(urlServico + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(delete(urlServico))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(urlServico))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveRetornar400QuandoDadosInvalidos() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nome\": \"\", \"preco\": 0, \"duracaoMinutos\": 0 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    void deveRetornar400QuandoJsonMalFormado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ nome: "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400QuandoIdInvalido() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404AoAtualizarOuExcluirInexistente() throws Exception {
        mockMvc.perform(put(URL + "/999").contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(URL + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar409QuandoNomeDuplicado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isCreated());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(SERVICO_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void deveExporDocumentacaoSwagger() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ServiceHub API"))
                .andExpect(jsonPath("$.paths['/api/servicos/{id}'].get.summary").value("Buscar serviço por ID"));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}

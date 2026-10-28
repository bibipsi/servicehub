package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.ServicoResponse;
import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.exception.RecursoNaoEncontradoException;
import br.com.servicehub.api.service.ServicoService;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Endpoint alternativo em GraphQL (POST /graphql) para consulta do catálogo.
 * Reutiliza as mesmas regras da API REST por meio do {@link ServicoService}.
 */
@Controller
public class ServicoGraphQLController {

    private final ServicoService servicoService;

    public ServicoGraphQLController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @QueryMapping
    public List<ServicoResponse> servicos(@Argument CategoriaServico categoria) {
        return servicoService.listar(categoria);
    }

    @QueryMapping
    public ServicoResponse servico(@Argument Long id) {
        return servicoService.buscarPorId(id);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleNaoEncontrado(RecursoNaoEncontradoException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .build();
    }
}

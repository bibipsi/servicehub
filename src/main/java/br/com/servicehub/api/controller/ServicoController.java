package br.com.servicehub.api.controller;

import br.com.servicehub.api.config.OpenApiConfig;
import br.com.servicehub.api.dto.ServicoRequest;
import br.com.servicehub.api.dto.ServicoResponse;
import br.com.servicehub.api.dto.ServicoStatusRequest;
import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.exception.ErroResponse;
import br.com.servicehub.api.security.UsuarioAutenticado;
import br.com.servicehub.api.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/servicos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Serviços", description = "Cadastro e gerenciamento do catálogo de serviços oferecidos na plataforma")
public class ServicoController {

    private static final String EXEMPLO_REQUEST = """
            {
              "nome": "Instalação de ar-condicionado",
              "descricao": "Instalação completa de aparelho split até 12.000 BTUs, incluindo suporte e tubulação de até 3 metros.",
              "preco": 350.00,
              "duracaoMinutos": 120,
              "categoria": "MANUTENCAO"
            }
            """;

    private static final String EXEMPLO_ERRO_VALIDACAO = """
            {
              "timestamp": "2026-09-22T10:15:30",
              "status": 400,
              "erro": "Bad Request",
              "mensagem": "Um ou mais campos estão inválidos",
              "path": "/api/servicos",
              "campos": [
                { "campo": "preco", "mensagem": "O preço deve ser maior que zero" },
                { "campo": "nome", "mensagem": "O nome é obrigatório" }
              ]
            }
            """;

    private static final String EXEMPLO_ERRO_NAO_ENCONTRADO = """
            {
              "timestamp": "2026-09-22T10:15:30",
              "status": 404,
              "erro": "Not Found",
              "mensagem": "Serviço com id 99 não encontrado",
              "path": "/api/servicos/99"
            }
            """;

    private static final String EXEMPLO_ERRO_CONFLITO = """
            {
              "timestamp": "2026-09-22T10:15:30",
              "status": 409,
              "erro": "Conflict",
              "mensagem": "Já existe um serviço cadastrado com o nome 'Instalação de ar-condicionado'",
              "path": "/api/servicos"
            }
            """;

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(
            summary = "Cadastrar serviço",
            description = "Cadastra um novo serviço no catálogo. O nome deve ser único e o serviço é criado como ativo. "
                    + "Retorna o serviço criado e o header Location com a URL do novo recurso. "
                    + "Requer o papel PROVIDER ou ADMIN; o usuário autenticado é registrado como prestador do serviço."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Dados do serviço a ser cadastrado",
            required = true,
            content = @Content(schema = @Schema(implementation = ServicoRequest.class),
                    examples = @ExampleObject(name = "Serviço de manutenção", value = EXEMPLO_REQUEST))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_VALIDACAO))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário sem o papel PROVIDER ou ADMIN",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe um serviço com o mesmo nome",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_CONFLITO)))
    })
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request,
                                                 @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ServicoResponse criado = servicoService.criar(request, usuario);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(
            summary = "Listar serviços",
            description = "Retorna todos os serviços cadastrados. Opcionalmente filtra pela categoria informada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServicoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Categoria inválida",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<List<ServicoResponse>> listar(
            @Parameter(description = "Filtra os serviços por categoria", example = "MANUTENCAO")
            @RequestParam(required = false) CategoriaServico categoria) {
        return ResponseEntity.ok(servicoService.listar(categoria));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar serviço por ID",
            description = "Retorna os dados de um serviço específico a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID em formato inválido",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_NAO_ENCONTRADO)))
    })
    public ResponseEntity<ServicoResponse> buscarPorId(
            @Parameter(description = "ID do serviço", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(
            summary = "Atualizar serviço",
            description = "Atualiza todos os dados de um serviço existente. Todos os campos obrigatórios devem ser enviados. "
                    + "Permitido apenas ao prestador responsável pelo serviço ou a um ADMIN."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Novos dados do serviço",
            required = true,
            content = @Content(schema = @Schema(implementation = ServicoRequest.class),
                    examples = @ExampleObject(name = "Atualização de preço e duração", value = """
                            {
                              "nome": "Instalação de ar-condicionado",
                              "descricao": "Instalação completa de aparelho split até 18.000 BTUs.",
                              "preco": 420.00,
                              "duracaoMinutos": 150,
                              "categoria": "MANUTENCAO"
                            }
                            """))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_VALIDACAO))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_NAO_ENCONTRADO))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não é o prestador do serviço nem ADMIN",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe outro serviço com o mesmo nome",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<ServicoResponse> atualizar(
            @Parameter(description = "ID do serviço", example = "1") @PathVariable Long id,
            @Valid @RequestBody ServicoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(servicoService.atualizar(id, request, usuario));
    }

    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(
            summary = "Ativar ou desativar serviço",
            description = "Altera apenas o status do serviço. Serviços inativos continuam cadastrados, "
                    + "mas não devem ser oferecidos para contratação. "
                    + "Permitido apenas ao prestador responsável pelo serviço ou a um ADMIN."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Novo status do serviço",
            required = true,
            content = @Content(schema = @Schema(implementation = ServicoStatusRequest.class),
                    examples = @ExampleObject(name = "Desativar", value = "{ \"ativo\": false }"))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso",
                    content = @Content(schema = @Schema(implementation = ServicoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não é o prestador do serviço nem ADMIN",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_NAO_ENCONTRADO)))
    })
    public ResponseEntity<ServicoResponse> alterarStatus(
            @Parameter(description = "ID do serviço", example = "1") @PathVariable Long id,
            @Valid @RequestBody ServicoStatusRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(servicoService.alterarStatus(id, request.ativo(), usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(
            summary = "Excluir serviço",
            description = "Remove definitivamente um serviço do catálogo. "
                    + "Permitido apenas ao prestador responsável pelo serviço ou a um ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço excluído com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "403", description = "Usuário não é o prestador do serviço nem ADMIN",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class),
                            examples = @ExampleObject(value = EXEMPLO_ERRO_NAO_ENCONTRADO)))
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID do serviço", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        servicoService.excluir(id, usuario);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

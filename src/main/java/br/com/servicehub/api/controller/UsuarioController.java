package br.com.servicehub.api.controller;

import br.com.servicehub.api.config.OpenApiConfig;
import br.com.servicehub.api.dto.AlterarPapelRequest;
import br.com.servicehub.api.dto.AtualizarUsuarioRequest;
import br.com.servicehub.api.dto.UsuarioResponse;
import br.com.servicehub.api.exception.ErroResponse;
import br.com.servicehub.api.security.UsuarioAutenticado;
import br.com.servicehub.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Usuários", description = "Consulta e gerenciamento de usuários (requer autenticação)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para a operação",
                content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários", description = "Retorna todos os usuários. Exclusivo para ADMIN.")
    @ApiResponse(responseCode = "200", description = "Lista de usuários",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar o próprio perfil", description = "Retorna os dados do usuário autenticado.")
    @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado",
            content = @Content(schema = @Schema(implementation = UsuarioResponse.class)))
    public ResponseEntity<UsuarioResponse> meuPerfil(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(usuarioService.buscarPorId(usuario.getId(), usuario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID",
            description = "Retorna os dados de um usuário. Permitido ao próprio usuário ou a um ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @Parameter(description = "ID do usuário", example = "2") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, usuario));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar perfil",
            description = "Atualiza nome e telefone. Permitido ao próprio usuário ou a um ADMIN. "
                    + "E-mail, senha e papel não podem ser alterados por esta operação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "ID do usuário", example = "2") @PathVariable Long id,
            @Valid @RequestBody AtualizarUsuarioRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request, usuario));
    }

    @PatchMapping(value = "/{id}/papel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar papel do usuário",
            description = "Define o papel (ADMIN, PROVIDER ou CLIENT) de outro usuário. Exclusivo para ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papel alterado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Papel ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "Administrador tentando alterar o próprio papel",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<UsuarioResponse> alterarPapel(
            @Parameter(description = "ID do usuário", example = "2") @PathVariable Long id,
            @Valid @RequestBody AlterarPapelRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(usuarioService.alterarPapel(id, request.papel(), usuario));
    }
}

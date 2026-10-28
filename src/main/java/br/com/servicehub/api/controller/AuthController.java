package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.CadastroUsuarioRequest;
import br.com.servicehub.api.dto.LoginRequest;
import br.com.servicehub.api.dto.TokenResponse;
import br.com.servicehub.api.dto.UsuarioResponse;
import br.com.servicehub.api.exception.ErroResponse;
import br.com.servicehub.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Autenticação", description = "Cadastro de usuários e emissão de tokens JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/cadastro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Cadastrar usuário",
            description = "Cria um usuário com o papel CLIENT. A senha é armazenada como hash BCrypt. "
                    + "Campos de papel enviados no corpo são ignorados; somente um ADMIN pode alterar papéis."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroUsuarioRequest request) {
        UsuarioResponse criado = authService.cadastrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/usuarios/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Autenticar usuário",
            description = "Valida e-mail e senha e retorna um JWT assinado, que deve ser enviado no header "
                    + "Authorization: Bearer <token> das operações protegidas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

package br.com.servicehub.api.dto;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Dados de um usuário. O hash da senha nunca é exposto; e-mail e telefone só são
 * retornados ao próprio usuário ou a um ADMIN.
 */
@Schema(description = "Dados de um usuário retornados pela API")
public record UsuarioResponse(

        @Schema(description = "Identificador do usuário", example = "2")
        Long id,

        @Schema(description = "Nome completo", example = "Maria Souza")
        String nome,

        @Schema(description = "E-mail", example = "maria@email.com")
        String email,

        @Schema(description = "Papel do usuário", example = "CLIENT")
        PapelUsuario papel,

        @Schema(description = "Telefone de contato", example = "(62) 99999-0000")
        String telefone,

        @Schema(description = "Indica se o usuário está ativo", example = "true")
        Boolean ativo,

        @Schema(description = "Data e hora de cadastro", example = "2026-10-27T10:15:30")
        LocalDateTime criadoEm
) {

    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getTelefone(),
                usuario.getAtivo(),
                usuario.getCriadoEm()
        );
    }
}

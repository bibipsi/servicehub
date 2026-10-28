package br.com.servicehub.api.dto;

import br.com.servicehub.api.validation.SemHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados que o próprio usuário pode alterar. E-mail, senha, papel e status
 * não fazem parte deste contrato.
 */
@Schema(description = "Dados para atualizar o perfil do usuário")
public record AtualizarUsuarioRequest(

        @Schema(description = "Nome completo", example = "Maria Souza", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @SemHtml(message = "O nome não pode conter os caracteres < e >")
        String nome,

        @Schema(description = "Telefone de contato", example = "(62) 99999-0000")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^[0-9()+\\- ]*$", message = "O telefone deve conter apenas números, espaços, parênteses, + e -")
        String telefone
) {
}

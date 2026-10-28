package br.com.servicehub.api.dto;

import br.com.servicehub.api.validation.SemHtml;
import br.com.servicehub.api.validation.SenhaBcrypt;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do cadastro público. Não há campo de papel: todo usuário cadastrado por esta rota
 * é CLIENT, e campos extras enviados no JSON (como "papel" ou "role") são ignorados.
 */
@Schema(description = "Dados para cadastrar um novo usuário (criado sempre com o papel CLIENT)")
public record CadastroUsuarioRequest(

        @Schema(description = "Nome completo", example = "Maria Souza", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        @SemHtml(message = "O nome não pode conter os caracteres < e >")
        String nome,

        @Schema(description = "E-mail usado no login", example = "maria@email.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @Schema(description = "Senha de 8 a 72 caracteres, armazenada apenas como hash BCrypt",
                example = "SenhaForte123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        @SenhaBcrypt
        String senha,

        @Schema(description = "Telefone de contato", example = "(62) 99999-0000")
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^[0-9()+\\- ]*$", message = "O telefone deve conter apenas números, espaços, parênteses, + e -")
        String telefone
) {
}

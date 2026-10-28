package br.com.servicehub.api.dto;

import br.com.servicehub.api.entity.PapelUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo papel do usuário (operação exclusiva de ADMIN)")
public record AlterarPapelRequest(

        @Schema(description = "Papel a ser atribuído", example = "PROVIDER", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "O papel é obrigatório")
        PapelUsuario papel
) {
}

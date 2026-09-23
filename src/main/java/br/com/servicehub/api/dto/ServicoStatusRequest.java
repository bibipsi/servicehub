package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para ativar ou desativar um serviço")
public record ServicoStatusRequest(

        @Schema(description = "Indica se o serviço está disponível para contratação", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "O campo ativo é obrigatório")
        Boolean ativo
) {
}

package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso gerado no login")
public record TokenResponse(

        @Schema(description = "JWT assinado, enviado no header Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tipo,

        @Schema(description = "Validade do token em segundos", example = "3600")
        long expiraEmSegundos
) {
}

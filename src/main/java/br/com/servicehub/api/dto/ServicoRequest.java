package br.com.servicehub.api.dto;

import br.com.servicehub.api.entity.CategoriaServico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastrar ou atualizar um serviço")
public record ServicoRequest(

        @Schema(description = "Nome do serviço (único)", example = "Instalação de ar-condicionado",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,

        @Schema(description = "Descrição detalhada do serviço",
                example = "Instalação completa de aparelho split até 12.000 BTUs, incluindo suporte e tubulação de até 3 metros.",
                maxLength = 500)
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "Preço do serviço em reais", example = "350.00",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0.01")
        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal preco,

        @Schema(description = "Duração estimada do serviço em minutos", example = "120",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "1440")
        @NotNull(message = "A duração é obrigatória")
        @Min(value = 1, message = "A duração deve ser de pelo menos 1 minuto")
        @Max(value = 1440, message = "A duração deve ser de no máximo 1440 minutos (24h)")
        Integer duracaoMinutos,

        @Schema(description = "Categoria do serviço", example = "MANUTENCAO",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "A categoria é obrigatória")
        CategoriaServico categoria
) {
}

package br.com.servicehub.api.dto;

import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.entity.Servico;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Dados de um serviço retornados pela API")
public record ServicoResponse(

        @Schema(description = "Identificador do serviço", example = "1")
        Long id,

        @Schema(description = "Nome do serviço", example = "Instalação de ar-condicionado")
        String nome,

        @Schema(description = "Descrição detalhada do serviço",
                example = "Instalação completa de aparelho split até 12.000 BTUs, incluindo suporte e tubulação de até 3 metros.")
        String descricao,

        @Schema(description = "Preço do serviço em reais", example = "350.00")
        BigDecimal preco,

        @Schema(description = "Duração estimada em minutos", example = "120")
        Integer duracaoMinutos,

        @Schema(description = "Categoria do serviço", example = "MANUTENCAO")
        CategoriaServico categoria,

        @Schema(description = "Indica se o serviço está disponível para contratação", example = "true")
        Boolean ativo,

        @Schema(description = "Data e hora de cadastro", example = "2026-09-22T10:15:30")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora da última atualização", example = "2026-09-22T10:15:30")
        LocalDateTime atualizadoEm
) {

    public static ServicoResponse fromEntity(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getDuracaoMinutos(),
                servico.getCategoria(),
                servico.getAtivo(),
                servico.getCriadoEm(),
                servico.getAtualizadoEm()
        );
    }
}

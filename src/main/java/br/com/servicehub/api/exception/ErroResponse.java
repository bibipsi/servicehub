package br.com.servicehub.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Formato padrão de erro retornado pela API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(

        @Schema(description = "Momento em que o erro ocorreu", example = "2026-09-22T10:15:30")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP", example = "404")
        int status,

        @Schema(description = "Descrição do status HTTP", example = "Not Found")
        String erro,

        @Schema(description = "Mensagem explicando o erro", example = "Serviço com id 99 não encontrado")
        String mensagem,

        @Schema(description = "Caminho da requisição", example = "/api/servicos/99")
        String path,

        @Schema(description = "Erros de validação por campo (apenas em respostas 400 de validação)")
        List<CampoErro> campos
) {

    @Schema(description = "Erro de validação de um campo")
    public record CampoErro(
            @Schema(description = "Nome do campo", example = "preco")
            String campo,
            @Schema(description = "Mensagem de validação", example = "O preço deve ser maior que zero")
            String mensagem
    ) {
    }
}

package br.com.servicehub.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroResponse.CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou com formato inválido", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensagem = "Valor inválido para o parâmetro '%s': %s".formatted(ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, mensagem, request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método %s não suportado para este recurso".formatted(ex.getMethod()), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", request, null);
    }

    private ResponseEntity<ErroResponse> build(HttpStatus status, String mensagem, HttpServletRequest request,
                                               List<ErroResponse.CampoErro> campos) {
        ErroResponse body = new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                campos
        );
        return ResponseEntity.status(status).body(body);
    }
}

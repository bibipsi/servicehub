package br.com.servicehub.api.security;

import br.com.servicehub.api.exception.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Responde às falhas de segurança ocorridas nos filtros, antes de a requisição chegar aos
 * Controllers, no mesmo formato de {@link ErroResponse} usado pelo GlobalExceptionHandler:
 * 401 quando não há autenticação válida e 403 quando o usuário não tem permissão.
 */
@Component
public class ErroSegurancaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAGEM_AUTENTICACAO_NECESSARIA =
            "Autenticação necessária. Envie o token no header Authorization: Bearer <token>";
    static final String MENSAGEM_ACESSO_NEGADO = "Você não tem permissão para acessar este recurso";

    private final JsonMapper jsonMapper;

    public ErroSegurancaHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        String mensagem = ex instanceof BadCredentialsException ? ex.getMessage() : MENSAGEM_AUTENTICACAO_NECESSARIA;
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escrever(response, request, HttpStatus.UNAUTHORIZED, mensagem);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escrever(response, request, HttpStatus.FORBIDDEN, MENSAGEM_ACESSO_NEGADO);
    }

    private void escrever(HttpServletResponse response, HttpServletRequest request,
                          HttpStatus status, String mensagem) throws IOException {
        ErroResponse body = new ErroResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, request.getRequestURI(), null);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}

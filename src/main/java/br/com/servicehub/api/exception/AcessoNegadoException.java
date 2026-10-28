package br.com.servicehub.api.exception;

/**
 * Lançada quando o usuário autenticado tenta acessar ou alterar um recurso que pertence
 * a outro usuário (proteção contra BOLA). Convertida em 403 Forbidden.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}

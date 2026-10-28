package br.com.servicehub.api.security;

import br.com.servicehub.api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o header {@code Authorization: Bearer <token>}, valida o JWT e autentica o usuário
 * no SecurityContext da requisição. Sem token, a requisição segue como anônima e as regras
 * do SecurityFilterChain decidem o acesso; com token inválido ou expirado, responde 401.
 *
 * <p>O usuário é recarregado do banco a cada requisição, de modo que alterações de papel
 * e desativações passam a valer imediatamente, mesmo para tokens já emitidos.</p>
 *
 * <p>Não é anotado com {@code @Component} para que o Spring Boot não o registre também
 * como filtro do servlet, fora do SecurityFilterChain.</p>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository,
                         AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long usuarioId = jwtService.extrairUsuarioId(header.substring(PREFIXO_BEARER.length()).trim());
            UsuarioAutenticado usuario = usuarioRepository.findById(usuarioId)
                    .map(UsuarioAutenticado::de)
                    .filter(UsuarioAutenticado::isEnabled)
                    .orElseThrow(() -> new BadCredentialsException("Usuário do token não existe ou está inativo"));

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Token inválido ou expirado", ex));
            return;
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

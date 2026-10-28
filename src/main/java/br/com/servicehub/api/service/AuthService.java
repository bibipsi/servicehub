package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.CadastroUsuarioRequest;
import br.com.servicehub.api.dto.LoginRequest;
import br.com.servicehub.api.dto.TokenResponse;
import br.com.servicehub.api.dto.UsuarioResponse;
import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.exception.RegraNegocioException;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.security.UsuarioAutenticado;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroUsuarioRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com o e-mail '%s'".formatted(email));
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setTelefone(textoOuNulo(request.telefone()));
        // O papel é definido pelo servidor, nunca pelo corpo da requisição (proteção contra Mass Assignment)
        usuario.setPapel(PapelUsuario.CLIENT);

        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    public TokenResponse login(LoginRequest request) {
        Authentication autenticacao = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizarEmail(request.email()), request.senha()));
        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacao.getPrincipal();
        return new TokenResponse(jwtService.gerarToken(usuario), "Bearer", jwtService.getExpiracao().toSeconds());
    }

    static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

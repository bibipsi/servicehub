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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void cadastrarDeveCriarClienteComSenhaCriptografadaEEmailNormalizado() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte123")).thenReturn("$2a$10$hashGerado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        UsuarioResponse resposta = authService.cadastrar(
                new CadastroUsuarioRequest("Maria Souza", "  Maria@Email.COM ", "SenhaForte123", ""));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo("maria@email.com");
        assertThat(salvo.getSenhaHash()).isEqualTo("$2a$10$hashGerado").isNotEqualTo("SenhaForte123");
        assertThat(salvo.getPapel()).isEqualTo(PapelUsuario.CLIENT);
        assertThat(salvo.getTelefone()).isNull();
        assertThat(resposta.papel()).isEqualTo(PapelUsuario.CLIENT);
        verify(passwordEncoder).encode("SenhaForte123");
    }

    @Test
    void cadastrarDeveLancarExcecaoQuandoEmailJaExiste() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.cadastrar(
                new CadastroUsuarioRequest("Maria Souza", "maria@email.com", "SenhaForte123", null)))
                .isInstanceOf(RegraNegocioException.class);

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginDeveRetornarTokenQuandoCredenciaisValidas() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(2L, "maria@email.com", "hash", PapelUsuario.CLIENT, true);
        Authentication autenticado = UsernamePasswordAuthenticationToken.authenticated(usuario, null,
                usuario.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(autenticado);
        when(jwtService.gerarToken(usuario)).thenReturn("token-jwt");
        when(jwtService.getExpiracao()).thenReturn(Duration.ofHours(1));

        TokenResponse resposta = authService.login(new LoginRequest("MARIA@email.com", "SenhaForte123"));

        assertThat(resposta.token()).isEqualTo("token-jwt");
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.expiraEmSegundos()).isEqualTo(3600);
        verify(authenticationManager).authenticate(argThat(auth -> "maria@email.com".equals(auth.getPrincipal())));
    }

    @Test
    void loginDevePropagarFalhaDeAutenticacaoSemGerarToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@email.com", "errada")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).gerarToken(any());
    }
}

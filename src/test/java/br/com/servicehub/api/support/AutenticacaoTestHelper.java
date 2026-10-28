package br.com.servicehub.api.support;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.security.UsuarioAutenticado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Cria usuários diretamente no banco de teste e gera tokens JWT reais para eles,
 * de modo que as requisições passem pelo JwtAuthFilter como em produção.
 */
public class AutenticacaoTestHelper {

    public static final String SENHA_PADRAO = "SenhaForte123";

    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacaoTestHelper(UsuarioRepository usuarioRepository, ServicoRepository servicoRepository,
                                  PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.servicoRepository = servicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Remove serviços antes dos usuários, respeitando a chave estrangeira servicos.prestador_id. */
    public void limparBanco() {
        servicoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    public Usuario criarUsuario(String email, PapelUsuario papel) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + papel.name());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(SENHA_PADRAO));
        usuario.setPapel(papel);
        return usuarioRepository.save(usuario);
    }

    public String token(Usuario usuario) {
        return jwtService.gerarToken(UsuarioAutenticado.de(usuario));
    }

    /** Adiciona o header Authorization: Bearer com o token do usuário à requisição do MockMvc. */
    public RequestPostProcessor bearer(Usuario usuario) {
        String token = token(usuario);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }
}

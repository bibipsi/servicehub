package br.com.servicehub.api.config;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Cria o primeiro usuário ADMIN na inicialização quando ADMIN_EMAIL e ADMIN_PASSWORD
 * estão definidos e ainda não existe usuário com esse e-mail. O cadastro público sempre
 * cria usuários CLIENT; os demais papéis são atribuídos por um ADMIN.
 */
@Configuration
public class AdminInicialConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialConfig.class);

    @Bean
    public ApplicationRunner criarAdminInicial(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                               @Value("${servicehub.admin.email:}") String email,
                                               @Value("${servicehub.admin.senha:}") String senha) {
        return args -> {
            if (!StringUtils.hasText(email) || !StringUtils.hasText(senha)) {
                return;
            }
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
            if (usuarioRepository.existsByEmail(emailNormalizado)) {
                return;
            }
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail(emailNormalizado);
            admin.setSenhaHash(passwordEncoder.encode(senha));
            admin.setPapel(PapelUsuario.ADMIN);
            usuarioRepository.save(admin);
            log.info("Usuário ADMIN inicial criado para {}", emailNormalizado);
        };
    }
}

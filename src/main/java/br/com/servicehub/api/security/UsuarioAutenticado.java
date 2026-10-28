package br.com.servicehub.api.security;

import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuário autenticado mantido no SecurityContext. Expõe o id e o papel usados nas
 * verificações de permissão e de propriedade dos recursos.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String email;
    private final String senhaHash;
    private final PapelUsuario papel;
    private final boolean ativo;

    public UsuarioAutenticado(Long id, String email, String senhaHash, PapelUsuario papel, boolean ativo) {
        this.id = id;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.ativo = ativo;
    }

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getSenhaHash(),
                usuario.getPapel(), Boolean.TRUE.equals(usuario.getAtivo()));
    }

    public Long getId() {
        return id;
    }

    public PapelUsuario getPapel() {
        return papel;
    }

    public boolean isAdmin() {
        return papel == PapelUsuario.ADMIN;
    }

    /**
     * Indica se o usuário pode acessar um recurso pertencente a {@code donoId}:
     * apenas o próprio dono ou um ADMIN.
     */
    public boolean podeAcessarRecursoDe(Long donoId) {
        return isAdmin() || (donoId != null && donoId.equals(id));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[id=%d, papel=%s]".formatted(id, papel);
    }
}

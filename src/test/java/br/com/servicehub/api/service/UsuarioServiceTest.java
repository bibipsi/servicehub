package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.AtualizarUsuarioRequest;
import br.com.servicehub.api.dto.UsuarioResponse;
import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.exception.AcessoNegadoException;
import br.com.servicehub.api.exception.RecursoNaoEncontradoException;
import br.com.servicehub.api.exception.RegraNegocioException;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private final UsuarioAutenticado cliente = new UsuarioAutenticado(2L, "cliente@servicehub.com", "hash",
            PapelUsuario.CLIENT, true);
    private final UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin@servicehub.com", "hash",
            PapelUsuario.ADMIN, true);

    @Test
    void buscarPorIdDeveRetornarOsProprioDados() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, PapelUsuario.CLIENT)));

        UsuarioResponse resposta = usuarioService.buscarPorId(2L, cliente);

        assertThat(resposta.id()).isEqualTo(2L);
        verify(usuarioRepository).findById(2L);
    }

    @Test
    void buscarPorIdDeveNegarAcessoAosDadosDeOutroUsuario() {
        assertThatThrownBy(() -> usuarioService.buscarPorId(3L, cliente))
                .isInstanceOf(AcessoNegadoException.class);

        // A consulta nem chega ao banco, evitando revelar se o id existe
        verify(usuarioRepository, never()).findById(anyLong());
    }

    @Test
    void adminPodeConsultarQualquerUsuario() {
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, PapelUsuario.PROVIDER)));

        assertThat(usuarioService.buscarPorId(3L, admin).papel()).isEqualTo(PapelUsuario.PROVIDER);
    }

    @Test
    void atualizarDeveAlterarSomenteNomeETelefone() {
        Usuario usuario = usuario(2L, PapelUsuario.CLIENT);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        UsuarioResponse resposta = usuarioService.atualizar(2L,
                new AtualizarUsuarioRequest("  Maria Souza  ", "(62) 99999-0000"), cliente);

        assertThat(resposta.nome()).isEqualTo("Maria Souza");
        assertThat(resposta.telefone()).isEqualTo("(62) 99999-0000");
        assertThat(resposta.papel()).isEqualTo(PapelUsuario.CLIENT);
        verify(usuarioRepository).saveAndFlush(usuario);
    }

    @Test
    void alterarPapelDevePromoverUsuario() {
        Usuario usuario = usuario(2L, PapelUsuario.CLIENT);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        UsuarioResponse resposta = usuarioService.alterarPapel(2L, PapelUsuario.PROVIDER, admin);

        assertThat(resposta.papel()).isEqualTo(PapelUsuario.PROVIDER);
        verify(usuarioRepository).saveAndFlush(usuario);
    }

    @Test
    void adminNaoPodeAlterarOProprioPapel() {
        assertThatThrownBy(() -> usuarioService.alterarPapel(1L, PapelUsuario.CLIENT, admin))
                .isInstanceOf(RegraNegocioException.class);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void alterarPapelDeveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarPapel(99L, PapelUsuario.PROVIDER, admin))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private static Usuario usuario(Long id, PapelUsuario papel) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Usuário " + id);
        usuario.setEmail("usuario" + id + "@servicehub.com");
        usuario.setSenhaHash("hash");
        usuario.setPapel(papel);
        return usuario;
    }
}

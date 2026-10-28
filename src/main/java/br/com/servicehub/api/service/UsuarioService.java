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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id, UsuarioAutenticado usuarioAtual) {
        verificarPropriedade(id, usuarioAtual);
        return UsuarioResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, AtualizarUsuarioRequest request, UsuarioAutenticado usuarioAtual) {
        verificarPropriedade(id, usuarioAtual);
        Usuario usuario = buscarEntidade(id);
        usuario.setNome(request.nome().trim());
        usuario.setTelefone(AuthService.textoOuNulo(request.telefone()));
        return UsuarioResponse.fromEntity(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioResponse alterarPapel(Long id, PapelUsuario papel, UsuarioAutenticado usuarioAtual) {
        if (id.equals(usuarioAtual.getId())) {
            throw new RegraNegocioException("Um administrador não pode alterar o próprio papel");
        }
        Usuario usuario = buscarEntidade(id);
        usuario.setPapel(papel);
        return UsuarioResponse.fromEntity(usuarioRepository.saveAndFlush(usuario));
    }

    // A verificação ocorre antes da consulta para não revelar quais ids existem (proteção contra BOLA)
    private void verificarPropriedade(Long id, UsuarioAutenticado usuarioAtual) {
        if (!usuarioAtual.podeAcessarRecursoDe(id)) {
            throw new AcessoNegadoException("Você só pode acessar os seus próprios dados");
        }
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário com id %d não encontrado".formatted(id)));
    }
}

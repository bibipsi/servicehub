package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ServicoRequest;
import br.com.servicehub.api.dto.ServicoResponse;
import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.entity.Servico;
import br.com.servicehub.api.exception.AcessoNegadoException;
import br.com.servicehub.api.exception.RecursoNaoEncontradoException;
import br.com.servicehub.api.exception.RegraNegocioException;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;

    public ServicoService(ServicoRepository servicoRepository, UsuarioRepository usuarioRepository) {
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest request, UsuarioAutenticado usuarioAtual) {
        String nome = request.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe um serviço cadastrado com o nome '%s'".formatted(nome));
        }

        Servico servico = new Servico();
        preencherDados(servico, request);
        servico.setAtivo(true);
        // O prestador é sempre o usuário autenticado, nunca um valor enviado no corpo da requisição
        servico.setPrestador(usuarioRepository.getReferenceById(usuarioAtual.getId()));

        return ServicoResponse.fromEntity(servicoRepository.save(servico));
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar(CategoriaServico categoria) {
        List<Servico> servicos = categoria == null
                ? servicoRepository.findAll()
                : servicoRepository.findByCategoria(categoria);

        return servicos.stream()
                .map(ServicoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        return ServicoResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request, UsuarioAutenticado usuarioAtual) {
        Servico servico = buscarEntidade(id);
        verificarPermissao(servico, usuarioAtual);

        String nome = request.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe outro serviço cadastrado com o nome '%s'".formatted(nome));
        }

        preencherDados(servico, request);
        return ServicoResponse.fromEntity(servicoRepository.saveAndFlush(servico));
    }

    @Transactional
    public ServicoResponse alterarStatus(Long id, Boolean ativo, UsuarioAutenticado usuarioAtual) {
        Servico servico = buscarEntidade(id);
        verificarPermissao(servico, usuarioAtual);
        servico.setAtivo(ativo);
        return ServicoResponse.fromEntity(servicoRepository.saveAndFlush(servico));
    }

    @Transactional
    public void excluir(Long id, UsuarioAutenticado usuarioAtual) {
        Servico servico = buscarEntidade(id);
        verificarPermissao(servico, usuarioAtual);
        servicoRepository.delete(servico);
    }

    // Apenas o prestador dono do serviço ou um ADMIN podem alterá-lo (proteção contra BOLA).
    // Serviços sem prestador, cadastrados antes da autenticação, só podem ser alterados por ADMIN.
    private void verificarPermissao(Servico servico, UsuarioAutenticado usuarioAtual) {
        Long prestadorId = servico.getPrestador() == null ? null : servico.getPrestador().getId();
        if (!usuarioAtual.podeAcessarRecursoDe(prestadorId)) {
            throw new AcessoNegadoException("Somente o prestador responsável ou um administrador pode alterar este serviço");
        }
    }

    private Servico buscarEntidade(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço com id %d não encontrado".formatted(id)));
    }

    private void preencherDados(Servico servico, ServicoRequest request) {
        servico.setNome(request.nome().trim());
        servico.setDescricao(request.descricao() == null ? null : request.descricao().trim());
        servico.setPreco(request.preco());
        servico.setDuracaoMinutos(request.duracaoMinutos());
        servico.setCategoria(request.categoria());
    }
}

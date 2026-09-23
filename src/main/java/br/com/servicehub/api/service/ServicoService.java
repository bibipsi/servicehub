package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ServicoRequest;
import br.com.servicehub.api.dto.ServicoResponse;
import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.entity.Servico;
import br.com.servicehub.api.exception.RecursoNaoEncontradoException;
import br.com.servicehub.api.exception.RegraNegocioException;
import br.com.servicehub.api.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        String nome = request.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe um serviço cadastrado com o nome '%s'".formatted(nome));
        }

        Servico servico = new Servico();
        preencherDados(servico, request);
        servico.setAtivo(true);

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
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarEntidade(id);

        String nome = request.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe outro serviço cadastrado com o nome '%s'".formatted(nome));
        }

        preencherDados(servico, request);
        return ServicoResponse.fromEntity(servicoRepository.saveAndFlush(servico));
    }

    @Transactional
    public ServicoResponse alterarStatus(Long id, Boolean ativo) {
        Servico servico = buscarEntidade(id);
        servico.setAtivo(ativo);
        return ServicoResponse.fromEntity(servicoRepository.saveAndFlush(servico));
    }

    @Transactional
    public void excluir(Long id) {
        Servico servico = buscarEntidade(id);
        servicoRepository.delete(servico);
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

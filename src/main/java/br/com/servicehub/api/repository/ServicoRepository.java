package br.com.servicehub.api.repository;

import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByCategoria(CategoriaServico categoria);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}

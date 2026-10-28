package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ServicoRequest;
import br.com.servicehub.api.dto.ServicoResponse;
import br.com.servicehub.api.entity.CategoriaServico;
import br.com.servicehub.api.entity.PapelUsuario;
import br.com.servicehub.api.entity.Servico;
import br.com.servicehub.api.entity.Usuario;
import br.com.servicehub.api.exception.AcessoNegadoException;
import br.com.servicehub.api.exception.RecursoNaoEncontradoException;
import br.com.servicehub.api.exception.RegraNegocioException;
import br.com.servicehub.api.repository.ServicoRepository;
import br.com.servicehub.api.repository.UsuarioRepository;
import br.com.servicehub.api.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ServicoService servicoService;

    private final UsuarioAutenticado prestador = autenticado(10L, PapelUsuario.PROVIDER);
    private final UsuarioAutenticado outroPrestador = autenticado(20L, PapelUsuario.PROVIDER);
    private final UsuarioAutenticado admin = autenticado(1L, PapelUsuario.ADMIN);

    @Test
    void criarDeveSalvarServicoAtivoComPrestadorAutenticado() {
        Usuario usuarioPrestador = usuario(10L);
        when(servicoRepository.existsByNomeIgnoreCase("Faxina residencial")).thenReturn(false);
        when(usuarioRepository.getReferenceById(10L)).thenReturn(usuarioPrestador);
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ServicoResponse resposta = servicoService.criar(request("  Faxina residencial  "), prestador);

        ArgumentCaptor<Servico> captor = ArgumentCaptor.forClass(Servico.class);
        verify(servicoRepository).save(captor.capture());
        Servico salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Faxina residencial");
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(salvo.getPrestador()).isSameAs(usuarioPrestador);
        assertThat(resposta.prestadorId()).isEqualTo(10L);
    }

    @Test
    void criarDeveLancarExcecaoQuandoNomeJaExiste() {
        when(servicoRepository.existsByNomeIgnoreCase("Faxina residencial")).thenReturn(true);

        assertThatThrownBy(() -> servicoService.criar(request("Faxina residencial"), prestador))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Faxina residencial");

        verify(servicoRepository, never()).save(any());
    }

    @Test
    void listarDeveFiltrarPorCategoriaQuandoInformada() {
        when(servicoRepository.findByCategoria(CategoriaServico.LIMPEZA))
                .thenReturn(List.of(servico(5L, usuario(10L))));

        List<ServicoResponse> resposta = servicoService.listar(CategoriaServico.LIMPEZA);

        assertThat(resposta).hasSize(1);
        verify(servicoRepository).findByCategoria(CategoriaServico.LIMPEZA);
        verify(servicoRepository, never()).findAll();
    }

    @Test
    void buscarPorIdDeveLancarExcecaoQuandoServicoNaoExiste() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Serviço com id 99 não encontrado");
    }

    @Test
    void atualizarDevePermitirAoPrestadorDono() {
        Servico servico = servico(5L, usuario(10L));
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico));
        when(servicoRepository.existsByNomeIgnoreCaseAndIdNot("Faxina completa", 5L)).thenReturn(false);
        when(servicoRepository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse resposta = servicoService.atualizar(5L, request("Faxina completa"), prestador);

        assertThat(resposta.nome()).isEqualTo("Faxina completa");
        verify(servicoRepository).saveAndFlush(servico);
    }

    @Test
    void atualizarDeveNegarAcessoAoPrestadorQueNaoEDono() {
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico(5L, usuario(10L))));

        assertThatThrownBy(() -> servicoService.atualizar(5L, request("Faxina completa"), outroPrestador))
                .isInstanceOf(AcessoNegadoException.class);

        verify(servicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void alterarStatusDevePermitirAoAdminEmServicoDeOutroPrestador() {
        Servico servico = servico(5L, usuario(10L));
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico));
        when(servicoRepository.saveAndFlush(servico)).thenReturn(servico);

        ServicoResponse resposta = servicoService.alterarStatus(5L, false, admin);

        assertThat(resposta.ativo()).isFalse();
        verify(servicoRepository).saveAndFlush(servico);
    }

    @Test
    void servicoSemPrestadorSoPodeSerAlteradoPorAdmin() {
        Servico servicoLegado = servico(7L, null);
        when(servicoRepository.findById(7L)).thenReturn(Optional.of(servicoLegado));

        assertThatThrownBy(() -> servicoService.excluir(7L, prestador))
                .isInstanceOf(AcessoNegadoException.class);
        verify(servicoRepository, never()).delete(any());

        servicoService.excluir(7L, admin);
        verify(servicoRepository).delete(servicoLegado);
    }

    @Test
    void excluirDeveLancarExcecaoQuandoServicoNaoExiste() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoService.excluir(99L, prestador))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(servicoRepository, never()).delete(any());
    }

    private static ServicoRequest request(String nome) {
        return new ServicoRequest(nome, "Limpeza de todos os cômodos", new BigDecimal("150.00"), 180,
                CategoriaServico.LIMPEZA);
    }

    private static UsuarioAutenticado autenticado(Long id, PapelUsuario papel) {
        return new UsuarioAutenticado(id, "usuario" + id + "@servicehub.com", "hash", papel, true);
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setPapel(PapelUsuario.PROVIDER);
        return usuario;
    }

    private static Servico servico(Long id, Usuario prestador) {
        Servico servico = new Servico();
        ReflectionTestUtils.setField(servico, "id", id);
        servico.setNome("Faxina residencial");
        servico.setPreco(new BigDecimal("150.00"));
        servico.setDuracaoMinutos(180);
        servico.setCategoria(CategoriaServico.LIMPEZA);
        servico.setAtivo(true);
        servico.setPrestador(prestador);
        return servico;
    }
}

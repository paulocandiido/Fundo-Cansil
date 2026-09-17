package com.curso.services;

import com.curso.domains.GrupoProduto;
import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.domains.enums.Status;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import com.curso.services.exceptions.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes de unidade da camada Service para GrupoProdutoService (com Mockito).
 */
@ExtendWith(MockitoExtension.class)
class GrupoProdutoServiceUnitTest {

    @Mock
    private GrupoProdutoRepository grupoProdutoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private GrupoProdutoService service;

    @BeforeEach
    void setUp() {
        // Ajuste este construtor se seu service tiver assinatura diferente
        service = new GrupoProdutoService(grupoProdutoRepository, produtoRepository);
    }

    @Test
    @DisplayName("findAll deve retornar lista de DTOs mapeada do repositório")
    void deveListarTodosOsGrupos() {
        GrupoProduto g1 = new GrupoProduto();
        g1.setId(1);
        g1.setDescricao("Bebidas");
        g1.setStatus(Status.ATIVO);

        GrupoProduto g2 = new GrupoProduto();
        g2.setId(2);
        g2.setDescricao("Higiene");
        g2.setStatus(Status.INATIVO);

        when(grupoProdutoRepository.findAll()).thenReturn(List.of(g1, g2));

        List<GrupoProdutoDTO> result = service.findAll();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(g1.getId(), result.get(0).getId()),
                () -> assertEquals(g1.getDescricao(), result.get(0).getDescricao()),
                () -> assertEquals(g1.getStatus().getId(), result.get(0).getStatus()),
                () -> assertEquals(g2.getId(), result.get(1).getId()),
                () -> assertEquals(g2.getDescricao(), result.get(1).getDescricao()),
                () -> assertEquals(g2.getStatus().getId(), result.get(1).getStatus())
        );
    }

    // =======================
    // findById
    // =======================

    @Test
    @DisplayName("findById deve retornar DTO quando o grupo existir")
    void deveBuscarPorIdQuandoExistir() {
        Integer id = 10;
        GrupoProduto entity = new GrupoProduto();
        entity.setId(id);
        entity.setDescricao("Bebidas");
        entity.setStatus(Status.ATIVO);

        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.of(entity));

        GrupoProdutoDTO dto = service.findById(id);

        assertAll(
                () -> assertEquals(id, dto.getId()),
                () -> assertEquals("Bebidas", dto.getDescricao()),
                () -> assertEquals(Status.ATIVO.getId(), dto.getStatus())
        );
    }

    @Test
    @DisplayName("findById deve lançar 400 quando id é nulo")
    void deveLancar400QuandoIdNuloEmFindById() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findById(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("id é obrigatório", ex.getReason());
    }

    @Test
    @DisplayName("findById deve lançar 404 quando grupo não for encontrado")
    void deveLancar404QuandoNaoEncontradoEmFindById() {
        Integer id = 99;
        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.empty());

        ObjectNotFoundException ex = assertThrows(ObjectNotFoundException.class,
                () -> service.findById(id));

        assertEquals("Grupo de produto não encontrado: id=" + id, ex.getMessage());
    }

    // =======================
    // create
    // =======================

    @Test
    @DisplayName("create deve persistir e retornar DTO com id gerado")
    void deveCriarPersistirERetornarDto() {
        GrupoProdutoDTO entrada = new GrupoProdutoDTO();
        entrada.setDescricao("Higiene");
        entrada.setStatus(Status.ATIVO.getId());

        // simulamos a entidade persistida (com id gerado)
        GrupoProduto salvo = new GrupoProduto();
        salvo.setId(1);
        salvo.setDescricao("Higiene");
        salvo.setStatus(Status.ATIVO);

        when(grupoProdutoRepository.save(any(GrupoProduto.class))).thenReturn(salvo);

        GrupoProdutoDTO result = service.create(entrada);

        // captura a entidade enviada ao save
        ArgumentCaptor<GrupoProduto> captor = ArgumentCaptor.forClass(GrupoProduto.class);
        verify(grupoProdutoRepository).save(captor.capture());
        GrupoProduto enviado = captor.getValue();

        assertAll(
                () -> assertNull(enviado.getId(), "Ao criar, o ID deve ser null antes do save"),
                () -> assertEquals("Higiene", enviado.getDescricao()),
                () -> assertEquals(Status.ATIVO, enviado.getStatus()),
                () -> assertEquals(1, result.getId()),
                () -> assertEquals("Higiene", result.getDescricao()),
                () -> assertEquals(Status.ATIVO.getId(), result.getStatus())
        );
    }

    @Test
    @DisplayName("create deve lançar 400 quando DTO é nulo")
    void deveLancar400AoCriarComDtoNulo() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Dados do grupo são obrigatórios", ex.getReason());
        verify(grupoProdutoRepository, never()).save(any());
    }

    // =======================
    // update
    // =======================

    @Test
    @DisplayName("update deve atualizar e retornar DTO quando id e DTO são válidos e grupo existir")
    void deveAtualizarQuandoExistir() {
        Integer id = 7;

        GrupoProduto existente = new GrupoProduto();
        existente.setId(id);
        existente.setDescricao("Informática");
        existente.setStatus(Status.ATIVO);

        GrupoProdutoDTO dto = new GrupoProdutoDTO();
        dto.setId(id); // o service também seta internamente; manter coerência
        dto.setDescricao("Informática e Acessórios");
        dto.setStatus(Status.ATIVO.getId());

        GrupoProduto salvo = new GrupoProduto();
        salvo.setId(id);
        salvo.setDescricao("Informática e Acessórios");
        salvo.setStatus(Status.ATIVO);

        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(grupoProdutoRepository.save(any(GrupoProduto.class))).thenReturn(salvo);

        GrupoProdutoDTO result = service.update(id, dto);

        verify(grupoProdutoRepository).findById(id);
        verify(grupoProdutoRepository).save(any(GrupoProduto.class));

        assertAll(
                () -> assertEquals(id, result.getId()),
                () -> assertEquals("Informática e Acessórios", result.getDescricao()),
                () -> assertEquals(Status.ATIVO.getId(), result.getStatus())
        );
    }

    @Test
    @DisplayName("update deve lançar 400 quando id é nulo")
    void deveLancar400NoUpdateQuandoIdNulo() {
        GrupoProdutoDTO dto = new GrupoProdutoDTO();
        dto.setDescricao("Qualquer");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(null, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Id é obrigatório", ex.getReason());
        verify(grupoProdutoRepository, never()).findById(any());
        verify(grupoProdutoRepository, never()).save(any());
    }

    @Test
    @DisplayName("update deve lançar 400 quando DTO é nulo")
    void deveLancar400NoUpdateQuandoDtoNulo() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(1, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Dados do grupo produto são obrigatórios", ex.getReason());
        verify(grupoProdutoRepository, never()).findById(any());
        verify(grupoProdutoRepository, never()).save(any());
    }

    @Test
    @DisplayName("update deve lançar 404 quando grupo não existir")
    void deveLancar404NoUpdateQuandoGrupoInexistente() {
        Integer id = 123;
        GrupoProdutoDTO dto = new GrupoProdutoDTO();
        dto.setDescricao("Novo Nome");
        dto.setStatus(Status.ATIVO.getId());

        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.empty());

        ObjectNotFoundException ex = assertThrows(ObjectNotFoundException.class,
                () -> service.update(id, dto));

        assertEquals("Grupo de Produto não encontrado: id=" + id, ex.getMessage());
        verify(grupoProdutoRepository, never()).save(any());
    }

    // =======================
    // delete
    // =======================

    @Test
    @DisplayName("delete deve remover quando grupo existir e não houver produtos associados")
    void deveExcluirQuandoNaoPossuiProdutosAssociados() {
        Integer id = 5;
        GrupoProduto existente = new GrupoProduto();
        existente.setId(id);
        existente.setDescricao("Pet Shop");
        existente.setStatus(Status.ATIVO);

        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(produtoRepository.existsByGrupoProduto_Id(id)).thenReturn(false);

        service.delete(id);

        verify(grupoProdutoRepository).delete(existente);
    }

    @Test
    @DisplayName("delete deve lançar 400 quando id é nulo")
    void deveLancar400NoDeleteQuandoIdNulo() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Id é obrigatório", ex.getReason());
        verify(grupoProdutoRepository, never()).findById(any());
        verify(grupoProdutoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete deve lançar 404 quando grupo não for encontrado")
    void deveLancar404NoDeleteQuandoInexistente() {
        Integer id = 42;
        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.empty());

        ObjectNotFoundException ex = assertThrows(ObjectNotFoundException.class,
                () -> service.delete(id));

        assertEquals("Grupo de Produto não encontrado: id=" + id, ex.getMessage());
        verify(grupoProdutoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete deve lançar DataIntegrityViolationException quando houver produtos associados")
    void deveLancarDataIntegrityQuandoPossuiProdutosAssociados() {
        Integer id = 6;
        GrupoProduto existente = new GrupoProduto();
        existente.setId(id);
        existente.setDescricao("Mercearia");

        when(grupoProdutoRepository.findById(id)).thenReturn(Optional.of(existente));
        when(produtoRepository.existsByGrupoProduto_Id(id)).thenReturn(true);

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> service.delete(id));

        assertTrue(ex.getMessage().contains("Grupo de produto possui produtos associados"));
        verify(grupoProdutoRepository, never()).delete(any());
    }
}

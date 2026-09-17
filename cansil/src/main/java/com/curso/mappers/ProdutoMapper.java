package com.curso.mappers;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.dtos.ProdutoDTO;
import com.curso.domains.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapper manual (sem frameworks) para Produto.
 * - Entity -> DTO: enum Status vira int (0/1) e GrupoProduto vira grupoProdutoId.
 * - DTO -> Entity: int (0/1) vira enum Status; grupoProdutoId vira GrupoProduto (via resolver).
 * - NÃO seta valorEstoque na Entity (é calculado no domínio).
 */
public final class ProdutoMapper {

    private ProdutoMapper() {}

    /* ======================= Entity -> DTO ======================= */

    /** Converte uma Entity em DTO. */
    public static ProdutoDTO toDto(Produto e) {
        if (e == null) return null;

        // idProduto (Long) -> Long do DTO
        Long idDto = e.getIdProduto();

        Integer grupoId = (e.getGrupoProduto() == null) ? null : e.getGrupoProduto().getId();
        int statusInt = (e.getStatus() == null) ? 0 : e.getStatus().getId();

        // valorEstoque: tenta usar o getter calculado; se vier null, calcula localmente p/ exibição
        BigDecimal valorEstoque = e.getValorEstoque();
        if (valorEstoque == null && e.getValorUnitario() != null && e.getSaldoEstoque() != null) {
            valorEstoque = e.getValorUnitario().multiply(e.getSaldoEstoque());
        }

        return new ProdutoDTO(
                idDto,
                e.getDescricao(),
                e.getCodigoBarra(),
                grupoId,
                statusInt,
                e.getValorUnitario(),
                e.getSaldoEstoque(),
                valorEstoque
        );
    }

    /** Converte uma coleção de Entities em lista de DTOs. */
    public static List<ProdutoDTO> toDtoList(Collection<Produto> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(Objects::nonNull)
                .map(ProdutoMapper::toDto)
                .collect(Collectors.toList());
    }

    /** Converte Page<Entity> em Page<DTO> preservando a paginação. */
    public static Page<ProdutoDTO> toDtoPage(Page<Produto> page) {
        List<ProdutoDTO> content = toDtoList(page.getContent());
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /* ======================= DTO -> Entity ======================= */

    /**
     * Cria uma nova Entity a partir do DTO, usando o GrupoProduto já carregado.
     * Não seta valorEstoque (é calculado na Entity/serviço).
     */
    public static Produto toEntity(ProdutoDTO dto, GrupoProduto grupoProduto) {
        if (dto == null) return null;

        Produto e = new Produto();

        // idProduto do DTO (Long) -> Long da Entity
        e.setIdProduto(dto.getIdProduto());

        e.setDescricao(trim(dto.getDescricao()));
        e.setCodigoBarra(trim(dto.getCodigoBarra()));
        e.setGrupoProduto(grupoProduto); // pode ser null se DTO não trouxer grupo
        e.setStatus(Status.toEnum(dto.getStatus())); // int -> enum
        e.setValorUnitario(dto.getValorUnitario());
        e.setSaldoEstoque(dto.getSaldoEstoque());

        // NÃO setar e.setValorEstoque(...);  // calculado no domínio
        return e;
    }

    /**
     * Cria uma nova Entity a partir do DTO, resolvendo o GrupoProduto via função (repo).
     * Ex.: toEntity(dto, grupoRepo::getReferenceById) ou findById(...).orElseThrow(...)
     */
    public static Produto toEntity(ProdutoDTO dto, Function<Integer, GrupoProduto> grupoResolver) {
        if (dto == null) return null;
        GrupoProduto grupo = (dto.getGrupoProdutoId() == null) ? null : grupoResolver.apply(dto.getGrupoProdutoId());
        return toEntity(dto, grupo);
    }

    /**
     * Atualiza uma Entity existente a partir do DTO (PUT completo),
     * usando o GrupoProduto já carregado. Não altera o id do target.
     * NÃO seta valorEstoque (é calculado no domínio).
     */
    public static void copyToEntity(ProdutoDTO dto, Produto target, GrupoProduto grupoProduto) {
        if (dto == null || target == null) return;

        target.setDescricao(trim(dto.getDescricao()));
        target.setCodigoBarra(trim(dto.getCodigoBarra()));
        target.setGrupoProduto(grupoProduto);
        target.setStatus(Status.toEnum(dto.getStatus()));
        target.setValorUnitario(dto.getValorUnitario());
        target.setSaldoEstoque(dto.getSaldoEstoque());
        // NÃO setar target.setValorEstoque(...);
    }

    /**
     * Atualiza uma Entity existente a partir do DTO (PUT completo),
     * resolvendo o GrupoProduto via função. Não altera o id do target.
     */
    public static void copyToEntity(ProdutoDTO dto, Produto target, Function<Integer, GrupoProduto> grupoResolver) {
        if (dto == null || target == null) return;
        GrupoProduto grupo = (dto.getGrupoProdutoId() == null) ? null : grupoResolver.apply(dto.getGrupoProdutoId());
        copyToEntity(dto, target, grupo);
    }

    /* ======================= Helpers ======================= */

    private static String trim(String s) {
        return (s == null) ? null : s.trim();
    }
}

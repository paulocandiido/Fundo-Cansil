package com.curso.mappers;

import com.curso.domains.GrupoProduto;
import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.domains.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper manual (sem frameworks) para GrupoProduto.
 * - Entity -> DTO: enum Status vira int (0/1).
 * - DTO -> Entity: int (0/1) vira enum Status.
 * - copyToEntity: atualiza apenas campos mutáveis (não mexe no id).
 */
public final class GrupoProdutoMapper {

    private GrupoProdutoMapper() {}

    /** Converte uma Entity em DTO. */
    public static GrupoProdutoDTO toDto(GrupoProduto e) {
        if (e == null) return null;
        int statusInt = (e.getStatus() == null) ? 0 : e.getStatus().getId();
        return new GrupoProdutoDTO(
                e.getId(),
                e.getDescricao(),
                statusInt
        );
    }

    /** Cria uma nova Entity a partir do DTO (respeita id do DTO se presente). */
    public static GrupoProduto toEntity(GrupoProdutoDTO dto) {
        if (dto == null) return null;
        GrupoProduto e = new GrupoProduto();
        e.setId(dto.getId()); // se null, JPA gera; se não, usado no update
        e.setDescricao(dto.getDescricao() == null ? null : dto.getDescricao().trim());
        e.setStatus(Status.toEnum(dto.getStatus())); // int -> enum (0/1)
        return e;
    }

    /**
     * Copia dados do DTO para uma Entity existente (PUT “completo”).
     * Não altera o id da entidade alvo.
     */
    public static void copyToEntity(GrupoProdutoDTO dto, GrupoProduto target) {
        if (dto == null || target == null) return;
        target.setDescricao(dto.getDescricao() == null ? null : dto.getDescricao().trim());
        target.setStatus(Status.toEnum(dto.getStatus()));
    }

    /** Converte uma coleção de Entities em lista de DTOs. */
    public static List<GrupoProdutoDTO> toDtoList(Collection<GrupoProduto> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(Objects::nonNull)
                .map(GrupoProdutoMapper::toDto)
                .collect(Collectors.toList());
    }

    /** Converte uma coleção de DTOs em lista de Entities. */
    public static List<GrupoProduto> toEntityList(Collection<GrupoProdutoDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .filter(Objects::nonNull)
                .map(GrupoProdutoMapper::toEntity)
                .collect(Collectors.toList());
    }

    /** Converte Page<Entity> em Page<DTO> (preserva paginação). */
    public static Page<GrupoProdutoDTO> toDtoPage(Page<GrupoProduto> page) {
        List<GrupoProdutoDTO> content = toDtoList(page.getContent());
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

}

package com.curso.domains.dtos;

import jakarta.validation.constraints.*;

public class GrupoProdutoDTO {

    // Grupos de validação opcionais para usar no Controller/Service
    public interface Create {}
    public interface Update {}

    @Null(groups = Create.class, message = "Id deve ser omitido na criação")
    @NotNull(groups = Update.class, message = "Id é obrigatório na atualização")
    private Integer id;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 120, message = "Descrição deve ter no máximo 120 caracteres")
    private String descricao;

    @NotNull(message = "Status é obrigatório")
    @Min(value = 0, message = "Status inválido: use 0 (INATIVO) ou 1 (ATIVO)")
    @Max(value = 1, message = "Status inválido: use 0 (INATIVO) ou 1 (ATIVO)")
    private int status;

    public GrupoProdutoDTO() {    }

    public GrupoProdutoDTO(Integer id, String descricao, int status) {
        this.id = id;
        this.descricao = descricao;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "GrupoProdutoDTO{" +
                "id=" + id +
                ", descricao='" + descricao + '\'' +
                ", status=" + status +
                '}';
    }

}

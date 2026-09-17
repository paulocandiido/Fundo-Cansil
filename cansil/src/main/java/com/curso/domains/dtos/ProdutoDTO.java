package com.curso.domains.dtos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO de Produto
 * - Mantém status como inteiro (0/1) para o front-end.
 * - Expõe o relacionamento com Grupo por meio de grupoProdutoId.
 * - Valores monetários/quantidades em BigDecimal.
 */
public class ProdutoDTO {

    public interface Create {}
    public interface Update {}

    @Null(groups = Create.class, message = "Id deve ser omitido na criação")
    @NotNull(groups = Update.class, message = "Id é obrigatório na atualização")
    private Long idProduto;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 150, message = "Descrição deve ter no máximo 150 caracteres")
    private String descricao;

    @NotBlank(message = "Código de barras é obrigatório")
    @Size(max = 50, message = "Código de barras deve ter no máximo 50 caracteres")
    private String codigoBarra;

    @NotNull(message = "Grupo do produto é obrigatório")
    private Integer grupoProdutoId;

    /**
     * status: 0 = INATIVO, 1 = ATIVO (tratado no front-end)
     */
    @Min(value = 0, message = "Status inválido: use 0 (INATIVO) ou 1 (ATIVO)")
    @Max(value = 1, message = "Status inválido: use 0 (INATIVO) ou 1 (ATIVO)")
    private int status;

    @NotNull(message = "Valor unitário é obrigatório")
    @Digits(integer = 12, fraction = 2, message = "Valor unitário deve ter no máximo 12 inteiros e 2 decimais")
    @PositiveOrZero(message = "Valor unitário não pode ser negativo")
    private BigDecimal valorUnitario;

    @Digits(integer = 12, fraction = 3, message = "Saldo de estoque deve ter no máximo 12 inteiros e 3 decimais")
    @PositiveOrZero(message = "Saldo de estoque não pode ser negativo")
    private BigDecimal saldoEstoque;

    /**
     * valorEstoque pode ser calculado no back (saldo * valor) e apenas exposto no DTO.
     * Se preferir, torne-o somente leitura na resposta e ignore em requisições de entrada.
     */
    @Digits(integer = 14, fraction = 2, message = "Valor de estoque deve ter no máximo 14 inteiros e 2 decimais")
    @PositiveOrZero(message = "Valor de estoque não pode ser negativo")
    private BigDecimal valorEstoque;

    public ProdutoDTO() {
    }

    public ProdutoDTO(
            Long idProduto,
            String descricao,
            String codigoBarra,
            Integer grupoProdutoId,
            int status,
            BigDecimal valorUnitario,
            BigDecimal saldoEstoque,
            BigDecimal valorEstoque
    ) {
        this.idProduto = idProduto;
        this.descricao = descricao;
        this.codigoBarra = codigoBarra;
        this.grupoProdutoId = grupoProdutoId;
        this.status = status;
        this.valorUnitario = valorUnitario;
        this.saldoEstoque = saldoEstoque;
        this.valorEstoque = valorEstoque;
    }

    public Long getIdProduto() {
        return idProduto;
    }
    public void setIdProduto(Long idProduto) {
        this.idProduto = idProduto;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = (descricao == null ? null : descricao.trim());
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }
    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = (codigoBarra == null ? null : codigoBarra.trim());
    }

    public Integer getGrupoProdutoId() {
        return grupoProdutoId;
    }
    public void setGrupoProdutoId(Integer grupoProdutoId) {
        this.grupoProdutoId = grupoProdutoId;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }
    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getSaldoEstoque() {
        return saldoEstoque;
    }
    public void setSaldoEstoque(BigDecimal saldoEstoque) {
        this.saldoEstoque = saldoEstoque;
    }

    public BigDecimal getValorEstoque() {
        return valorEstoque;
    }
    public void setValorEstoque(BigDecimal valorEstoque) {
        this.valorEstoque = valorEstoque;
    }
}

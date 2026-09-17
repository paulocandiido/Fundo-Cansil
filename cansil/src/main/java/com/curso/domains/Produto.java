package com.curso.domains;

import com.curso.domains.enums.Status;
import com.curso.infra.StatusConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name="produto")
@SequenceGenerator(
        name = "seq_produto",          // mesmo nome usado no @GeneratedValue
        sequenceName = "seq_produto",  // nome da sequência no banco
        allocationSize = 1                  // incrementa de 1 em 1 (evita “saltos”)
)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_produto")
    private Long idProduto;

    @NotBlank
    @Column(name="codigobarra", nullable=false, length=50, unique = true)
    private String codigoBarra;

    @NotBlank
    @Column(nullable=false, length=100)
    private String descricao;

    @NotNull
    @Digits(integer = 15, fraction = 3)
    @Column(precision = 18, scale = 3, nullable = false)
    private BigDecimal saldoEstoque;

    @NotNull
    @Digits(integer = 15, fraction = 3)
    @Column(precision = 18, scale = 3, nullable = false)
    private BigDecimal valorUnitario;

    @NotNull
    @Digits(integer = 15, fraction = 2)
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal valorEstoque;

    @JsonFormat(pattern = "dd/MM/yyyy")
    @Column(nullable = false)
    private LocalDate  dataCadastro = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idgrupoproduto", nullable = false)
    @JsonBackReference // <- evita recursão infinita com @JsonManagedReference
    private GrupoProduto grupoProduto;

    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false)
    private Status status;

    public Produto() {
        this.saldoEstoque = BigDecimal.ZERO;
        this.valorUnitario = BigDecimal.ZERO;
        this.valorEstoque = BigDecimal.ZERO;
        this.status = Status.ATIVO;
    }

    public Produto(Long idProduto, String codigoBarra, String descricao, BigDecimal saldoEstoque, BigDecimal valorUnitario, LocalDate dataCadastro, GrupoProduto grupoProduto, Status status) {
        this.idProduto = idProduto;
        this.codigoBarra = codigoBarra;
        this.descricao = descricao;
        this.saldoEstoque = saldoEstoque != null ? saldoEstoque : BigDecimal.ZERO;
        this.valorUnitario = valorUnitario != null ? valorUnitario : BigDecimal.ZERO;
        this.dataCadastro = dataCadastro;
        this.grupoProduto = grupoProduto;
        this.status = status;
        this.valorEstoque = this.saldoEstoque.multiply(this.valorUnitario)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Long idProduto) {
        this.idProduto = idProduto;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getSaldoEstoque() {
        return saldoEstoque;
    }

    public void setSaldoEstoque(BigDecimal saldoEstoque) {
        this.saldoEstoque = saldoEstoque;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getValorEstoque() {
        return valorEstoque;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public GrupoProduto getGrupoProduto() {
        return grupoProduto;
    }

    public void setGrupoProduto(GrupoProduto grupoProduto) {
        this.grupoProduto = grupoProduto;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Produto produto = (Produto) o;
        return Objects.equals(idProduto, produto.idProduto) && Objects.equals(codigoBarra, produto.codigoBarra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProduto, codigoBarra);
    }

    @PrePersist @PreUpdate
    private void recalcValorEstoque() {
        BigDecimal saldo = (saldoEstoque  != null ? saldoEstoque  : BigDecimal.ZERO);
        BigDecimal valor = (valorUnitario != null ? valorUnitario : BigDecimal.ZERO);
        this.valorEstoque = saldo.multiply(valor).setScale(2, RoundingMode.HALF_UP);
    }


}

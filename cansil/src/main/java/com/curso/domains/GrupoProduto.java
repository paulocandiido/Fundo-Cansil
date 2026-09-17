package com.curso.domains;

import com.curso.domains.enums.Status;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name="grupoproduto")
@SequenceGenerator(
        name = "seq_grupoproduto",          // mesmo nome usado no @GeneratedValue
        sequenceName = "seq_grupoproduto",  // nome da sequência no banco
        allocationSize = 1                  // incrementa de 1 em 1 (evita “saltos”)
)
public class GrupoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_grupoproduto")
    private Integer id;
    @NotBlank
    @Column(nullable=false, length=120)
    private String descricao;
    @Convert(converter = com.curso.infra.StatusConverter.class)
    @Column(name = "status", nullable = false)
    private Status status;

    // ===== RELAÇÃO INVERSA (um->muitos) =====
    @JsonManagedReference // <- opcional (ver observação abaixo)
    @OneToMany(
            mappedBy = "grupoProduto",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // ajuste conforme sua regra
            orphanRemoval = false, // true só se quiser remover filhos “órfãos”
            fetch = FetchType.LAZY
    )
    @OrderBy("descricao ASC") // opcional: ordena a coleção por campo
    private List<Produto> produtos = new ArrayList<>();

    public GrupoProduto() {
        this.status = Status.ATIVO;
    }

    public GrupoProduto(Integer id, String descricao, Status status) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setProdutos(List<Produto> produtos) { this.produtos = produtos; }

    public List<Produto> getProdutos() {
        return produtos;
    }

    public void addProduto(Produto p) {
        if (p == null) return;
        produtos.add(p);
        p.setGrupoProduto(this);
    }

    public void removeProduto(Produto p) {
        if (p == null) return;
        produtos.remove(p);
        if (p.getGrupoProduto() == this) p.setGrupoProduto(null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GrupoProduto that = (GrupoProduto) o;
        return Objects.equals(id, that.id) && Objects.equals(descricao, that.descricao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, descricao);
    }
}

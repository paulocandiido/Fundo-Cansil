package com.curso.domains;

import jakarta.persistence.*;
import com.curso.domains.enums.Mercado;
import java.time.Instant;

@Entity
@Table(name = "corretora")
public class Corretora {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_corretora")
    @SequenceGenerator(name = "seq_corretora", sequenceName = "seq_corretora", allocationSize = 1)
    private Long id;
    @Column(nullable = false, unique = true, length = 80) private String codigo;
    @Column(nullable = false, length = 254) private String nome;
    @Column(nullable = false) private boolean ativa;
    @Column(nullable = false) private boolean legada;
    @Column(nullable = false) private boolean removida;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @Column(length = 14) private String cnpj;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3) private Mercado mercado = Mercado.BR;
    @Column(name = "verificado_em") private Instant verificadoEm;
    protected Corretora() {}
    public Corretora(Long id, String codigo, String nome, boolean ativa, boolean legada) { this.id=id; this.codigo=codigo; this.nome=nome; this.ativa=ativa; this.legada=legada; }
    public Corretora(Usuario usuario, String cnpj, String nome, Mercado mercado, boolean ativa, Instant verificadoEm) {
        this(null, java.util.UUID.randomUUID().toString(), nome, ativa, false);
        this.usuario=usuario; this.cnpj=cnpj; this.mercado=mercado; this.verificadoEm=verificadoEm;
    }
    public void regularizar(String cnpj, String nome, boolean ativa, Instant verificadoEm) {
        this.cnpj=cnpj; this.nome=nome; this.ativa=ativa; this.verificadoEm=verificadoEm; this.legada=false;
    }
    public String getCnpj(){return cnpj;} public Mercado getMercado(){return mercado;} public Instant getVerificadoEm(){return verificadoEm;}
    public Usuario getUsuario(){return usuario;}
    public boolean isRemovida(){return removida;}
    public void remover(){removida=true;}
    public void restaurar(){removida=false;}
    public Long getId(){return id;} public String getCodigo(){return codigo;} public String getNome(){return nome;} public boolean isAtiva(){return ativa;} public boolean isLegada(){return legada;}
}

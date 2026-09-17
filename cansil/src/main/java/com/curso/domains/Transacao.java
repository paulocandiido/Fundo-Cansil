package com.curso.domains;

import com.curso.domains.enums.TipoTransacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="transacao")
public class Transacao {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="seq_transacao")
    @SequenceGenerator(name="seq_transacao",sequenceName="seq_transacao",allocationSize=1)
    private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="usuario_id",nullable=false) private Usuario usuario;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="ativo_id",nullable=false) private Ativo ativo;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="corretora_id",nullable=false) private Corretora corretora;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private TipoTransacao tipo;
    @Column(nullable=false,precision=19,scale=6) private BigDecimal quantidade;
    @Column(name="valor_unitario",nullable=false,precision=19,scale=6) private BigDecimal valorUnitario;
    @Column(name="data_operacao",nullable=false) private LocalDateTime dataOperacao;
    protected Transacao() {}
    public Transacao(Long id,Usuario usuario,Ativo ativo,Corretora corretora,TipoTransacao tipo,BigDecimal quantidade,BigDecimal valorUnitario,LocalDateTime dataOperacao){this.id=id;this.usuario=usuario;this.ativo=ativo;this.corretora=corretora;this.tipo=tipo;this.quantidade=quantidade;this.valorUnitario=valorUnitario;this.dataOperacao=dataOperacao;}
    public Long getId(){return id;} public Usuario getUsuario(){return usuario;} public Ativo getAtivo(){return ativo;} public Corretora getCorretora(){return corretora;} public TipoTransacao getTipo(){return tipo;} public BigDecimal getQuantidade(){return quantidade;} public BigDecimal getValorUnitario(){return valorUnitario;} public LocalDateTime getDataOperacao(){return dataOperacao;}
}

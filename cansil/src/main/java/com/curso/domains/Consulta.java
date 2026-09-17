package com.curso.domains;

import com.curso.domains.enums.FonteCotacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="consulta")
public class Consulta {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="seq_consulta")
    @SequenceGenerator(name="seq_consulta",sequenceName="seq_consulta",allocationSize=1)
    private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="usuario_id",nullable=false) private Usuario usuario;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="ativo_id",nullable=false) private Ativo ativo;
    @Column(nullable=false,precision=19,scale=6) private BigDecimal valor;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private FonteCotacao fonte;
    @Column(name="consultado_em",nullable=false) private LocalDateTime consultadoEm;
    protected Consulta() {}
    public Consulta(Long id,Usuario usuario,Ativo ativo,BigDecimal valor,FonteCotacao fonte,LocalDateTime consultadoEm){this.id=id;this.usuario=usuario;this.ativo=ativo;this.valor=valor;this.fonte=fonte;this.consultadoEm=consultadoEm;}
    public Long getId(){return id;} public Usuario getUsuario(){return usuario;} public Ativo getAtivo(){return ativo;} public BigDecimal getValor(){return valor;} public FonteCotacao getFonte(){return fonte;} public LocalDateTime getConsultadoEm(){return consultadoEm;}
}

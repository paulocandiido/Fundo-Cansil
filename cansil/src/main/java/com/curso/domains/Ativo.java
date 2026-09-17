package com.curso.domains;

import jakarta.persistence.*;

@Entity @Table(name="ativo")
public class Ativo {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="seq_ativo")
    @SequenceGenerator(name="seq_ativo",sequenceName="seq_ativo",allocationSize=1)
    private Long id;
    @Column(nullable=false,unique=true,length=20) private String simbolo;
    @Column(nullable=false,length=160) private String nome;
    @Column(nullable=false,length=30) private String bolsa;
    @Column(nullable=false,length=3,updatable=false) private String moeda="BRL";
    protected Ativo() {}
    public Ativo(Long id,String simbolo,String nome,String bolsa){this.id=id;this.simbolo=simbolo;this.nome=nome;this.bolsa=bolsa;}
    public Ativo(Long id,String simbolo,String nome,String bolsa,String moeda){this(id,simbolo,nome,bolsa);this.moeda=com.curso.cotacao.Moedas.normalizar(moeda);}
    public String getMoeda(){return moeda;}
    public Long getId(){return id;} public String getSimbolo(){return simbolo;} public String getNome(){return nome;} public String getBolsa(){return bolsa;}
    public void setId(Long id){this.id=id;} public void setSimbolo(String simbolo){this.simbolo=simbolo;} public void setNome(String nome){this.nome=nome;} public void setBolsa(String bolsa){this.bolsa=bolsa;}
}

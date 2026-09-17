package com.curso.domains;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_usuario")
    @SequenceGenerator(name = "seq_usuario", sequenceName = "seq_usuario", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 120) private String nome;
    @Column(nullable = false, unique = true, length = 160) private String email;
    @Column(nullable = false, unique = true, length = 14) private String cpf;
    @Column(name = "senha_hash", nullable = false, length = 100) private String senhaHash;
    protected Usuario() {}
    public Usuario(Long id, String nome, String email, String cpf, String senhaHash) { this.id=id; this.nome=nome; this.email=email; this.cpf=cpf; this.senhaHash=senhaHash; }
    public Long getId(){return id;} public String getNome(){return nome;} public String getEmail(){return email;} public String getCpf(){return cpf;} public String getSenhaHash(){return senhaHash;}
    public void setId(Long id){this.id=id;} public void setNome(String nome){this.nome=nome;} public void setEmail(String email){this.email=email;} public void setCpf(String cpf){this.cpf=cpf;} public void setSenhaHash(String senhaHash){this.senhaHash=senhaHash;}
}

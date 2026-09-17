package com.curso.services;

import com.curso.domains.Usuario;
import com.curso.domains.dtos.AuthDTOs.*;
import com.curso.repositories.UsuarioRepository;
import com.curso.security.JwtService;
import com.curso.services.exceptions.ConflitoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository repo,PasswordEncoder encoder,JwtService jwt) {
        this.repo=repo;
        this.encoder=encoder;
        this.jwt=jwt;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest dados) {
        if (dados==null||!senhaAceita(dados.senha())||dados.senha().length()<8)
            throw new IllegalArgumentException("Senha inválida");
        String email=dados.email().trim().toLowerCase(Locale.ROOT);
        String cpf=dados.cpf().replaceAll("\\D","");
        if (repo.existsByEmailIgnoreCase(email)||repo.existsByCpf(cpf))
            throw new ConflitoException("E-mail ou CPF já cadastrado");
        try {
            // A consulta prévia não elimina a corrida entre cadastros simultâneos.
            Usuario usuario=repo.saveAndFlush(new Usuario(null,dados.nome().trim(),email,cpf,encoder.encode(dados.senha())));
            return new UsuarioResponse(usuario.getId(),usuario.getNome(),usuario.getEmail());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("E-mail ou CPF já cadastrado");
        }
    }

    @Transactional(readOnly=true)
    public TokenResponse login(LoginRequest dados) {
        if (dados==null||dados.email()==null||!senhaAceita(dados.senha()))
            throw new BadCredentialsException("Credenciais inválidas");
        Usuario usuario=repo.findByEmailIgnoreCase(dados.email().trim())
            .orElseThrow(()->new BadCredentialsException("Credenciais inválidas"));
        if (!encoder.matches(dados.senha(),usuario.getSenhaHash()))
            throw new BadCredentialsException("Credenciais inválidas");
        return new TokenResponse(jwt.emitir(usuario.getEmail()),"Bearer",jwt.expiracao());
    }

    private boolean senhaAceita(String senha) {
        return senha!=null&&!senha.isBlank()&&senha.getBytes(StandardCharsets.UTF_8).length<=72;
    }
}

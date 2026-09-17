package com.curso.security;
import com.curso.config.JwtProperties;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest{private static final String SECRET="chave-de-testes-com-mais-de-trinta-e-dois-bytes";
 @Test void emiteEValida(){JwtService s=new JwtService(new JwtProperties(SECRET,60));assertEquals("u@teste.com",s.validar(s.emitir("u@teste.com")));}
 @Test void rejeitaAssinaturaInvalida(){JwtService a=new JwtService(new JwtProperties(SECRET,60));JwtService b=new JwtService(new JwtProperties("outra-chave-de-testes-com-mais-de-trinta-e-dois",60));assertThrows(RuntimeException.class,()->b.validar(a.emitir("u@teste.com")));}
 @Test void rejeitaExpirado(){JwtService s=new JwtService(new JwtProperties(SECRET,-1));assertThrows(RuntimeException.class,()->s.validar(s.emitir("u@teste.com")));}}

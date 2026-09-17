package com.curso.services;

import com.curso.cotacao.CotacaoOrchestrator;
import com.curso.domains.dtos.AuthDTOs.*;
import com.curso.domains.dtos.InvestimentoDTOs.TransacaoRequest;
import com.curso.domains.enums.TipoTransacao;
import com.curso.repositories.*;
import com.curso.security.JwtService;
import com.curso.services.exceptions.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewRegressionTest {
    @Test void corridaDeCadastroRetornaConflitoSemDetalhesDoBanco() {
        var repo=mock(UsuarioRepository.class);
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("SQL segredo 12345678901"));
        var service=new AuthService(repo,mock(PasswordEncoder.class),mock(JwtService.class));
        var error=assertThrows(ConflitoException.class,()->service.cadastrar(new CadastroRequest("Ana","ana@test.com","12345678901","senha123")));
        assertEquals("E-mail ou CPF já cadastrado",error.getMessage());
        assertNull(error.getCause());
    }
    @Test void loginComMaisDe72BytesNaoChegaAoBcrypt() {
        var repo=mock(UsuarioRepository.class);
        var encoder=mock(PasswordEncoder.class);
        var service=new AuthService(repo,encoder,mock(JwtService.class));
        assertThrows(BadCredentialsException.class,()->service.login(new LoginRequest("a@test.com","é".repeat(40))));
        verifyNoInteractions(repo,encoder);
    }
    @Test void loginNormalizaEspacosDoEmail() {
        var repo=mock(UsuarioRepository.class);
        var service=new AuthService(repo,mock(PasswordEncoder.class),mock(JwtService.class));
        assertThrows(BadCredentialsException.class,()->service.login(new LoginRequest(" a@test.com ","senha123")));
        verify(repo).findByEmailIgnoreCase("a@test.com");
    }
    @Test void cadastroRejeitaSenhaCurtaMesmoForaDaApi() {
        var repo=mock(UsuarioRepository.class);
        var service=new AuthService(repo,mock(PasswordEncoder.class),mock(JwtService.class));
        assertThrows(IllegalArgumentException.class,()->service.cadastrar(new CadastroRequest("Ana","a@test.com","12345678901","a")));
        verifyNoInteractions(repo);
    }
    @Test void transacaoRejeitaDecimaisQueSeriamArredondadosNoBanco() {
        verificarRejeicao(new BigDecimal("0.0000001"));
    }
    @Test void transacaoRejeitaValorAcimaDaPrecisaoDoBanco() {
        verificarRejeicao(new BigDecimal("10000000000000"));
    }
    private void verificarRejeicao(BigDecimal quantidade) {
        var repo=mock(TransacaoRepository.class);
        var atual=mock(UsuarioAtualService.class);
        var service=new TransacaoService(repo,mock(AtivoRepository.class),atual,new CalculadoraPosicao(),mock(CotacaoOrchestrator.class),mock(CorretoraService.class),java.time.Clock.systemUTC());
        assertThrows(RegraNegocioException.class,()->service.registrar(new TransacaoRequest("PETR4",1L,TipoTransacao.COMPRA,quantidade,BigDecimal.TEN,LocalDateTime.now())));
        verifyNoInteractions(repo,atual);
    }
}

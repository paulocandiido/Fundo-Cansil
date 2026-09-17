package com.curso.services;

import com.curso.cotacao.CotacaoOrchestrator;
import com.curso.domains.*;
import com.curso.domains.dtos.InvestimentoDTOs.TransacaoRequest;
import com.curso.domains.enums.TipoTransacao;
import com.curso.domains.enums.Mercado;
import com.curso.repositories.*;
import com.curso.services.exceptions.RegraNegocioException;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransacaoServiceTest {
    final TransacaoRepository repo = mock(TransacaoRepository.class);
    final AtivoRepository ativos = mock(AtivoRepository.class);
    final UsuarioAtualService atual = mock(UsuarioAtualService.class);
    final Usuario usuario = new Usuario(1L,"Teste","teste@test.com","12345678901","hash");
    final Ativo ativo = new Ativo(2L,"PETR4","Petrobras","B3");
    final LocalDateTime data = LocalDateTime.of(2026,9,1,10,0);
    final CotacaoOrchestrator orq=mock(CotacaoOrchestrator.class);
    final Corretora corretora=new Corretora(10L,"XP","XP Investimentos",true,false);
    final CorretoraService corretoras=mock(CorretoraService.class);
    final Clock relogio=Clock.fixed(Instant.parse("2026-09-08T15:34:56Z"),ZoneId.of("America/Sao_Paulo"));
    final TransacaoService service = new TransacaoService(repo,ativos,atual,new CalculadoraPosicao(),orq,corretoras,relogio);

    @BeforeEach void preparar() {
        when(atual.bloquear()).thenReturn(usuario);
        when(ativos.findBySimboloIgnoreCase("PETR4")).thenReturn(Optional.of(ativo));
        when(corretoras.obterParaOperacao(eq(10L),any())).thenReturn(corretora);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    Transacao compra() { return new Transacao(1L,usuario,ativo,corretora,TipoTransacao.COMPRA,BigDecimal.TEN,BigDecimal.TEN,data); }
    TransacaoRequest venda(int quantidade, LocalDateTime momento) { return new TransacaoRequest("PETR4",10L,TipoTransacao.VENDA,BigDecimal.valueOf(quantidade),BigDecimal.TEN,momento); }

    @Test void vendaComSaldoUsaLockDoUsuario() {
        when(repo.findForUpdate(1L,2L)).thenReturn(List.of(compra()));
        assertEquals(TipoTransacao.VENDA,service.registrar(venda(5,data.plusHours(1))).tipo());
        verify(atual).bloquear(); verify(repo).save(any());
    }
    @Test void rejeitaVendaSemSaldoSemGravar() {
        when(repo.findForUpdate(1L,2L)).thenReturn(List.of(compra()));
        assertThrows(RegraNegocioException.class,()->service.registrar(venda(11,data.plusHours(1))));
        verify(repo,never()).save(any());
    }
    @Test void ignoraDataDoClienteERegistraHorarioAtualDoServidor() {
        when(repo.findForUpdate(1L,2L)).thenReturn(List.of(compra()));
        service.registrar(venda(5,data.minusHours(1)));
        verify(repo).save(argThat(t->t.getDataOperacao().equals(LocalDateTime.of(2026,9,8,12,34,56))));
    }
    @Test void rejeitaQuantidadeNegativaAntesDeAcessarBanco() {
        assertThrows(RegraNegocioException.class,()->service.registrar(venda(-1,data)));
        verifyNoInteractions(atual,repo,ativos);
    }
    @Test void registraCompraInicial() {
        when(repo.findForUpdate(1L,2L)).thenReturn(List.of());
        assertEquals(TipoTransacao.COMPRA,service.registrar(new TransacaoRequest("PETR4",10L,TipoTransacao.COMPRA,BigDecimal.TEN,BigDecimal.TEN,data)).tipo());
        verify(repo).save(any());
    }
    @Test void mercadoUsaDefineUsdERejeitaAtivoEmReal() {
        Ativo ibm=new Ativo(3L,"IBM","IBM","NYSE","USD");
        when(ativos.findBySimboloIgnoreCase("IBM")).thenReturn(Optional.of(ibm));
        when(repo.findForUpdate(1L,3L)).thenReturn(List.of());
        var compraUsd=new TransacaoRequest("IBM",10L,TipoTransacao.COMPRA,BigDecimal.ONE,BigDecimal.TEN,Mercado.USA);
        assertEquals("USD",service.registrar(compraUsd).moeda());
        var mercadoIncorreto=new TransacaoRequest("PETR4",10L,TipoTransacao.COMPRA,BigDecimal.ONE,BigDecimal.TEN,Mercado.USA);
        assertThrows(RegraNegocioException.class,()->service.registrar(mercadoIncorreto));
    }
    @Test void avaliaPrejuizoComFonteDoOrquestrador() {
        when(atual.obter()).thenReturn(usuario);
        when(repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L)).thenReturn(List.of(compra()));
        when(orq.buscar("PETR4")).thenReturn(new com.curso.cotacao.CotacaoAtual("PETR4","Petrobras","B3",new BigDecimal("8"),com.curso.domains.enums.FonteCotacao.HG_FINANCE));
        var resultado=service.avaliar().get(0);
        assertEquals(new BigDecimal("-20.00"),resultado.lucroPrejuizo());
        assertEquals(new BigDecimal("-20.00"),resultado.lucroPrejuizoPercentual());
        assertEquals(com.curso.domains.enums.FonteCotacao.HG_FINANCE,resultado.fonte());
    }
    @Test void preservaCustoExatoNasPosicoesEAvaliacaoAposVendaParcial() {
        when(atual.obter()).thenReturn(usuario);
        var primeira = new Transacao(1L,usuario,ativo,corretora,TipoTransacao.COMPRA,new BigDecimal("10000"),BigDecimal.ONE,data);
        var segunda = new Transacao(2L,usuario,ativo,corretora,TipoTransacao.COMPRA,new BigDecimal("20000"),new BigDecimal("2"),data.plusMinutes(1));
        when(repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L)).thenReturn(List.of(primeira,segunda));
        assertEquals(new BigDecimal("1.666667"),service.posicoes().get(0).precoMedio());
        assertEquals(new BigDecimal("50000.00"),service.posicoes().get(0).custoTotal());
        assertEquals(new BigDecimal("50000.00"),service.posicoesPorCorretora().get(0).custoTotal());
        when(orq.buscar("PETR4")).thenReturn(new com.curso.cotacao.CotacaoAtual("PETR4","Petrobras","B3",new BigDecimal("2"),com.curso.domains.enums.FonteCotacao.BRAPI));
        assertEquals(new BigDecimal("10000.00"),service.avaliar().get(0).lucroPrejuizo());
        var venda = new Transacao(3L,usuario,ativo,corretora,TipoTransacao.VENDA,new BigDecimal("6000"),new BigDecimal("3"),data.plusMinutes(2));
        when(repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L)).thenReturn(List.of(primeira,segunda,venda));
        assertEquals(new BigDecimal("40000.00"),service.posicoes().get(0).custoTotal());
        assertEquals(new BigDecimal("8000.00"),service.avaliar().get(0).lucroPrejuizo());
        var recompra = new Transacao(4L,usuario,ativo,corretora,TipoTransacao.COMPRA,new BigDecimal("6000"),new BigDecimal("2"),data.plusMinutes(3));
        when(repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L)).thenReturn(List.of(primeira,segunda,venda,recompra));
        assertEquals(new BigDecimal("52000.00"),service.posicoes().get(0).custoTotal());
        assertEquals(new BigDecimal("1.733333"),service.posicoes().get(0).precoMedio());
    }
    @Test void avaliacaoNaoRetornaParcialQuandoCotacaoFalha() {
        when(atual.obter()).thenReturn(usuario);
        when(repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L)).thenReturn(List.of(compra()));
        when(orq.buscar("PETR4")).thenThrow(new com.curso.cotacao.CotacaoIndisponivelException("PETR4",List.of("BRAPI")));
        assertThrows(com.curso.cotacao.CotacaoIndisponivelException.class,service::avaliar);
    }
    @Test void listaSomenteOperacoesDoPrincipal() {
        when(atual.obter()).thenReturn(usuario);
        when(repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L)).thenReturn(List.of(compra()));
        assertEquals(1,service.listar().size());
        verify(repo).findByUsuarioIdOrderByDataOperacaoAscIdAsc(1L);
    }
}

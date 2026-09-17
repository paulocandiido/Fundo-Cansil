package com.curso.services;

import com.curso.cotacao.*;
import com.curso.domains.*;
import com.curso.domains.enums.FonteCotacao;
import com.curso.repositories.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CotacaoServiceTest {
    final CotacaoOrchestrator orq=mock(CotacaoOrchestrator.class);
    final AtivoRepository ativos=mock(AtivoRepository.class);
    final ConsultaRepository consultas=mock(ConsultaRepository.class);
    final UsuarioAtualService atual=mock(UsuarioAtualService.class);
    final CotacaoService service=new CotacaoService(orq,ativos,consultas,atual);

    @Test void persisteFonteSomenteAposSucesso() {
        Usuario u=new Usuario(1L,"Teste","test@test.com","12345678901","hash");
        when(atual.obter()).thenReturn(u);
        when(orq.buscar("PETR4")).thenReturn(new CotacaoAtual("PETR4","Petrobras","B3",BigDecimal.TEN,FonteCotacao.HG_FINANCE));
        when(ativos.findBySimboloIgnoreCase("PETR4")).thenReturn(Optional.of(new Ativo(2L,"PETR4","Petrobras","B3")));
        assertEquals(FonteCotacao.HG_FINANCE,service.buscar("PETR4").fonte());
        var captor=ArgumentCaptor.forClass(Consulta.class);
        verify(consultas).save(captor.capture());
        assertEquals(FonteCotacao.HG_FINANCE,captor.getValue().getFonte());
        assertEquals(u,captor.getValue().getUsuario());
    }
    @Test void falhaTotalNaoGravaAtivoNemHistorico() {
        when(orq.buscar("PETR4")).thenThrow(new CotacaoIndisponivelException("PETR4",List.of("BRAPI")));
        assertThrows(CotacaoIndisponivelException.class,()->service.buscar("PETR4"));
        verifyNoInteractions(ativos,consultas);
    }
}

package com.curso.services;

import com.curso.cadastro.*;
import com.curso.domains.*;
import com.curso.domains.dtos.InvestimentoDTOs.*;
import com.curso.domains.enums.*;
import com.curso.repositories.CorretoraRepository;
import com.curso.repositories.TransacaoRepository;
import com.curso.services.exceptions.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorretoraServiceTest {
    CorretoraRepository repo=mock(CorretoraRepository.class);
    UsuarioAtualService atual=mock(UsuarioAtualService.class);
    BrasilApiCnpjClient client=mock(BrasilApiCnpjClient.class);
    TransacaoRepository transacoes=mock(TransacaoRepository.class);
    CorretoraService service=new CorretoraService(repo,atual,client,transacoes);
    Usuario usuario=new Usuario(7L,"Teste","teste@local.invalid","77777777777","h");
    CnpjResponse empresa=new CnpjResponse("19131243000197","RAZÃO SOCIAL REAL","ATIVA",true,Instant.parse("2026-09-06T12:00:00Z"));
    @BeforeEach void init(){when(atual.obter()).thenReturn(usuario);when(atual.bloquear()).thenReturn(usuario);}
    @Test void cadastraNomeDoServidorEVinculaUsuarioEMercado(){
        when(client.consultar(empresa.cnpj())).thenReturn(empresa);when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        var r=service.cadastrar(new CorretoraRequest("19.131.243/0001-97",Mercado.USA,null));
        assertEquals(empresa.razaoSocial(),r.nome());assertEquals(Mercado.BR,r.mercado());assertEquals(empresa.cnpj(),r.cnpj());assertNotNull(r.verificadoEm());
        verify(repo).saveAndFlush(argThat(c->c.getUsuario()==usuario));
    }
    @Test void duplicidadeDaMesmaInstituicaoAntesDaConsulta(){
        when(repo.findByUsuarioIdAndCnpj(7L,empresa.cnpj())).thenReturn(Optional.of(new Corretora(usuario,empresa.cnpj(),empresa.razaoSocial(),Mercado.BR,true,empresa.consultadoEm())));
        assertThrows(ConflitoException.class,()->service.cadastrar(new CorretoraRequest(empresa.cnpj(),Mercado.USA,null)));
        verifyNoInteractions(client);verify(repo,never()).saveAndFlush(any());
    }
    @Test void listaSomenteCorretorasDaConta(){
        when(repo.findByUsuarioIdOrderByNomeAscIdAsc(7L)).thenReturn(List.of());
        assertTrue(service.listar().isEmpty());verify(repo).findByUsuarioIdOrderByNomeAscIdAsc(7L);
    }
    @Test void rejeitaOutraContaAntesDeConsultar(){
        assertThrows(IllegalArgumentException.class,()->service.cadastrar(new CorretoraRequest(empresa.cnpj(),Mercado.BR,99L)));
        assertThrows(IllegalArgumentException.class,()->service.obterParaOperacao(99L,TipoTransacao.COMPRA));verifyNoInteractions(client);
    }
    @Test void corretoraUSAPodeOperarSemRestricao(){
        when(repo.findByIdAndUsuarioId(8L,7L)).thenReturn(Optional.of(new Corretora(usuario,empresa.cnpj(),empresa.razaoSocial(),Mercado.USA,true,empresa.consultadoEm())));
        assertDoesNotThrow(()->service.obterParaOperacao(8L,TipoTransacao.COMPRA));
        assertDoesNotThrow(()->service.obterParaOperacao(8L,TipoTransacao.VENDA));
    }
    @Test void legadoVendeMasNaoCompraEPreservaBRNaRegularizacao(){
        var legado=new Corretora(8L,"LEGADO","Legado",false,true);
        when(repo.findByIdAndUsuarioId(8L,7L)).thenReturn(Optional.of(legado));
        assertSame(legado,service.obterParaOperacao(8L,TipoTransacao.VENDA));
        assertThrows(IllegalArgumentException.class,()->service.obterParaOperacao(8L,TipoTransacao.COMPRA));
        when(client.consultar(empresa.cnpj())).thenReturn(empresa);when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
        var r=service.cadastrar(new CorretoraRequest(empresa.cnpj(),Mercado.USA,8L));assertEquals(8L,r.id());assertFalse(r.legada());assertEquals(empresa.razaoSocial(),r.nome());
        assertSame(legado,service.obterParaOperacao(8L,TipoTransacao.COMPRA));
    }
    @Test void fonteIndisponivelNaoGrava(){
        when(client.consultar(empresa.cnpj())).thenThrow(new CadastroIndisponivelException());
        assertThrows(CadastroIndisponivelException.class,()->service.cadastrar(new CorretoraRequest(empresa.cnpj(),Mercado.BR,null)));verify(repo,never()).saveAndFlush(any());
    }
    @Test void exclusaoRemoveOperacoesDaContaAntesDaCorretora(){
        var c=new Corretora(8L,"XP","XP",true,false);
        when(repo.findByIdAndUsuarioId(8L,7L)).thenReturn(Optional.of(c));
        service.remover(8L);
        var ordem=inOrder(transacoes,repo);
        ordem.verify(transacoes).deleteByUsuarioIdAndCorretoraId(7L,8L);
        ordem.verify(repo).delete(c);
        ordem.verify(repo).flush();
    }
}

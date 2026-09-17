package com.curso.repositories;

import com.curso.domains.*;
import com.curso.domains.enums.*;
import com.curso.cansil.CansilApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=CansilApplication.class)
@Transactional
class InvestimentoRepositoryTest {
    @Autowired UsuarioRepository usuarios;
    @Autowired AtivoRepository ativos;
    @Autowired TransacaoRepository transacoes;
    @Autowired ConsultaRepository consultas;
    @Autowired CorretoraRepository corretoras;
    Usuario usuario(String email,String cpf) { return usuarios.saveAndFlush(new Usuario(null,"Teste",email,cpf,"hash-falso")); }
    @Test void emailUnicoNoBanco() {
        usuario("unique@teste.com","44444444440");
        assertThrows(DataIntegrityViolationException.class,()->usuario("unique@teste.com","44444444441"));
    }
    @Test void cpfUnicoNoBanco() {
        usuario("cpf1@teste.com","44444444442");
        assertThrows(DataIntegrityViolationException.class,()->usuario("cpf2@teste.com","44444444442"));
    }
    @Test void ordenaHistoricosEIsolaUsuarios() {
        var u=usuario("repo1@teste.com","44444444443");
        var outro=usuario("repo2@teste.com","44444444444");
        var ativo=ativos.saveAndFlush(new Ativo(null,"TEST4","Ativo teste","B3"));
        var corretora=corretoras.findByCodigo("XP").orElseThrow();
        var data=LocalDateTime.of(2026,9,1,10,0);
        transacoes.saveAndFlush(new Transacao(null,u,ativo,corretora,TipoTransacao.COMPRA,BigDecimal.ONE,BigDecimal.TEN,data.plusHours(1)));
        transacoes.saveAndFlush(new Transacao(null,u,ativo,corretora,TipoTransacao.COMPRA,BigDecimal.ONE,BigDecimal.TEN,data));
        var itens=transacoes.findForUpdate(u.getId(),ativo.getId());
        assertEquals(data,itens.get(0).getDataOperacao());
        assertEquals(u.getId(),usuarios.bloquear(u.getId()).orElseThrow().getId());
        assertTrue(transacoes.findByUsuarioIdOrderByDataOperacaoAscIdAsc(outro.getId()).isEmpty());
        consultas.saveAndFlush(new Consulta(null,u,ativo,BigDecimal.TEN,FonteCotacao.BRAPI,data));
        consultas.saveAndFlush(new Consulta(null,u,ativo,BigDecimal.TEN,FonteCotacao.HG_FINANCE,data.plusHours(1)));
        assertEquals(FonteCotacao.HG_FINANCE,consultas.findByUsuarioIdOrderByConsultadoEmDesc(u.getId()).get(0).getFonte());
        assertTrue(consultas.findByUsuarioIdOrderByConsultadoEmDesc(outro.getId()).isEmpty());
    }
    @Test void catalogoReutilizaAtivoECarregaLinhaDeBloqueio() {
        assertEquals(1,ativos.bloquearCatalogo());
        var primeiro=ativos.obterOuCriar("LOCK4","Teste lock","B3");
        ativos.flush();
        var segundo=ativos.obterOuCriar("LOCK4","Teste lock","B3");
        assertEquals(primeiro.getId(),segundo.getId());
    }
    @Test void historicoDesempataMesmoInstantePorIdDecrescente() {
        var u=usuario("ordem@teste.com","44444444445");
        var ativo=ativos.saveAndFlush(new Ativo(null,"ORDER4","Teste ordem","B3"));
        var data=LocalDateTime.of(2026,9,1,10,0);
        consultas.saveAndFlush(new Consulta(null,u,ativo,BigDecimal.TEN,FonteCotacao.BRAPI,data));
        var ultima=consultas.saveAndFlush(new Consulta(null,u,ativo,BigDecimal.TEN,FonteCotacao.HG_FINANCE,data));
        assertEquals(ultima.getId(),consultas.findByUsuarioIdOrderByConsultadoEmDesc(u.getId()).get(0).getId());
    }
}

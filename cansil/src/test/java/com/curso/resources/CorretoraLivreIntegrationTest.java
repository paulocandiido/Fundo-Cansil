package com.curso.resources;

import com.curso.cotacao.*;
import com.curso.cadastro.BrasilApiCnpjClient;
import com.curso.domains.dtos.InvestimentoDTOs.CnpjResponse;
import com.curso.domains.enums.FonteCotacao;
import com.curso.cansil.CansilApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CansilApplication.class) @AutoConfigureMockMvc
class CorretoraLivreIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean CotacaoOrchestrator cotacoes;
    @MockitoBean BrasilApiCnpjClient cadastro;

    @Test void mesmaCorretoraOperaReaisEDolaresEExclusaoApagaOperacoesEPosicoes() throws Exception {
        String a=conta("6"), b=conta("7");
        when(cadastro.consultar("19131243000197")).thenReturn(new CnpjResponse("19131243000197","Corretora real","ATIVA",true,Instant.now()));
        String body="{\"cnpj\":\"19131243000197\"}";
        var criada=mvc.perform(post("/api/corretoras").header("Authorization","Bearer "+a).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.removida").value(false)).andReturn();
        long id=json.readTree(criada.getResponse().getContentAsString()).get("id").asLong();
        when(cotacoes.buscar("IBM")).thenReturn(new CotacaoAtual("IBM","IBM","NYSE",new BigDecimal("110"),FonteCotacao.ALPHA_VANTAGE,"USD"));
        when(cotacoes.buscar("PETR4")).thenReturn(new CotacaoAtual("PETR4","Petrobras","B3",new BigDecimal("25"),FonteCotacao.BRAPI,"BRL"));
        operacao(a,id,"IBM","COMPRA","USD",2,100,201);
        operacao(a,id,"PETR4","COMPRA","BRL",2,20,201);
        mvc.perform(get("/api/carteira").header("Authorization","Bearer "+a)).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/carteira/avaliacao").header("Authorization","Bearer "+a)).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.simbolo == 'IBM')].moeda").value(org.hamcrest.Matchers.contains("USD")))
            .andExpect(jsonPath("$[?(@.simbolo == 'IBM')].lucroPrejuizo").value(org.hamcrest.Matchers.contains(20.0)));
        // O mercado da operação define a moeda, sem depender da corretora.
        operacao(a,id,"IBM","COMPRA","BRL",1,100,422);
        mvc.perform(delete("/api/corretoras/"+id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/corretoras/"+id).header("Authorization","Bearer "+b)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/corretoras/"+id).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/corretoras/"+id).header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/corretoras").header("Authorization","Bearer "+a)).andExpect(content().json("[]"));
        mvc.perform(get("/api/transacoes").header("Authorization","Bearer "+a)).andExpect(content().json("[]"));
        mvc.perform(get("/api/carteira").header("Authorization","Bearer "+a)).andExpect(content().json("[]"));
        mvc.perform(get("/api/carteira/corretoras").header("Authorization","Bearer "+a)).andExpect(content().json("[]"));
        operacao(a,id,"IBM","COMPRA","USD",1,100,400);
        var nova=mvc.perform(post("/api/corretoras").header("Authorization","Bearer "+a).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.removida").value(false)).andReturn();
        long novoId=json.readTree(nova.getResponse().getContentAsString()).get("id").asLong();
        assertNotEquals(id,novoId);
        operacao(a,novoId,"IBM","COMPRA","USD",1,100,201);
        mvc.perform(get("/api/transacoes").header("Authorization","Bearer "+a)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/transacoes").header("Authorization","Bearer "+b)).andExpect(content().json("[]"));
        // Uma resposta incompatível não muda a moeda persistida nem gera avaliação falsa.
        when(cotacoes.buscar("IBM")).thenReturn(new CotacaoAtual("IBM","IBM","NYSE",BigDecimal.TEN,FonteCotacao.ALPHA_VANTAGE,"BRL"));
        mvc.perform(get("/api/cotacoes/IBM").header("Authorization","Bearer "+a)).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/cotacoes/historico").header("Authorization","Bearer "+a)).andExpect(content().json("[]"));
        mvc.perform(get("/api/carteira/avaliacao").header("Authorization","Bearer "+a)).andExpect(status().isUnprocessableEntity());
    }
    private void operacao(String token,long corretora,String simbolo,String tipo,String mercado,int quantidade,int preco,int status) throws Exception {
        String body="{\"simbolo\":\"%s\",\"corretoraId\":%d,\"tipo\":\"%s\",\"mercado\":\"%s\",\"quantidade\":%d,\"valorUnitario\":%d}".formatted(simbolo,corretora,tipo,"USD".equals(mercado)?"USA":"BR",quantidade,preco);
        var resultado=mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(status));
        if(status==201) resultado.andExpect(jsonPath("$.dataOperacao").isNotEmpty());
    }
    private String conta(String prefixo) throws Exception {
        String n=Long.toString(System.nanoTime());String cpf=prefixo+n.substring(n.length()-10),email=cpf+"@livre.invalid";
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Teste livre\",\"email\":\""+email+"\",\"cpf\":\""+cpf+"\",\"senha\":\"segredo123\"}")).andExpect(status().isCreated());
        var r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"senha\":\"segredo123\"}")).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }
}

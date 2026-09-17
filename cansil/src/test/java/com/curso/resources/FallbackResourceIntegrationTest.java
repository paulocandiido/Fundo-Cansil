package com.curso.resources;

import com.curso.cotacao.*;
import com.curso.domains.enums.FonteCotacao;
import com.curso.repositories.ConsultaRepository;
import com.curso.cansil.CansilApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CansilApplication.class)
@AutoConfigureMockMvc
class FallbackResourceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ConsultaRepository consultas;
    @MockitoBean BrapiProvider brapi;
    @MockitoBean AlphaVantageProvider alpha;
    @MockitoBean HgFinanceProvider hg;

    @ParameterizedTest @ValueSource(ints={0,1,2,3})
    void cadeiaRealPersisteSomenteSucesso(int sucesso) throws Exception {
        when(brapi.fonte()).thenReturn(FonteCotacao.BRAPI);
        when(alpha.fonte()).thenReturn(FonteCotacao.ALPHA_VANTAGE);
        when(hg.fonte()).thenReturn(FonteCotacao.HG_FINANCE);
        CotacaoProvider[] fontes={brapi,alpha,hg};
        FonteCotacao[] nomes=FonteCotacao.values();
        for(int i=0;i<3;i++) {
            if(i==sucesso) when(fontes[i].buscar("PETR4")).thenReturn(new CotacaoAtual("PETR4","Petrobras","B3",BigDecimal.TEN,nomes[i]));
            else if(i==0) when(fontes[i].buscar("PETR4")).thenReturn(null);
            else when(fontes[i].buscar("PETR4")).thenThrow(new ProviderCotacaoException(nomes[i],"Indisponível"));
        }
        String email="fallback"+sucesso+"@teste.com";
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Teste\",\"email\":\""+email+"\",\"cpf\":\"3333333333"+sucesso+"\",\"senha\":\"segredo123\"}")).andExpect(status().isCreated());
        var login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"senha\":\"segredo123\"}")).andExpect(status().isOk()).andReturn();
        String auth="Bearer "+json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        long antes=consultas.count();
        var resultado=mvc.perform(get("/api/cotacoes/PETR4").header("Authorization",auth));
        if(sucesso<3) {
            resultado.andExpect(status().isOk()).andExpect(jsonPath("$.fonte").value(nomes[sucesso].name()));
            assertEquals(antes+1,consultas.count());
            mvc.perform(get("/api/cotacoes/historico").header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].fonte").value(nomes[sucesso].name()));
        } else {
            resultado.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message",org.hamcrest.Matchers.containsString("BRAPI"))).andExpect(jsonPath("$.message",org.hamcrest.Matchers.containsString("HG_FINANCE")));
            assertEquals(antes,consultas.count());
            mvc.perform(get("/api/cotacoes/historico").header("Authorization",auth)).andExpect(content().json("[]"));
        }
        var ordem=inOrder(brapi,alpha,hg);
        for(int i=0;i<=Math.min(sucesso,2);i++) ordem.verify(fontes[i]).buscar("PETR4");
        for(int i=sucesso+1;i<3;i++) verify(fontes[i],never()).buscar(anyString());
    }
}

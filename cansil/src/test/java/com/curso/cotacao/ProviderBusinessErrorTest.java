package com.curso.cotacao;

import com.curso.config.CotacaoProperties;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProviderBusinessErrorTest {
    RestClient.Builder builder;
    MockRestServiceServer server;
    CotacaoProperties properties;
    @BeforeEach void preparar() {
        builder=RestClient.builder();
        server=MockRestServiceServer.bindTo(builder).build();
        var fonte=new CotacaoProperties.Fonte("http://quotes.test","segredo-falso");
        properties=new CotacaoProperties(fonte,fonte,fonte);
    }
    @Test void hgRestricaoPlanoNaoViraErroDeDesserializacao() {
        server.expect(anything()).andRespond(withSuccess("{\"valid_key\":true,\"results\":{\"error\":true,\"message\":\"Requer plano Premium segredo-falso\"}}",MediaType.APPLICATION_JSON));
        var ex=assertThrows(ProviderCotacaoException.class,()->new HgFinanceProvider(builder,properties).buscar("PETR4"));
        assertEquals("PLANO",ex.getCategoria());
        assertFalse(ex.getMessage().contains("segredo-falso"));
        assertNull(ex.getCause());
    }
    @Test void hgChaveInvalidaTemCategoriaDeAutenticacao() {
        server.expect(anything()).andRespond(withSuccess("{\"valid_key\":false,\"results\":{\"error\":true,\"message\":\"Invalid key\"}}",MediaType.APPLICATION_JSON));
        assertEquals("AUTENTICACAO",assertThrows(ProviderCotacaoException.class,()->new HgFinanceProvider(builder,properties).buscar("PETR4")).getCategoria());
    }
    @Test void alphaLimiteComHttp200NaoViraSimboloAusente() {
        server.expect(anything()).andRespond(withSuccess("{\"Information\":\"API rate limit segredo-falso\"}",MediaType.APPLICATION_JSON));
        var ex=assertThrows(ProviderCotacaoException.class,()->new AlphaVantageProvider(builder,properties).buscar("PETR4"));
        assertEquals("LIMITE",ex.getCategoria());
        assertFalse(ex.getMessage().contains("segredo-falso"));
    }
    @Test void alphaDistingueAcaoDeFracionarioEValidaLimiteNaCotacao() {
        server.expect(anything()).andRespond(withSuccess("{\"bestMatches\":[{\"1. symbol\":\"PETR4F.SAO\",\"8. currency\":\"BRL\"},{\"1. symbol\":\"PETR4.SAO\",\"8. currency\":\"BRL\"}]}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://quotes.test/query?function=GLOBAL_QUOTE&symbol=PETR4.SAO&apikey=segredo-falso")).andRespond(withSuccess("{\"Note\":\"API call frequency exceeded\"}",MediaType.APPLICATION_JSON));
        assertEquals("LIMITE",assertThrows(ProviderCotacaoException.class,()->new AlphaVantageProvider(builder,properties).buscar("PETR4")).getCategoria());
        server.verify();
    }
}

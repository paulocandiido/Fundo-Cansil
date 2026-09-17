package com.curso.cotacao;

import com.curso.config.CotacaoProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.net.SocketTimeoutException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProvidersFailureTest {
    @ParameterizedTest
    @CsvSource({"BRAPI,401","BRAPI,429","BRAPI,500","BRAPI,0","BRAPI,1","BRAPI,2",
        "ALPHA,401","ALPHA,429","ALPHA,500","ALPHA,0","ALPHA,1","ALPHA,2",
        "HG,401","HG,429","HG,500","HG,0","HG,1","HG,2"})
    void falhasNaoVazamToken(String fonte,int resposta) {
        var builder=RestClient.builder();
        var server=MockRestServiceServer.bindTo(builder).build();
        var f=new CotacaoProperties.Fonte("http://quotes.test","segredo-falso");
        var p=new CotacaoProperties(f,f,f);
        CotacaoProvider provider=switch(fonte) {
            case "BRAPI" -> new BrapiProvider(builder,p);
            case "ALPHA" -> new AlphaVantageProvider(builder,p);
            default -> new HgFinanceProvider(builder,p);
        };
        var expectation=server.expect(anything());
        if(resposta==0) expectation.andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        else if(resposta==1) expectation.andRespond(withException(new SocketTimeoutException("timeout")));
        else if(resposta==2) expectation.andRespond(withSuccess("invalid-json",MediaType.APPLICATION_JSON));
        else expectation.andRespond(withStatus(HttpStatus.valueOf(resposta)).body("segredo-falso"));
        var ex=assertThrows(ProviderCotacaoException.class,()->provider.buscar("PETR4"));
        assertFalse(ex.getMessage().contains("segredo-falso"));
        assertNull(ex.getCause());
        if(resposta==401) assertEquals("AUTENTICACAO",ex.getCategoria());
        if(resposta==429) assertEquals("LIMITE",ex.getCategoria());
        if(resposta==1) assertEquals("TIMEOUT_OU_TRANSPORTE",ex.getCategoria());
        server.verify();
    }
}

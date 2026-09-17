package com.curso.cadastro;

import com.curso.services.exceptions.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrasilApiCnpjClientTest {
    RestClient.Builder builder; MockRestServiceServer server; BrasilApiCnpjClient client;
    @BeforeEach void init() {
        builder=RestClient.builder(); server=MockRestServiceServer.bindTo(builder).build();
        client=new BrasilApiCnpjClient(builder,Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"),ZoneOffset.UTC),"http://cadastro.test");
    }
    @Test void validaFormatosEDigitosVerificadores() {
        assertEquals("19131243000197",Cnpj.normalizar("19.131.243/0001-97"));
        assertEquals("12ABC34501DE35",Cnpj.normalizar("12.abc.345/01de-35"));
        for(String invalido:new String[]{"00000000000000","11111111111111","19131243000198","12ABC34501DE34","../19131243000197","1913124300019","19 131243000197"})
            assertThrows(IllegalArgumentException.class,()->Cnpj.normalizar(invalido));
    }
    @Test void extraiSomenteCadastroMinimoENomeReal() {
        server.expect(requestTo("http://cadastro.test/api/cnpj/v1/19131243000197"))
            .andRespond(withSuccess("{\"cnpj\":\"19131243000197\",\"razao_social\":\" EMPRESA REAL \",\"situacao_cadastral\":2,\"qsa\":[{\"nome_socio\":\"não persistir\"}]}",MediaType.APPLICATION_JSON));
        var r=client.consultar("19.131.243/0001-97");
        assertEquals("EMPRESA REAL",r.razaoSocial());assertTrue(r.ativa());assertEquals(Instant.parse("2026-09-06T12:00:00Z"),r.consultadoEm());server.verify();
    }
    @Test void rejeitaIdentidadeDivergente() {
        server.expect(anything()).andRespond(withSuccess("{\"cnpj\":\"00000000000191\",\"razao_social\":\"Outra\",\"situacao_cadastral\":2}",MediaType.APPLICATION_JSON));
        assertThrows(CadastroIndisponivelException.class,()->client.consultar("19131243000197"));
    }
    @Test void cadastroIncompletoNaoEhAceito() {
        server.expect(anything()).andRespond(withSuccess("{\"cnpj\":\"19131243000197\",\"razao_social\":\"Empresa\"}",MediaType.APPLICATION_JSON));
        assertThrows(CadastroIndisponivelException.class,()->client.consultar("19131243000197"));
    }
    @Test void cnpjInexistenteRetorna404Seguro() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND).body("conteúdo externo"));
        assertEquals("CNPJ não encontrado na fonte cadastral",assertThrows(ObjectNotFoundException.class,()->client.consultar("19131243000197")).getMessage());
    }
    @Test void falhaDeFornecedorNaoExpoeCorpo() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("dado externo confidencial"));
        assertFalse(assertThrows(CadastroIndisponivelException.class,()->client.consultar("19131243000197")).getMessage().contains("confidencial"));
    }
    @Test void cnpjInvalidoNaoConsultaFonte() {assertThrows(IllegalArgumentException.class,()->client.consultar("123"));server.verify();}
}

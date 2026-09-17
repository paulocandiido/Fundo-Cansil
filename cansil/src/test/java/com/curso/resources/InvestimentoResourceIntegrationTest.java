package com.curso.resources;

import com.curso.cotacao.*;import com.curso.domains.enums.FonteCotacao;import com.curso.cansil.CansilApplication;import com.fasterxml.jackson.databind.*;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.test.context.bean.override.mockito.MockitoBean;import org.springframework.test.web.servlet.*;import java.math.BigDecimal;import static org.hamcrest.Matchers.*;import static org.mockito.Mockito.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=CansilApplication.class) @AutoConfigureMockMvc
class InvestimentoResourceIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @MockitoBean CotacaoOrchestrator orquestrador;@MockitoBean BrapiCatalogoClient catalogo;@MockitoBean com.curso.cadastro.BrasilApiCnpjClient cadastro;

 @Test void exigeAutenticacaoEValidaCorretoraECatalogo()throws Exception{
  mvc.perform(get("/api/corretoras")).andExpect(status().isUnauthorized());
  String sufixo=Long.toString(System.nanoTime());sufixo=sufixo.substring(sufixo.length()-11);String token=cadastrarELogar("contratos"+sufixo+"@invest.com",sufixo);
  String sem="{\"mercado\":\"BR\",\"simbolo\":\"PETR4\",\"tipo\":\"COMPRA\",\"quantidade\":1,\"valorUnitario\":20,\"dataOperacao\":\"2026-09-01T10:00:00\"}";
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(sem)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(sem.replace("\"tipo\"","\"corretoraId\":3,\"tipo\""))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(sem.replace("\"tipo\"","\"corretoraId\":99,\"tipo\""))).andExpect(status().isBadRequest());
  when(catalogo.buscar()).thenReturn(java.util.List.of("VALE3","PETR4","PETR4","^BVSP"));
  mvc.perform(get("/api/ativos?busca=pet&pagina=0&tamanho=10").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].simbolo").value("PETR4")).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.desatualizado").value(false));
  mvc.perform(get("/api/ativos?busca=PETR/4").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
 }

 @Test void fluxoCotacaoCompraCarteiraVendaInvalidaEIsolamento() throws Exception {
  String tokenA=cadastrarELogar("a@invest.com","11111111111");String tokenB=cadastrarELogar("b@invest.com","22222222222");
  when(orquestrador.buscar("PETR4")).thenReturn(new CotacaoAtual("PETR4","Petrobras","B3",new BigDecimal("25.00"),FonteCotacao.ALPHA_VANTAGE));
  mvc.perform(get("/api/cotacoes/PETR4").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(jsonPath("$.fonte").value("ALPHA_VANTAGE")).andExpect(jsonPath("$.preco").value(25.0));
  mvc.perform(get("/api/cotacoes/historico").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(jsonPath("$[0].fonte").value("ALPHA_VANTAGE"));
  mvc.perform(get("/api/corretoras").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(content().json("[]"));
  long corretoraId=cadastrarCorretora(tokenA);
  String compra="{\"mercado\":\"BR\",\"simbolo\":\"PETR4\",\"corretoraId\":"+corretoraId+",\"tipo\":\"COMPRA\",\"quantidade\":10,\"valorUnitario\":20,\"dataOperacao\":\"2026-09-01T10:00:00\"}";
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+tokenA).contentType(MediaType.APPLICATION_JSON).content(compra)).andExpect(status().isCreated()).andExpect(jsonPath("$.simbolo").value("PETR4"));
  mvc.perform(get("/api/carteira").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(jsonPath("$[0].precoMedio").value(20.0));
  mvc.perform(get("/api/carteira").header("Authorization","Bearer "+tokenB)).andExpect(status().isOk()).andExpect(content().json("[]"));
  mvc.perform(get("/api/carteira/corretoras").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(jsonPath("$[0].corretora.cnpj").value("19131243000197")).andExpect(jsonPath("$[0].custoTotal").value(200.0));
  String venda="{\"mercado\":\"BR\",\"simbolo\":\"PETR4\",\"corretoraId\":"+corretoraId+",\"tipo\":\"VENDA\",\"quantidade\":11,\"valorUnitario\":25,\"dataOperacao\":\"2026-09-01T11:00:00\"}";
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+tokenA).contentType(MediaType.APPLICATION_JSON).content(venda)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message",containsString("saldo")));
  mvc.perform(get("/api/carteira/avaliacao").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(jsonPath("$[0].lucroPrejuizo").value(50.0)).andExpect(jsonPath("$[0].lucroPrejuizoPercentual").value(25.0));
  mvc.perform(get("/api/transacoes").header("Authorization","Bearer "+tokenB)).andExpect(content().json("[]"));
  mvc.perform(get("/api/cotacoes/historico").header("Authorization","Bearer "+tokenB)).andExpect(content().json("[]"));
  String vendaParcial=venda.replace("\"quantidade\":11","\"quantidade\":5");
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+tokenA).contentType(MediaType.APPLICATION_JSON).content(vendaParcial)).andExpect(status().isCreated());
  mvc.perform(get("/api/carteira").header("Authorization","Bearer "+tokenA)).andExpect(jsonPath("$[0].quantidade").value(5)).andExpect(jsonPath("$[0].precoMedio").value(20.0));
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+tokenA).contentType(MediaType.APPLICATION_JSON).content(vendaParcial)).andExpect(status().isCreated());
  mvc.perform(get("/api/carteira").header("Authorization","Bearer "+tokenA)).andExpect(content().json("[]"));
 }

 @Test void cadastroIsoladoRejeitaDuplicidadeOutroDonoMasNaoMercado()throws Exception{
  String n=Long.toString(System.nanoTime());String sufixo=n.substring(n.length()-10);
  String a=cadastrarELogar("brokerA"+sufixo+"@test.invalid","8"+sufixo),b=cadastrarELogar("brokerB"+sufixo+"@test.invalid","9"+sufixo);
  mvc.perform(get("/api/corretoras/cnpj/19131243000197")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/corretoras").contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\":\"19131243000197\",\"mercado\":\"BR\"}")).andExpect(status().isUnauthorized());
  long idA=cadastrarCorretora(a);
  mvc.perform(post("/api/corretoras").header("Authorization","Bearer "+a).contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\":\"19.131.243/0001-97\",\"mercado\":\"USA\"}")).andExpect(status().isConflict());
  mvc.perform(get("/api/corretoras").header("Authorization","Bearer "+b)).andExpect(content().json("[]"));
  var criada=mvc.perform(post("/api/corretoras").header("Authorization","Bearer "+b).contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\":\"19131243000197\",\"mercado\":\"USA\",\"nome\":\"Nome inventado\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Corretora de teste")).andReturn();
  long idB=json.readTree(criada.getResponse().getContentAsString()).get("id").asLong();
  String operacao="{\"mercado\":\"BR\",\"simbolo\":\"PETR4\",\"corretoraId\":%d,\"tipo\":\"COMPRA\",\"quantidade\":1,\"valorUnitario\":20,\"dataOperacao\":\"2026-09-01T10:00:00\"}";
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+b).contentType(MediaType.APPLICATION_JSON).content(operacao.formatted(idA))).andExpect(status().isBadRequest());
  when(orquestrador.buscar("PETR4")).thenReturn(new CotacaoAtual("PETR4","Petrobras","B3",BigDecimal.TEN,FonteCotacao.BRAPI));
  mvc.perform(post("/api/transacoes").header("Authorization","Bearer "+b).contentType(MediaType.APPLICATION_JSON).content(operacao.formatted(idB))).andExpect(status().isCreated()).andExpect(jsonPath("$.moeda").value("BRL"));
  mvc.perform(get("/api/transacoes").header("Authorization","Bearer "+b)).andExpect(jsonPath("$.length()").value(1));
 }
 private long cadastrarCorretora(String token)throws Exception{
  when(cadastro.consultar("19131243000197")).thenReturn(new com.curso.domains.dtos.InvestimentoDTOs.CnpjResponse("19131243000197","Corretora de teste","ATIVA",true,java.time.Instant.now()));
  var r=mvc.perform(post("/api/corretoras").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\":\"19131243000197\",\"mercado\":\"BR\"}")).andExpect(status().isCreated()).andReturn();
  return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
 }
 private String cadastrarELogar(String email,String cpf)throws Exception{String c="{\"nome\":\"Usuário\",\"email\":\""+email+"\",\"cpf\":\""+cpf+"\",\"senha\":\"segredo123\"}";mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(c)).andExpect(status().isCreated());MvcResult r=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"senha\":\"segredo123\"}")).andExpect(status().isOk()).andReturn();return json.readTree(r.getResponse().getContentAsString()).get("token").asText();}
}

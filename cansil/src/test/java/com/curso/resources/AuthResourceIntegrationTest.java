package com.curso.resources;
import com.curso.repositories.UsuarioRepository;import com.curso.cansil.CansilApplication;import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;import static org.hamcrest.Matchers.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=CansilApplication.class) @AutoConfigureMockMvc class AuthResourceIntegrationTest{@Autowired MockMvc mvc;@Autowired UsuarioRepository repo;
 @Test void cadastroLoginProtecaoESegredos()throws Exception{String cadastro="{\"nome\":\"Ana\",\"email\":\"ana@teste.com\",\"cpf\":\"12345678901\",\"senha\":\"segredo123\"}";mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(cadastro)).andExpect(status().isCreated()).andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.senha").doesNotExist()).andExpect(content().string(not(containsString("segredo123"))));assertNotEquals("segredo123",repo.findByEmailIgnoreCase("ana@teste.com").orElseThrow().getSenhaHash());mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@teste.com\",\"senha\":\"segredo123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());mvc.perform(get("/api/carteira")).andExpect(status().isUnauthorized());}
 @Test void cadastroDuplicadoRetorna409()throws Exception {
  String body="{\"nome\":\"Ana\",\"email\":\"duplicado@teste.com\",\"cpf\":\"55555555555\",\"senha\":\"segredo123\"}";
  mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
  mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict()).andExpect(content().string(not(containsString("55555555555"))));
 }
 @Test void senhaAusenteNaoVazaDadosNaValidacao()throws Exception {
  mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Ana\",\"email\":\"invalido@teste.com\",\"cpf\":\"66666666666\"}"))
    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Dados de entrada inválidos")).andExpect(content().string(not(containsString("66666666666"))));
 }
 @Test void loginIncorretoETokenInvalidoRetornam401()throws Exception {
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ausente@teste.com\",\"senha\":\"incorreta\"}"))
    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciais inválidas"));
  mvc.perform(get("/api/carteira").header("Authorization","Bearer invalido")).andExpect(status().isUnauthorized());
 }
 @Test void jsonMalformadoTemErroSeguro()throws Exception {
  mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"segredo-nao-expor\","))
    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("JSON inválido ou tipo de campo incompatível"))
    .andExpect(content().string(not(containsString("segredo-nao-expor"))));
 }
 private static void assertNotEquals(Object a,Object b){org.junit.jupiter.api.Assertions.assertNotEquals(a,b);}}

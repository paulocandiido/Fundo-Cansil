package com.curso.resources;

import com.curso.cansil.CansilApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = CansilApplication.class)
@AutoConfigureMockMvc
class FrontendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private static final String ORIGIN = "http://localhost:5173";

    @Test void preflightPermiteAuthorizationSemLogin() throws Exception {
        mvc.perform(options("/api/transacoes").header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test void origemDesconhecidaNaoRecebePermissaoCors() throws Exception {
        mvc.perform(options("/api/transacoes").header("Origin", "https://nao-autorizado.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void semTokenRecebeJson401LegivelPeloFront() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Origin", ORIGIN))
            .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
            .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.message").value("Não autenticado"))
            .andExpect(jsonPath("$.path").value("/api/usuarios/me")).andExpect(jsonPath("$.timeStamp").isNumber());
    }

    @Test void tokenInvalidoTambemRecebeJson401() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer invalido"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test void usuarioAtualRetornaApenasDtoDoPrincipal() throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Front Teste","email":"front-integration@example.invalid","cpf":"73737373737","senha":"teste-local-123"}
            """)).andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"front-integration@example.invalid","senha":"teste-local-123"}
            """)).andExpect(status().isOk()).andReturn();
        var token = json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/usuarios/me").param("usuarioId", "99999")
                .header("Origin", ORIGIN).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
            .andExpect(jsonPath("$.nome").value("Front Teste"))
            .andExpect(jsonPath("$.email").value("front-integration@example.invalid"))
            .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.cpf").doesNotExist())
            .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist());
    }
}

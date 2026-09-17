package com.curso.resources;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.domains.enums.Status;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import com.curso.cansil.CansilApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração da camada Resource (controller) para GrupoProduto.
 * Sobe o contexto do Spring Boot e exercita os endpoints reais via MockMvc.
 */
@SpringBootTest(classes = CansilApplication.class)
@AutoConfigureMockMvc
@Transactional
class GrupoProdutoResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GrupoProdutoRepository grupoProdutoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    private GrupoProduto grupoExistente;

    @BeforeEach
    void setUp() {
        produtoRepository.deleteAll();
        grupoProdutoRepository.deleteAll();

        GrupoProduto g = new GrupoProduto();
        g.setDescricao("Informática");
        g.setStatus(Status.ATIVO);
        grupoExistente = grupoProdutoRepository.save(g);
    }

    // ================== GET /{id} ==================

    @Test
    @DisplayName("GET /api/grupoproduto/{id} deve retornar o grupo correspondente")
    void deveBuscarGrupoPorId() throws Exception {
        mockMvc.perform(get("/api/grupoproduto/{id}", grupoExistente.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(grupoExistente.getId()))
                .andExpect(jsonPath("$.descricao").value("Informática"))
                .andExpect(jsonPath("$.status").value(Status.ATIVO.getId()));
    }

    @Test
    @DisplayName("GET /api/grupoproduto/{id} deve retornar 404 quando grupo não existir")
    void deveRetornar404AoBuscarGrupoInexistente() throws Exception {
        //teste de integração
        Integer idInexistente = 99999;
        mockMvc.perform(get("/api/grupoproduto/{id}", idInexistente)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", equalToIgnoringCase(
                        "Grupo de Produto não encontrado: id=" + idInexistente)));
    }

    // ================== POST ==================

    @Test
    @DisplayName("POST /api/grupoproduto deve criar grupo, retornar Location exato e persistir no banco")
    void deveCriarGrupoRetornarLocationEPersistir() throws Exception {
        GrupoProdutoDTO payload = new GrupoProdutoDTO();
        payload.setDescricao("Higiene");
        payload.setStatus(Status.ATIVO.getId());

        String body = objectMapper.writeValueAsString(payload);

        MvcResult result = mockMvc.perform(post("/api/grupoproduto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/grupoproduto/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.descricao").value("Higiene"))
                .andReturn();

        // Lê o corpo e confere Location exato
        GrupoProdutoDTO created = objectMapper.readValue(result.getResponse().getContentAsByteArray(), GrupoProdutoDTO.class);
        Integer createdId = created.getId();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost/api/grupoproduto/" + createdId);

        // Confirma persistência
        Optional<GrupoProduto> salvo = grupoProdutoRepository.findById(createdId);
        assertThat(salvo).isPresent();
        assertThat(salvo.get().getDescricao()).isEqualTo("Higiene");
        assertThat(salvo.get().getStatus()).isEqualTo(Status.ATIVO);

        // Recupera via GET
        mockMvc.perform(get("/api/grupoproduto/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.descricao").value("Higiene"));
    }

    // ================== PUT /{id} ==================

    @Test
    @DisplayName("PUT /api/grupoproduto/{id} deve atualizar grupo existente")
    void deveAtualizarGrupo() throws Exception {
        GrupoProdutoDTO dto = new GrupoProdutoDTO();
        dto.setId(grupoExistente.getId());
        dto.setDescricao("Informática e Acessórios");
        dto.setStatus(Status.ATIVO.getId());

        String body = objectMapper.writeValueAsString(dto);

        mockMvc.perform(put("/api/grupoproduto/{id}", grupoExistente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(grupoExistente.getId()))
                .andExpect(jsonPath("$.descricao").value("Informática e Acessórios"))
                .andExpect(jsonPath("$.status").value(Status.ATIVO.getId()));

        GrupoProduto atualizado = grupoProdutoRepository.findById(grupoExistente.getId()).orElseThrow();
        assertThat(atualizado.getDescricao()).isEqualTo("Informática e Acessórios");
        assertThat(atualizado.getStatus()).isEqualTo(Status.ATIVO);
    }

    @Test
    @DisplayName("PUT /api/grupoproduto/{id} deve retornar 404 quando grupo não existir")
    void deveRetornar404AoAtualizarGrupoInexistente() throws Exception {
        GrupoProdutoDTO dto = new GrupoProdutoDTO();
        dto.setId(12345);
        dto.setDescricao("Novo Nome");
        dto.setStatus(Status.ATIVO.getId());

        mockMvc.perform(put("/api/grupoproduto/{id}", 12345)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Grupo de Produto não encontrado: id=12345"));
    }

    // ================== DELETE /{id} ==================

    @Test
    @DisplayName("DELETE /api/grupoproduto/{id} deve remover o grupo quando existir e não tiver produtos associados")
    void deveExcluirGrupo() throws Exception {
        mockMvc.perform(delete("/api/grupoproduto/{id}", grupoExistente.getId()))
                .andExpect(status().isNoContent());

        assertThat(grupoProdutoRepository.findById(grupoExistente.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/grupoproduto/{id} deve retornar 404 quando grupo não for encontrado")
    void deveRetornar404AoExcluirGrupoInexistente() throws Exception {
        mockMvc.perform(delete("/api/grupoproduto/{id}", 98765))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Grupo de Produto não encontrado: id=98765"));
    }

    @Test
    @DisplayName("DELETE /api/grupoproduto/{id} deve retornar 400 quando houver produtos associados ao grupo")
    void deveRetornar400AoExcluirComProdutosAssociados() throws Exception {
        // Cria um produto associado ao grupo existente
        Produto p = new Produto();
        p.setDescricao("Cabo HDMI");
        p.setCodigoBarra("1234567890123");
        p.setGrupoProduto(grupoExistente);
        p.setStatus(Status.ATIVO);
        p.setSaldoEstoque(new BigDecimal("5.000"));
        p.setValorUnitario(new BigDecimal("39.90"));
        produtoRepository.save(p);

        mockMvc.perform(delete("/api/grupoproduto/{id}", grupoExistente.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Data Integrity Violation"))
                .andExpect(jsonPath("$.message").value(
                        "Grupo de produto possui produtos associados e não pode ser removido: id=" + grupoExistente.getId()
                ));
    }
}

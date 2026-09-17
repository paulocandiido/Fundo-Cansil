package com.curso.resources;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.dtos.ProdutoDTO;
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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
@SpringBootTest(classes = CansilApplication.class)
@AutoConfigureMockMvc
@Transactional
class ProdutoResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private GrupoProdutoRepository grupoProdutoRepository;

    private GrupoProduto grupoProduto;
    private Produto produtoCaboHdmi;
    private Produto produtoNotebook;

    @BeforeEach
    void setUp() {
        produtoRepository.deleteAll();
        grupoProdutoRepository.deleteAll();

        GrupoProduto grupo = new GrupoProduto();
        grupo.setDescricao("Informática");
        grupo.setStatus(Status.ATIVO);
        grupoProduto = grupoProdutoRepository.save(grupo);

        Produto caboHdmi = new Produto();
        caboHdmi.setDescricao("Cabo HDMI");
        caboHdmi.setCodigoBarra("1234567890123");
        caboHdmi.setGrupoProduto(grupoProduto);
        caboHdmi.setStatus(Status.ATIVO);
        caboHdmi.setSaldoEstoque(new BigDecimal("5.000"));
        caboHdmi.setValorUnitario(new BigDecimal("39.90"));
        produtoCaboHdmi = produtoRepository.save(caboHdmi);

        Produto notebook = new Produto();
        notebook.setDescricao("Notebook Gamer");
        notebook.setCodigoBarra("7891234567890");
        notebook.setGrupoProduto(grupoProduto);
        notebook.setStatus(Status.ATIVO);
        notebook.setSaldoEstoque(new BigDecimal("2.000"));
        notebook.setValorUnitario(new BigDecimal("5999.00"));
        produtoNotebook = produtoRepository.save(notebook);
    }

    @Test
    @DisplayName("GET /api/produto deve retornar paginação com produtos cadastrados")
    void deveListarProdutosComPaginacao() throws Exception {
        mockMvc.perform(get("/api/produto")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].idProduto").value(produtoCaboHdmi.getIdProduto()))
                .andExpect(jsonPath("$.content[0].descricao").value("Cabo HDMI"))
                .andExpect(jsonPath("$.content[0].codigoBarra").value("1234567890123"))
                .andExpect(jsonPath("$.content[0].grupoProdutoId").value(grupoProduto.getId()))
                .andExpect(jsonPath("$.content[0].status").value(Status.ATIVO.getId()))
                .andExpect(jsonPath("$.content[0].valorUnitario").value(closeTo(39.90, 0.001)))
                .andExpect(jsonPath("$.content[0].saldoEstoque").value(closeTo(5.0, 0.001)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /api/produto com grupoId deve retornar somente produtos do grupo informado")
    void deveListarProdutosFiltradosPorGrupoComPaginacao() throws Exception {
        GrupoProduto perifericos = new GrupoProduto();
        perifericos.setDescricao("Periféricos");
        perifericos.setStatus(Status.ATIVO);
        perifericos = grupoProdutoRepository.save(perifericos);

        Produto teclado = new Produto();
        teclado.setDescricao("Teclado Mecânico");
        teclado.setCodigoBarra("3216549870123");
        teclado.setGrupoProduto(perifericos);
        teclado.setStatus(Status.ATIVO);
        teclado.setSaldoEstoque(new BigDecimal("4.000"));
        teclado.setValorUnitario(new BigDecimal("299.90"));
        Produto produtoTeclado = produtoRepository.save(teclado);

        mockMvc.perform(get("/api/produto")
                        .param("grupoId", perifericos.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].idProduto").value(produtoTeclado.getIdProduto()))
                .andExpect(jsonPath("$.content[0].descricao").value("Teclado Mecânico"))
                .andExpect(jsonPath("$.content[0].codigoBarra").value("3216549870123"))
                .andExpect(jsonPath("$.content[0].grupoProdutoId").value(perifericos.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/produto/all com grupoId deve retornar lista filtrada pelo grupo")
    void deveListarProdutosFiltradosPorGrupoSemPaginacao() throws Exception {
        GrupoProduto acessorios = new GrupoProduto();
        acessorios.setDescricao("Acessórios");
        acessorios.setStatus(Status.ATIVO);
        acessorios = grupoProdutoRepository.save(acessorios);

        Produto mouse = new Produto();
        mouse.setDescricao("Mouse Sem Fio");
        mouse.setCodigoBarra("6549873210123");
        mouse.setGrupoProduto(acessorios);
        mouse.setStatus(Status.ATIVO);
        mouse.setSaldoEstoque(new BigDecimal("6.000"));
        mouse.setValorUnitario(new BigDecimal("149.90"));
        Produto produtoMouse = produtoRepository.save(mouse);

        mockMvc.perform(get("/api/produto/all")
                        .param("grupoId", acessorios.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idProduto").value(produtoMouse.getIdProduto()))
                .andExpect(jsonPath("$[0].descricao").value("Mouse Sem Fio"))
                .andExpect(jsonPath("$[0].codigoBarra").value("6549873210123"))
                .andExpect(jsonPath("$[0].grupoProdutoId").value(acessorios.getId()));
    }

    @Test
    @DisplayName("GET /api/produto/{id} deve retornar o produto correspondente")
    void deveBuscarProdutoPorId() throws Exception {
        mockMvc.perform(get("/api/produto/{id}", produtoCaboHdmi.getIdProduto())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduto").value(produtoCaboHdmi.getIdProduto()))
                .andExpect(jsonPath("$.descricao").value("Cabo HDMI"))
                .andExpect(jsonPath("$.codigoBarra").value("1234567890123"))
                .andExpect(jsonPath("$.grupoProdutoId").value(grupoProduto.getId()))
                .andExpect(jsonPath("$.status").value(Status.ATIVO.getId()))
                .andExpect(jsonPath("$.valorUnitario").value(closeTo(39.90, 0.001)))
                .andExpect(jsonPath("$.saldoEstoque").value(closeTo(5.0, 0.001)));
    }

    @Test
    @DisplayName("GET /api/produto/codigobarra/{codigo} deve localizar produto pelo código de barras")
    void deveBuscarProdutoPorCodigoDeBarra() throws Exception {
        mockMvc.perform(get("/api/produto/codigobarra/{codigo}", produtoNotebook.getCodigoBarra())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduto").value(produtoNotebook.getIdProduto()))
                .andExpect(jsonPath("$.descricao").value("Notebook Gamer"))
                .andExpect(jsonPath("$.codigoBarra").value("7891234567890"))
                .andExpect(jsonPath("$.grupoProdutoId").value(grupoProduto.getId()))
                .andExpect(jsonPath("$.status").value(Status.ATIVO.getId()))
                .andExpect(jsonPath("$.valorUnitario").value(closeTo(5999.00, 0.001)))
                .andExpect(jsonPath("$.saldoEstoque").value(closeTo(2.0, 0.001)));
    }

    @Test
    @DisplayName("GET /api/produto/codigobarra/{codigo} deve retornar 404 quando produto não for encontrado pelo código de barras")
    void deveRetornar404AoBuscarProdutoPorCodigoDeBarrasInexistente() throws Exception {
        mockMvc.perform(get("/api/produto/codigobarra/{codigo}", "0000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: codigoBarra=0000000000000"));
    }

    @Test
    @DisplayName("POST /api/produto deve criar, retornar Location exato, persistir no banco e permitir consulta por ID")
    void deveCriarProdutoPersistirERetornarDto() throws Exception {
        // Monta o payload (DTO) do produto a ser criado
        ProdutoDTO payload = new ProdutoDTO();
        payload.setDescricao("Água Mineral");
        payload.setCodigoBarra("7891000100001");
        payload.setGrupoProdutoId(grupoProduto.getId());
        payload.setStatus(Status.ATIVO.getId());
        payload.setValorUnitario(new BigDecimal("5.25"));
        payload.setSaldoEstoque(new BigDecimal("10.000"));
        payload.setValorEstoque(new BigDecimal("52.50"));

        // Serializa para JSON
        String body = objectMapper.writeValueAsString(payload);

        // Executa o POST e valida: 201, Location contendo o path, e campos básicos do JSON
        MvcResult result = mockMvc.perform(post("/api/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/produto/")))
                .andExpect(jsonPath("$.idProduto").isNumber())
                .andExpect(jsonPath("$.descricao").value("Água Mineral"))
                .andReturn();

        // Lê o corpo da resposta como DTO para obter o id gerado
        ProdutoDTO created = objectMapper.readValue(result.getResponse().getContentAsByteArray(), ProdutoDTO.class);
        Long createdId = created.getIdProduto();

        // Verifica o Location EXATO após conhecer o id
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost/api/produto/" + createdId);

        // Garante que persistiu no repositório e com os dados esperados
        Optional<Produto> saved = produtoRepository.findById(createdId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getDescricao()).isEqualTo("Água Mineral");
        assertThat(saved.get().getGrupoProduto().getId()).isEqualTo(grupoProduto.getId());

        // Consulta via GET para confirmar recuperação do recurso criado
        mockMvc.perform(get("/api/produto/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduto").value(createdId))
                .andExpect(jsonPath("$.descricao").value("Água Mineral"));
    }


    @Test
    @DisplayName("POST /api/produto deve retornar 404 quando grupo informado não existir")
    void deveRetornar404AoCriarProdutoComGrupoInexistente() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("descricao", "Monitor 4K");
        payload.put("codigoBarra", "5554443332221");
        payload.put("grupoProdutoId", 9999);
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 1800.00);
        payload.put("saldoEstoque", 7.0);

        mockMvc.perform(post("/api/produto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Grupo de Produto não encontrado: id=9999"));
    }

    @Test
    @DisplayName("PUT /api/produto/{id} deve atualizar o produto existente")
    void deveAtualizarProduto() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("idProduto", produtoCaboHdmi.getIdProduto());
        payload.put("descricao", "Cabo HDMI 4K");
        payload.put("codigoBarra", produtoCaboHdmi.getCodigoBarra());
        payload.put("grupoProdutoId", grupoProduto.getId());
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 49.90);
        payload.put("saldoEstoque", 10.0);

        mockMvc.perform(put("/api/produto/{id}", produtoCaboHdmi.getIdProduto())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Cabo HDMI 4K"))
                .andExpect(jsonPath("$.valorUnitario").value(closeTo(49.90, 0.001)))
                .andExpect(jsonPath("$.saldoEstoque").value(closeTo(10.0, 0.001)));

        Produto updated = produtoRepository.findById(produtoCaboHdmi.getIdProduto()).orElseThrow();
        assertThat(updated.getDescricao()).isEqualTo("Cabo HDMI 4K");
        assertThat(updated.getValorUnitario()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(updated.getSaldoEstoque()).isEqualByComparingTo(new BigDecimal("10.000"));
    }

    @Test
    @DisplayName("PUT /api/produto/{id} deve retornar 404 quando produto não existir")
    void deveRetornar404AoAtualizarProdutoInexistente() throws Exception {
        var payload = objectMapper.createObjectNode();
        payload.put("idProduto", 999L);
        payload.put("descricao", "Produto Inexistente");
        payload.put("codigoBarra", "0000000000000");
        payload.put("grupoProdutoId", grupoProduto.getId());
        payload.put("status", Status.ATIVO.getId());
        payload.put("valorUnitario", 10.00);
        payload.put("saldoEstoque", 1.0);

        mockMvc.perform(put("/api/produto/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=999"));
    }

    @Test
    @DisplayName("DELETE /api/produto/{id} deve remover o produto quando existir")
    void deveExcluirProduto() throws Exception {
        mockMvc.perform(delete("/api/produto/{id}", produtoNotebook.getIdProduto()))
                .andExpect(status().isNoContent());

        assertThat(produtoRepository.findById(produtoNotebook.getIdProduto())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/produto/{id} deve retornar 404 quando produto não for encontrado")
    void deveRetornar404AoExcluirProdutoInexistente() throws Exception {
        mockMvc.perform(delete("/api/produto/{id}", 12345L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=12345"));
    }


}

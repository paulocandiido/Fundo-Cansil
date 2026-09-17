package com.curso.domains;

import com.curso.domains.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrupoProdutoDomainUnitTest {

    @Test
    @DisplayName("Construtor padrão deve iniciar com status ATIVO e lista de produtos vazia")
    void construtorPadraoDeveIniciarAtivoEListaVazia() {
        GrupoProduto grupo = new GrupoProduto();

        assertEquals(Status.ATIVO, grupo.getStatus());
        assertTrue(grupo.getProdutos().isEmpty());
    }

    @Test
    @DisplayName("addProduto deve manter relação bidirecional (grupo contém produto e produto referencia o grupo)")
    void addProdutoDeveManterRelacaoBidirecional() {
        GrupoProduto grupo = new GrupoProduto();
        Produto produto = new Produto();
        produto.setCodigoBarra("1234567890123");
        produto.setDescricao("Produto Teste");

        grupo.addProduto(produto);

        assertTrue(grupo.getProdutos().contains(produto));
        assertSame(grupo, produto.getGrupoProduto());
    }

    @Test
    @DisplayName("removeProduto deve desfazer a relação quando o produto pertence ao grupo")
    void removeProdutoDeveDesfazerRelacaoQuandoPertenceAoGrupo() {
        GrupoProduto grupo = new GrupoProduto();
        Produto produto = new Produto();
        produto.setCodigoBarra("1234567890123");
        produto.setDescricao("Produto Teste");
        grupo.addProduto(produto);

        grupo.removeProduto(produto);

        assertTrue(grupo.getProdutos().isEmpty());
        assertNull(produto.getGrupoProduto());
    }

    @Test
    @DisplayName("equals/hashCode devem considerar id e descrição (objetos iguais/diferentes)")
    void equalsEHashCodeDevemUsarIdEDescricao() {
        GrupoProduto grupo1 = new GrupoProduto();
        grupo1.setId(1);
        grupo1.setDescricao("Grupo");

        GrupoProduto grupo2 = new GrupoProduto();
        grupo2.setId(1);
        grupo2.setDescricao("Grupo");

        GrupoProduto grupo3 = new GrupoProduto();
        grupo3.setId(2);
        grupo3.setDescricao("Outro Grupo");

        assertEquals(grupo1, grupo2);
        assertEquals(grupo1.hashCode(), grupo2.hashCode());
        assertNotEquals(grupo1, grupo3);
        assertNotEquals(grupo1.hashCode(), grupo3.hashCode());
    }


    @Test
    @DisplayName("removeProduto não deve alterar nada quando o produto não pertence ao grupo")
    void removeProdutoNaoDeveAlterarQuandoProdutoNaoPertenceAoGrupo() {
        GrupoProduto grupo = new GrupoProduto();
        GrupoProduto outroGrupo = new GrupoProduto();
        Produto produto = new Produto();

        outroGrupo.addProduto(produto); // pertence a outro grupo

        grupo.removeProduto(produto); // tentar remover de 'grupo'

        assertAll(
                () -> assertTrue(grupo.getProdutos().isEmpty()),
                () -> assertSame(outroGrupo, produto.getGrupoProduto())
        );
    }

    @Test
    @DisplayName("equals/hashCode devem cumprir reflexividade, simetria, transitividade e consistência (contrato básico)")
    void contratoBasicoDeEqualsEHashCode() {
        GrupoProduto a = new GrupoProduto(); a.setId(1); a.setDescricao("Grupo");
        GrupoProduto b = new GrupoProduto(); b.setId(1); b.setDescricao("Grupo");
        GrupoProduto c = new GrupoProduto(); c.setId(1); c.setDescricao("Grupo");
        GrupoProduto d = new GrupoProduto(); d.setId(2); d.setDescricao("Outro");

        assertAll(
                () -> assertTrue(a.equals(a)),                    // reflexivo
                () -> assertTrue(a.equals(b) && b.equals(a)),     // simétrico
                () -> assertTrue(a.equals(b) && b.equals(c) && a.equals(c)), // transitivo
                () -> assertFalse(a.equals(null)),                // com null
                () -> assertFalse(a.equals("string")),            // classe diferente
                () -> assertEquals(a.hashCode(), b.hashCode()),
                () -> assertNotEquals(a, d)
        );
    }

    @Test
    @DisplayName("equals/hashCode com id nulo: deve usar descrição conforme regra pré-persistência")
    void equalsComIdNuloDeveUsarDescricao() {
        GrupoProduto x = new GrupoProduto(); x.setDescricao("X");
        GrupoProduto y = new GrupoProduto(); y.setDescricao("X");
        GrupoProduto z = new GrupoProduto(); z.setDescricao("Z");

        // Ajuste conforme sua regra de negócio para pré-persistência:
        assertEquals(x, y);
        assertEquals(x.hashCode(), y.hashCode());
        assertNotEquals(x, z);
    }

    @Test
    @DisplayName("equals/hashCode: comportamento estável quando descricao é nula")
    void equalsHashCodeComDescricaoNula() {
        GrupoProduto a = new GrupoProduto(); a.setId(1); a.setDescricao(null);
        GrupoProduto b = new GrupoProduto(); b.setId(1); b.setDescricao(null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("removeProduto: não deve alterar a referência do produto se ele não pertence a este grupo")
    void removeProdutoNaoDeveZerarGrupoDeOutro() {
        GrupoProduto g1 = new GrupoProduto();
        GrupoProduto g2 = new GrupoProduto();
        Produto p = new Produto();
        g2.addProduto(p); // pertence ao g2

        g1.removeProduto(p); // tentar remover de g1

        assertAll(
                () -> assertSame(g2, p.getGrupoProduto()),
                () -> assertFalse(g1.getProdutos().contains(p)),
                () -> assertTrue(g2.getProdutos().contains(p))
        );
    }
}

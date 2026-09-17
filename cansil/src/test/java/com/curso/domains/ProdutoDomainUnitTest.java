package com.curso.domains;

import com.curso.domains.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoDomainUnitTest {

    // ---- utilitário para acionar o cálculo do JPA (@PrePersist/@PreUpdate) sem persistir ----
    private static void invocarRecalcValorEstoque(Produto p) throws Exception {
        Method m = Produto.class.getDeclaredMethod("recalcValorEstoque");
        m.setAccessible(true);
        m.invoke(p);
    }

    @Test
    @DisplayName("Deve permitir configurar propriedades básicas (getters/setters) do Produto")
    void deveConfigurarPropriedadesBasicas() {
        Produto p = new Produto();

        Long id = 100L;
        String descricao = "Produto Teste";
        String codigoBarra = "7891234567890";
        Status status = Status.ATIVO;
        BigDecimal saldo = new BigDecimal("3.000");
        BigDecimal valorUnit = new BigDecimal("19.90");

        p.setIdProduto(id);
        p.setDescricao(descricao);
        p.setCodigoBarra(codigoBarra);
        p.setStatus(status);
        p.setSaldoEstoque(saldo);
        p.setValorUnitario(valorUnit);

        assertAll(
                () -> assertEquals(id, p.getIdProduto()),
                () -> assertEquals(descricao, p.getDescricao()),
                () -> assertEquals(codigoBarra, p.getCodigoBarra()),
                () -> assertEquals(status, p.getStatus()),
                () -> assertEquals(saldo, p.getSaldoEstoque()),
                () -> assertEquals(valorUnit, p.getValorUnitario())
                // valorEstoque é calculado no ciclo JPA; validado em testes específicos abaixo
        );
    }

    @Test
    @DisplayName("Deve recalcular valorEstoque via @PrePersist/@PreUpdate (saldo * valorUnitario, escala 2, HALF_UP)")
    void deveRecalcularValorEstoque() throws Exception {
        Produto p = new Produto();
        p.setSaldoEstoque(new BigDecimal("3.000"));
        p.setValorUnitario(new BigDecimal("19.90"));

        invocarRecalcValorEstoque(p);

        assertEquals(new BigDecimal("59.70"), p.getValorEstoque());
    }

    @Test
    @DisplayName("Recalcular valorEstoque deve usar ZERO quando saldo ou valorUnitário forem nulos")
    void deveRecalcularComZerosQuandoNulos() throws Exception {
        // saldo nulo
        Produto a = new Produto();
        a.setSaldoEstoque(null);
        a.setValorUnitario(new BigDecimal("10.00"));
        invocarRecalcValorEstoque(a);
        assertEquals(new BigDecimal("0.00"), a.getValorEstoque());

        // valor unitário nulo
        Produto b = new Produto();
        b.setSaldoEstoque(new BigDecimal("5.000"));
        b.setValorUnitario(null);
        invocarRecalcValorEstoque(b);
        assertEquals(new BigDecimal("0.00"), b.getValorEstoque());

        // ambos nulos
        Produto c = new Produto();
        invocarRecalcValorEstoque(c);
        assertEquals(new BigDecimal("0.00"), c.getValorEstoque());
    }

    @Test
    @DisplayName("Recalcular valorEstoque deve arredondar HALF_UP para 2 casas decimais")
    void deveArredondarHalfUpParaDuasCasas() throws Exception {
        Produto p = new Produto();
        p.setSaldoEstoque(new BigDecimal("1.005"));   // 1.005
        p.setValorUnitario(new BigDecimal("1.005"));  // 1.005
        // multiplicação exata: 1.010025 → HALF_UP(2) = 1.01
        invocarRecalcValorEstoque(p);
        assertEquals(new BigDecimal("1.01"), p.getValorEstoque());

        // outro exemplo: 2.335 * 3.333 = 7.774... → 7.78 (HALF_UP em 2 casas)
        Produto q = new Produto();
        q.setSaldoEstoque(new BigDecimal("2.335"));
        q.setValorUnitario(new BigDecimal("3.333"));
        invocarRecalcValorEstoque(q);
        assertEquals(new BigDecimal("7.78"), q.getValorEstoque());
    }

    @Test
    @DisplayName("Produto deve refletir associação quando incluído/retirado de um GrupoProduto (consistência bidirecional)")
    void deveRefletirAssociacaoComGrupoProduto() {
        Produto p = new Produto();
        p.setIdProduto(10L);
        p.setDescricao("Cabo HDMI");
        p.setCodigoBarra("1234567890123");
        p.setStatus(Status.ATIVO);

        GrupoProduto g = new GrupoProduto();
        g.setId(1);
        g.setDescricao("Informática");
        g.setStatus(Status.ATIVO);

        // associa pelo agregado correto
        g.addProduto(p);

        assertAll(
                () -> assertSame(g, p.getGrupoProduto()),
                () -> assertTrue(g.getProdutos().contains(p))
        );

        // desfaz associação
        g.removeProduto(p);

        assertAll(
                () -> assertNull(p.getGrupoProduto()),
                () -> assertFalse(g.getProdutos().contains(p))
        );
    }

    @Test
    @DisplayName("equals/hashCode básicos: reflexivo, com null e tipo diferente, e objetos com IDs diferentes não são iguais")
    void contratoBasicoDeEqualsEHashCode() {
        Produto a = new Produto();
        a.setIdProduto(1L);
        a.setCodigoBarra("ABC");
        a.setDescricao("Produto A");

        Produto b = new Produto();
        b.setIdProduto(2L);
        b.setCodigoBarra("DEF");
        b.setDescricao("Produto B");

        assertAll(
                () -> assertTrue(a.equals(a)),          // reflexivo
                () -> assertFalse(a.equals(null)),      // null
                () -> assertFalse(a.equals("string")),  // tipo diferente
                () -> assertNotEquals(a, b)             // ids diferentes → não iguais (ajuste se sua regra for outra)
        );
    }

    @Test
    @DisplayName("Com id nulo, equals/hashCode devem seguir a regra definida para pré-persistência (ajuste conforme implementação)")
    void equalsComIdNuloDeveSeguirRegraPrePersistencia() {
        // Se a sua implementação considera outros campos quando id é nulo, ajuste abaixo.
        // Aqui assumimos que dois produtos sem id e com mesmo código/descrição são iguais.
        Produto x = new Produto();
        x.setIdProduto(null);
        x.setCodigoBarra("ZZZ");
        x.setDescricao("X");

        Produto y = new Produto();
        y.setIdProduto(null);
        y.setCodigoBarra("ZZZ");
        y.setDescricao("X");

        // Se na sua implementação objetos sem id NUNCA são iguais, troque para assertNotEquals(x, y)
        assertEquals(x, y);
        assertEquals(x.hashCode(), y.hashCode());

        Produto z = new Produto();
        z.setIdProduto(null);
        z.setCodigoBarra("YYY");
        z.setDescricao("Z");

        assertNotEquals(x, z);
    }
}

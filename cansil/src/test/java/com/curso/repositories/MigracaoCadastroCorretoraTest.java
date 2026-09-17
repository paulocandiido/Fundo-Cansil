package com.curso.repositories;

import java.sql.*;
import liquibase.*;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MigracaoCadastroCorretoraTest {
    private final String url=System.getProperty("cnpj.test.jdbc-url","jdbc:h2:mem:migracao_cnpj;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(url,System.getProperty("cnpj.test.user","sa"),System.getProperty("cnpj.test.password",""));
    }
    private void aplicar(String file) throws Exception {
        Database db=DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conectar()));
        try(Liquibase l=new Liquibase("db/changelog/changes/"+file,new ClassLoaderResourceAccessor(),db)){l.update(new Contexts(),new LabelExpression());}
    }
    @Test void isolaLegadoPorUsuarioEPreservaOperacoes() throws Exception {
        aplicar("001-criar-estrutura-inicial.xml");aplicar("002-criar-estrutura-investimentos.xml");aplicar("003-serializar-criacao-ativos.xml");aplicar("004-adicionar-corretoras.xml");
        try(Connection c=conectar();Statement s=c.createStatement()){
            s.executeUpdate("insert into usuario(id,nome,email,cpf,senha_hash) values(71,'Um','um@teste.invalid','77777777771','h'),(72,'Dois','dois@teste.invalid','77777777772','h')");
            s.executeUpdate("insert into ativo(id,simbolo,nome,bolsa) values(81,'PETR4','Petrobras','B3')");
            s.executeUpdate("insert into transacao(id,usuario_id,ativo_id,corretora_id,tipo,quantidade,valor_unitario,data_operacao) values(91,71,81,1,'COMPRA',10,20,TIMESTAMP '2026-09-01 10:00:00'),(92,72,81,1,'COMPRA',15,30,TIMESTAMP '2026-09-02 11:00:00'),(93,71,81,3,'COMPRA',2,25,TIMESTAMP '2026-09-03 12:00:00')");
        }
        aplicar("005-cadastro-corretoras-cnpj.xml");
        try(Connection c=conectar();Statement s=c.createStatement()){
            try(ResultSet r=s.executeQuery("select t.id,t.quantidade,t.valor_unitario,t.data_operacao,c.usuario_id,c.cnpj,c.mercado,c.legada,c.ativa,c.id as corretora from transacao t join corretora c on t.corretora_id=c.id order by t.id")){
                assertTrue(r.next());assertEquals(91,r.getLong("id"));assertEquals(71,r.getLong("usuario_id"));assertEquals(0,new java.math.BigDecimal("10").compareTo(r.getBigDecimal("quantidade")));assertEquals(0,new java.math.BigDecimal("20").compareTo(r.getBigDecimal("valor_unitario")));assertEquals(Timestamp.valueOf("2026-09-01 10:00:00"),r.getTimestamp("data_operacao"));assertNull(r.getString("cnpj"));assertTrue(r.getBoolean("legada"));assertFalse(r.getBoolean("ativa"));assertEquals("BR",r.getString("mercado"));long corretora=r.getLong("corretora");
                assertTrue(r.next());assertEquals(72,r.getLong("usuario_id"));assertNotEquals(corretora,r.getLong("corretora"));
                assertTrue(r.next());assertEquals(93,r.getLong("id"));assertFalse(r.next());
            }
            s.executeUpdate("insert into corretora(id,codigo,nome,ativa,legada,usuario_id,cnpj,mercado) values(nextval('seq_corretora'),'test1','Um',true,false,71,'19131243000197','BR')");
            assertThrows(SQLException.class,()->s.executeUpdate("insert into corretora(id,codigo,nome,ativa,legada,usuario_id,cnpj,mercado) values(nextval('seq_corretora'),'test2','Um',true,false,71,'19131243000197','USA')"));
            s.executeUpdate("insert into corretora(id,codigo,nome,ativa,legada,usuario_id,cnpj,mercado) values(nextval('seq_corretora'),'test3','Dois',true,false,72,'19131243000197','USA')");
        }
    }
}

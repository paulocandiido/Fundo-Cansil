# Java Spring Boot — Aula 11 — Versionamento do banco de dados com Liquibase

## 11.1 Objetivos da aula

Ao final desta aula o estudante deverá ser capaz de:

- explicar por que o banco precisa ser versionado;
- diferenciar Hibernate/JPA de uma ferramenta de migration;
- entender changelog, changeSet, checksum e lock;
- adicionar Liquibase ao Spring Boot;
- portar o schema existente para um changelog inicial;
- iniciar bancos H2 e PostgreSQL com a mesma estrutura;
- criar uma nova evolução sem alterar changeSets executados.

## 11.2 O problema do `ddl-auto=create`

Antes desta aula, o perfil de desenvolvimento continha:

```properties
spring.jpa.hibernate.ddl-auto=create
```

Nesse modo, o Hibernate remove e recria as tabelas quando a aplicação inicia. Isso ajuda nas primeiras aulas, mas passa a causar problemas:

- os dados são perdidos;
- não existe um histórico explícito de alterações;
- dois estudantes podem ficar com estruturas diferentes;
- atualizar um servidor se torna arriscado;
- não há uma ordem documentada para criar tabelas, colunas e constraints.

## 11.3 O que é uma migration

Uma migration é uma alteração versionada na estrutura ou nos dados controlados do banco.

Exemplos:

- criar uma tabela;
- adicionar uma coluna;
- criar uma chave estrangeira;
- adicionar um índice;
- ajustar dados necessários para uma nova regra.

O Liquibase lê uma lista ordenada de changeSets. Antes de executar um changeSet, ele consulta o próprio histórico no banco. Se aquele changeSet já estiver registrado, ele não é executado novamente.

## 11.4 Liquibase e Hibernate possuem papéis diferentes

Neste projeto:

| Tecnologia | Responsabilidade |
|---|---|
| JPA | Mapear objetos Java para tabelas |
| Hibernate | Implementar o mapeamento e executar operações SQL |
| Liquibase | Criar e evoluir a estrutura do banco de forma versionada |

Usaremos apenas o Liquibase para criar o schema. O Hibernate ficará em modo `validate`, verificando se as entidades são compatíveis com as tabelas.

## 11.5 Adicionando a dependência

No `pom.xml`, logo após o starter JPA, foi adicionada a dependência:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Não foi informada uma versão. O `spring-boot-starter-parent` já seleciona uma versão compatível com a versão do Spring Boot utilizada pelo projeto.

## 11.6 Estrutura dos arquivos

Foi criada esta estrutura:

```text
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.xml
        └── changes/
            └── 001-criar-estrutura-inicial.xml
```

O arquivo master funciona como índice. Cada nova aula ou alteração do banco cria outro arquivo na pasta `changes` e o inclui no final do master.

## 11.7 Changelog master

Arquivo `src/main/resources/db/changelog/db.changelog-master.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.31.xsd">

    <include file="changes/001-criar-estrutura-inicial.xml"
             relativeToChangelogFile="true"/>

</databaseChangeLog>
```

O atributo `relativeToChangelogFile="true"` informa que o caminho do arquivo incluído é relativo à localização do master.

## 11.8 Portando o schema já construído

O schema inicial não foi inventado novamente. Ele foi derivado das entidades atuais:

- `GrupoProduto` usa a tabela `grupoproduto` e a sequência `seq_grupoproduto`;
- `Produto` usa a tabela `produto` e a sequência `seq_produto`;
- o status é persistido como número inteiro;
- `codigobarra` é único;
- `produto.idgrupoproduto` referencia `grupoproduto.id`;
- os nomes em snake case seguem a estratégia utilizada pelo Hibernate.

Arquivo completo `001-criar-estrutura-inicial.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.31.xsd">

    <changeSet id="001-criar-sequencias" author="curso">
        <createSequence sequenceName="seq_grupoproduto" startValue="1" incrementBy="1"/>
        <createSequence sequenceName="seq_produto" startValue="1" incrementBy="1"/>
    </changeSet>

    <changeSet id="002-criar-tabela-grupoproduto" author="curso">
        <createTable tableName="grupoproduto">
            <column name="id" type="INTEGER">
                <constraints primaryKey="true"
                             primaryKeyName="pk_grupoproduto"
                             nullable="false"/>
            </column>
            <column name="descricao" type="VARCHAR(120)">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INTEGER">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

    <changeSet id="003-criar-tabela-produto" author="curso">
        <createTable tableName="produto">
            <column name="id_produto" type="BIGINT">
                <constraints primaryKey="true"
                             primaryKeyName="pk_produto"
                             nullable="false"/>
            </column>
            <column name="codigobarra" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="descricao" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="saldo_estoque" type="DECIMAL(18,3)">
                <constraints nullable="false"/>
            </column>
            <column name="valor_unitario" type="DECIMAL(18,3)">
                <constraints nullable="false"/>
            </column>
            <column name="valor_estoque" type="DECIMAL(18,2)">
                <constraints nullable="false"/>
            </column>
            <column name="data_cadastro" type="DATE">
                <constraints nullable="false"/>
            </column>
            <column name="idgrupoproduto" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="INTEGER">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addUniqueConstraint
                tableName="produto"
                columnNames="codigobarra"
                constraintName="uk_produto_codigobarra"/>

        <addForeignKeyConstraint
                baseTableName="produto"
                baseColumnNames="idgrupoproduto"
                referencedTableName="grupoproduto"
                referencedColumnNames="id"
                constraintName="fk_produto_grupoproduto"/>
    </changeSet>

</databaseChangeLog>
```

Cada changeSet possui uma identificação formada por `id`, `author` e caminho do arquivo. Essa combinação deve ser única no projeto.

## 11.9 Configurando o Spring Boot

O `application.properties` passou a conter:

```properties
# O Liquibase passa a ser o responsavel pela criacao e evolucao do banco.
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml

# O Hibernate apenas confere se as entidades correspondem ao banco.
spring.jpa.hibernate.ddl-auto=validate
```

O `ddl-auto=create` foi removido do perfil de desenvolvimento.

Ordem de inicialização simplificada:

```text
Aplicação inicia
      ↓
DataSource conecta ao banco
      ↓
Liquibase lê DATABASECHANGELOG
      ↓
Liquibase executa changeSets pendentes
      ↓
Hibernate valida as entidades
      ↓
Aplicação fica disponível
```

## 11.10 Tabelas de controle do Liquibase

Na primeira execução, o Liquibase cria automaticamente:

### `DATABASECHANGELOG`

Registra os changeSets executados, sua ordem e checksum. Essa tabela responde à pergunta: “quais versões já foram aplicadas neste banco?”.

### `DATABASECHANGELOGLOCK`

Impede que duas instâncias alterem simultaneamente a estrutura. O lock é liberado ao final da execução.

Essas tabelas não representam entidades do sistema e não devem ser removidas manualmente.

## 11.11 Por que não alterar um changeSet executado

O Liquibase calcula um checksum do conteúdo. Se um changeSet já executado for alterado, o conteúdo deixa de corresponder ao histórico registrado.

Regra do curso:

> Depois que um changeSet foi enviado ao repositório e executado por outras pessoas, ele é imutável. Toda correção deve ser feita em um novo changeSet.

Exemplo: para adicionar a coluna `unidade_medida`, crie `002-adicionar-unidade-medida.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.31.xsd">

    <changeSet id="004-adicionar-unidade-medida" author="nome-do-aluno">
        <addColumn tableName="produto">
            <column name="unidade_medida" type="VARCHAR(10)" defaultValue="UN">
                <constraints nullable="false"/>
            </column>
        </addColumn>

        <rollback>
            <dropColumn tableName="produto" columnName="unidade_medida"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

Depois inclua o arquivo no final do master:

```xml
<include file="changes/001-criar-estrutura-inicial.xml" relativeToChangelogFile="true"/>
<include file="changes/002-adicionar-unidade-medida.xml" relativeToChangelogFile="true"/>
```

Esse exemplo é apenas exercício e não foi aplicado ao projeto nesta aula.

## 11.12 Ajustando a carga de dados

Quando `ddl-auto=create` era usado, o banco sempre começava vazio. Agora os dados permanecem entre as execuções.

Por isso, o método `DBService.initDB()` recebeu uma proteção simples:

```java
if (grupoProdutoRepo.count() > 0) {
    return;
}
```

Sem essa verificação, a aplicação tentaria cadastrar novamente produtos com os mesmos códigos de barras na segunda inicialização.

Os exemplos continuam no código para preservar a abordagem didática adotada nas aulas de 2025. O Liquibase será responsável pela estrutura; o `DBService`, somente pelos dados de demonstração.

## 11.13 Primeiro uso em um banco vazio

Para um banco novo:

1. Crie o banco `cursodb` no PostgreSQL.
2. Configure as variáveis da Aula 10.
3. Execute a aplicação com o perfil `dev`.
4. Observe no log a execução de três changeSets.
5. Consulte as tabelas:

```sql
select id, author, filename, dateexecuted
from databasechangelog
order by orderexecuted;
```

6. Reinicie a aplicação.
7. Confirme que os changeSets não são executados novamente e que os dados permanecem.

## 11.14 Portando um banco PostgreSQL que já possui dados

Se as tabelas foram criadas anteriormente pelo Hibernate, iniciar diretamente com o changeSet inicial causará conflito: o Liquibase tentará criar objetos que já existem.

Há duas estratégias.

### Estratégia A — recriar o banco didático

Use esta opção quando os dados podem ser descartados.

1. Faça backup se houver qualquer dado importante.
2. Remova e recrie apenas o banco `cursodb`.
3. Inicie a aplicação.
4. O Liquibase criará toda a estrutura e seu histórico.

Essa é a opção recomendada para os bancos locais da turma.

### Estratégia B — criar uma linha de base sem apagar os dados

Use esta opção somente quando o banco existente precisa ser preservado.

1. Faça um backup testado.
2. Compare tabelas, colunas, tipos, sequências, chave estrangeira e constraint única com o changeSet inicial.
3. Corrija qualquer diferença antes de continuar.
4. Instale a CLI do Liquibase ou use uma estação administrativa preparada pelo professor.
5. Configure as variáveis da CLI sem escrever a senha no comando:

```bash
export LIQUIBASE_COMMAND_URL=jdbc:postgresql://localhost:5432/cursodb
export LIQUIBASE_COMMAND_USERNAME=postgres
export LIQUIBASE_COMMAND_PASSWORD='sua-senha-local'
export LIQUIBASE_COMMAND_CHANGELOG_FILE=src/main/resources/db/changelog/db.changelog-master.xml
```

6. Na raiz do projeto, execute:

```bash
liquibase changelog-sync
```

O `changelog-sync` cria o controle do Liquibase e marca os changeSets como executados sem tentar recriar as tabelas. Ele só é seguro quando a estrutura existente já corresponde ao changelog.

7. Inicie a aplicação e deixe o Hibernate validar o mapeamento.
8. Confira a tabela `DATABASECHANGELOG`.

> Nunca use `changelog-sync` apenas para esconder um erro. Ele declara que o banco já contém exatamente as alterações descritas.

## 11.15 Como gerar um changelog a partir de um banco existente

A CLI do Liquibase também consegue inspecionar um banco:

```bash
liquibase generate-changelog \
  --changelog-file=estrutura-gerada.xml
```

O arquivo gerado deve ser revisado. Ferramentas automáticas podem produzir:

- nomes de constraints pouco didáticos;
- tipos específicos de um único banco;
- objetos que não pertencem à aplicação;
- uma ordem difícil de compreender;
- diferenças em relação às entidades JPA.

Como este projeto possui apenas duas tabelas, o changelog inicial foi escrito e revisado manualmente. Isso facilita relacionar cada coluna com a respectiva anotação JPA.

## 11.16 H2 nos testes

O perfil de teste utiliza:

```properties
spring.datasource.url=jdbc:h2:mem:cursodb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
```

O mesmo master changelog é executado antes dos testes. Assim, os testes deixam de depender de um schema criado de maneira diferente pelo Hibernate.

Execute:

```bash
./mvnw test
```

Um teste verde agora verifica também que o changelog pode criar uma estrutura aceita pelo Hibernate.

## 11.17 Diagnóstico de problemas comuns

### “Table already exists”

O banco já possuía tabelas e ainda não foi preparado para o Liquibase. Use a estratégia A ou B da seção 11.14.

### Erro de checksum

Um changeSet executado foi modificado. Restaure o arquivo original e crie um novo changeSet para a correção.

### Liquibase aguardando lock

Verifique se outra aplicação está migrando o banco. Não remova o lock enquanto houver outra instância em execução. Depois de confirmar uma interrupção anormal, um administrador pode liberar o lock com a ferramenta do Liquibase.

### Hibernate informa coluna ausente ou tipo incompatível

O changelog e a entidade não representam a mesma estrutura. Compare `@Column`, nomes, tamanho, precisão, escala e nulabilidade. Não volte para `ddl-auto=update` para esconder a diferença.

## 11.18 Arquivos modificados

```text
pom.xml
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-test.properties
src/main/resources/db/changelog/db.changelog-master.xml
src/main/resources/db/changelog/changes/001-criar-estrutura-inicial.xml
src/main/java/com/curso/services/DBService.java
docs/aulas/AULA-11-LIQUIBASE.md
```

## 11.19 Checklist

- [ ] A dependência `liquibase-core` está no projeto.
- [ ] O master inclui o changeSet inicial.
- [ ] Hibernate está configurado com `ddl-auto=validate`.
- [ ] A aplicação cria um banco vazio.
- [ ] A segunda execução não recria tabelas nem duplica dados.
- [ ] Os testes usam o mesmo changelog.
- [ ] Sei quando usar recriação e quando usar `changelog-sync`.
- [ ] Sei que changeSet executado não deve ser editado.

## 11.20 Ponto de versionamento

```bash
./mvnw test
git add .
git commit -m "Aula 11: versiona banco de dados com Liquibase"
git tag -a aula-11-liquibase -m "Schema inicial versionado com Liquibase"
git push origin main --follow-tags
```

## 11.21 Referências oficiais

- [Spring Boot — Database Initialization](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)
- [Liquibase — `changelog-sync`](https://docs.liquibase.com/community/reference-guide-5-0/database-inspection-change-tracking-and-utility-commands/changelog-sync)
- [Liquibase — gerar changelog de um banco existente](https://docs.liquibase.com/oss/implementation-guide-4-33/generate-changelog-from-existing-database)
- [Liquibase — tabela `DATABASECHANGELOG`](https://docs.liquibase.com/concepts/basic/databasechangelog-table.html)

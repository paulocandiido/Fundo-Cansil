# Java Spring Boot — Aula 10 — Variáveis de ambiente e arquivo `.env`

## 10.1 Objetivos da aula

Ao final desta aula o estudante deverá ser capaz de:

- explicar por que senhas não devem ficar no código-fonte;
- criar um arquivo `.env` local;
- manter um `.env.example` seguro no Git;
- configurar variáveis de ambiente no IntelliJ IDEA;
- usar placeholders no `application-dev.properties`;
- iniciar o projeto com o perfil `dev` sem publicar a senha do banco.

## 10.2 O problema que será resolvido

Antes desta aula, a configuração de desenvolvimento possuía os dados do PostgreSQL diretamente no arquivo versionado:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cursodb
spring.datasource.username=postgres
spring.datasource.password=senha-exposta-no-repositorio
```

O arquivo `application-dev.properties` faz parte do projeto e é enviado ao GitHub. Consequentemente, qualquer segredo escrito nele também passa a fazer parte do histórico do Git.

Mesmo que a senha seja removida em um commit posterior, ela pode continuar visível nos commits antigos. Por isso, a regra principal é:

> Segredos não devem entrar no Git nem temporariamente.

## 10.3 O que é uma variável de ambiente

Uma variável de ambiente é um par formado por nome e valor, disponibilizado pelo sistema operacional ao processo que executa a aplicação.

Exemplo conceitual:

```text
Nome:  DB_PASSWORD
Valor: minha-senha-local
```

O código permanece igual para todos os estudantes, mas cada computador fornece sua própria configuração. Em um servidor, a equipe de infraestrutura fornece os valores apropriados daquele ambiente.

Neste projeto serão utilizadas quatro variáveis:

| Variável | Finalidade | É segredo? |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Seleciona o perfil Spring | Não |
| `DB_URL` | Endereço JDBC do PostgreSQL | Normalmente não |
| `DB_USERNAME` | Usuário do banco | Informação sensível, mas nem sempre secreta |
| `DB_PASSWORD` | Senha do banco | Sim |

## 10.4 Criando o arquivo `.env`

Na raiz do projeto existe o arquivo `.env.example`. Ele documenta os nomes das variáveis sem fornecer uma senha verdadeira:

```dotenv
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/cursodb
DB_USERNAME=postgres
DB_PASSWORD=troque-esta-senha
```

Crie uma cópia chamada `.env`.

### Pelo IntelliJ IDEA

1. Localize `.env.example` na raiz do projeto.
2. Clique com o botão direito sobre o arquivo.
3. Escolha **Copy**.
4. Clique com o botão direito sobre a raiz do projeto.
5. Escolha **Paste**.
6. Informe o nome `.env`.
7. Substitua `troque-esta-senha` pela senha do seu PostgreSQL local.

### Pelo terminal no Windows PowerShell

```powershell
Copy-Item .env.example .env
```

### Pelo terminal no Linux ou macOS

```bash
cp .env.example .env
```

Exemplo de `.env` local:

```dotenv
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/cursodb
DB_USERNAME=postgres
DB_PASSWORD=minha-senha-local
```

Não coloque espaços em torno do sinal `=`. Para os valores usados nesta aula, aspas não são necessárias.

## 10.5 Protegendo o `.env` com `.gitignore`

Foram acrescentadas estas linhas ao `.gitignore`:

```gitignore
### Variaveis de ambiente locais
.env
.env.*
!.env.example
```

Explicação:

- `.env` ignora o arquivo principal com os valores locais;
- `.env.*` ignora variações como `.env.dev` e `.env.local`;
- `!.env.example` cria uma exceção para que o modelo possa ser versionado.

Confirme a proteção:

```bash
git check-ignore -v .env
git status
```

O `.env` não deve aparecer entre os arquivos que serão commitados. O `.env.example` deve aparecer quando for novo ou modificado.

> O `.gitignore` não remove arquivos que já foram versionados. Se um `.env` já tiver sido commitado, a senha deve ser trocada e o arquivo precisa deixar de ser rastreado. Apenas apagar o arquivo em um commit novo não elimina o segredo do histórico.

## 10.6 Alterando o `application-dev.properties`

A configuração anterior foi substituída por:

```properties
#conexao ao postgresql
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/cursodb}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
```

A sintaxe de placeholder é:

```text
${NOME_DA_VARIAVEL:valor-padrao}
```

Assim:

- `${DB_URL:jdbc:postgresql://localhost:5432/cursodb}` usa `DB_URL` quando ela existir; caso contrário, usa a URL local indicada depois dos dois-pontos;
- `${DB_USERNAME:postgres}` usa `postgres` apenas como usuário padrão local;
- `${DB_PASSWORD}` não possui valor padrão e torna a senha obrigatória.

A senha não recebe um valor padrão porque uma aplicação não deve iniciar silenciosamente com uma senha conhecida por todos.

## 10.7 Importante: quem lê o `.env`?

O Spring Boot lê variáveis fornecidas pelo sistema operacional, pela IDE ou pelo processo que iniciou a aplicação. Ele não transforma automaticamente qualquer arquivo chamado `.env` em variáveis de ambiente.

Neste curso, o `.env` possui duas funções:

1. guardar localmente os valores que pertencem ao estudante;
2. servir como fonte para preencher a configuração de execução do IntelliJ.

Essa separação evita adicionar uma biblioteca apenas para ler quatro valores e deixa claro o papel de cada tecnologia.

## 10.8 Configurando as variáveis no IntelliJ IDEA

1. Abra o projeto no IntelliJ IDEA.
2. No menu principal, escolha **Run → Edit Configurations**.
3. Selecione a configuração que executa `CansilApplication`.
4. Caso ela ainda não exista, clique em **+**, escolha **Spring Boot** ou **Application** e selecione a classe principal.
5. Localize o campo **Environment variables**.
6. Se o campo não estiver visível, use **Modify options** e habilite **Environment variables**.
7. Clique no botão de edição ao lado do campo.
8. Abra o `.env` local e cadastre os mesmos pares de nome e valor:

```text
SPRING_PROFILES_ACTIVE = dev
DB_URL                 = jdbc:postgresql://localhost:5432/cursodb
DB_USERNAME            = postgres
DB_PASSWORD            = senha-local-do-estudante
```

9. Mantenha habilitada a opção de incluir as variáveis do sistema.
10. Clique em **OK** e depois em **Apply**.
11. Execute a aplicação por essa configuração.

As configurações pessoais do IntelliJ ficam normalmente na pasta `.idea`, que também está ignorada pelo Git. Elas não devem ser usadas para transmitir senhas a outros estudantes.

## 10.9 Executando pelo terminal

O arquivo `.env` continua sendo apenas uma referência. No terminal, exporte as variáveis antes de executar o Maven.

### Windows PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:postgresql://localhost:5432/cursodb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua-senha-local"
./mvnw spring-boot:run
```

As variáveis acima valem para a janela atual do PowerShell.

### Linux ou macOS

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://localhost:5432/cursodb
export DB_USERNAME=postgres
export DB_PASSWORD='sua-senha-local'
./mvnw spring-boot:run
```

Use aspas quando a senha tiver caracteres interpretados pelo shell.

## 10.10 Como o Spring escolhe os valores

As variáveis de ambiente podem substituir valores presentes nos arquivos `application.properties`. Esse comportamento permite que o mesmo JAR seja utilizado em desenvolvimento, homologação e produção.

Fluxo deste projeto:

```text
IntelliJ ou terminal
        ↓
variáveis de ambiente
        ↓
placeholders do application-dev.properties
        ↓
configuração automática do DataSource
        ↓
conexão com PostgreSQL
```

Não é necessário escrever uma classe Java para ler `DB_PASSWORD`. O próprio Spring Boot resolve o placeholder e configura o `DataSource`.

## 10.11 Cuidados de segurança

1. Nunca envie `.env` para GitHub, Moodle, e-mail ou grupo de mensagens.
2. Nunca use uma senha de banco igual à senha institucional ou pessoal.
3. Use uma conta de banco com apenas as permissões necessárias.
4. Não registre senhas com `System.out.println` ou logger.
5. Não coloque segredos em prints de tela usados na apostila.
6. Em produção, utilize o gerenciador de segredos oferecido pela infraestrutura.
7. Se uma senha for publicada, considere-a comprometida e altere-a imediatamente.
8. O `.env.example` deve conter apenas valores falsos ou públicos.

## 10.12 Teste da aula

1. Inicie o PostgreSQL.
2. Configure as quatro variáveis no IntelliJ.
3. Execute a aplicação.
4. Confirme no console que o perfil ativo é `dev`.
5. Altere temporariamente `DB_PASSWORD` para um valor incorreto e observe a falha de autenticação.
6. Restaure a senha correta.
7. Execute `git status` e confirme que `.env` não aparece.

## 10.13 Arquivos modificados

```text
.env.example
.gitignore
src/main/resources/application-dev.properties
docs/aulas/AULA-10-VARIAVEIS-DE-AMBIENTE.md
```

## 10.14 Checklist

- [ ] Criei meu `.env` local.
- [ ] Minha senha não está no `application-dev.properties`.
- [ ] O Git ignora meu `.env`.
- [ ] Configurei as variáveis no IntelliJ.
- [ ] A aplicação inicia com o perfil `dev`.
- [ ] Sei explicar por que o `.env.example` pode ser versionado.

## 10.15 Ponto de versionamento

```bash
git add .
git commit -m "Aula 10: externaliza configuracao do banco"
git tag -a aula-10-variaveis-ambiente -m "Variaveis de ambiente e seguranca"
git push origin main --follow-tags
```

Antes do commit, verifique novamente que `.env` não foi incluído.

## 10.16 Referências oficiais

- [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html)
- [IntelliJ IDEA — Program arguments, VM options and environment variables](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html)
- [IntelliJ IDEA — Run/Debug Configurations](https://www.jetbrains.com/help/idea/run-debug-configurations-dialog.html)

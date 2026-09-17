# Fundo Cansil — Backend

**API REST para gestão de carteira de investimentos, desenvolvida com Java 17 e Spring Boot.**

O Fundo Cansil permite cadastrar usuários e corretoras, registrar compras e vendas, consultar cotações e acompanhar posições consolidadas ou por instituição. O backend concentra autenticação, regras de negócio, persistência e integração com fontes externas. As operações são registros de acompanhamento: o sistema não envia ordens para corretoras.

## Funcionalidades

- Cadastro e login com JWT, Spring Security e senhas protegidas por BCrypt.
- Consulta e cadastro de corretoras por CNPJ com integração à BrasilAPI.
- Registro de compras e vendas com validação de saldo e precisão decimal.
- Cálculo de quantidade, preço médio e custo restante por ativo e por corretora.
- Avaliação da carteira com cotações e lucro ou prejuízo da posição.
- Catálogo de ativos e histórico de consultas de cotações.
- Migrações de banco com Liquibase e testes unitários e de integração.

## Tecnologias

| Área | Ferramentas |
| --- | --- |
| Linguagem e framework | Java 17, Spring Boot 3.5.5 |
| API e validação | Spring Web, Bean Validation |
| Segurança | Spring Security, JWT, BCrypt |
| Persistência | Spring Data JPA, Hibernate, PostgreSQL 17, Liquibase |
| Testes | JUnit 5, Mockito, Spring Boot Test, H2 |
| Build e execução | Maven Wrapper, Docker, Docker Compose |

A brapi é a fonte de cotações ativa por padrão. Os adaptadores Alpha Vantage e HG Finance estão implementados e desativados na configuração padrão. A disponibilidade de ativos depende da cobertura e das permissões dos provedores.

## Executar com Docker

O Compose desta pasta reúne PostgreSQL, backend e frontend Angular 22 servido por Nginx. Para compilar a aplicação completa, mantenha a estrutura:

```text
GestaoAcoes/
├── cansil/       # Este diretório
└── frontend/     # Angular e Nginx
```

### 1. Configurar o ambiente

Com Docker e Compose v2 em execução, use o PowerShell nesta pasta:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Edite o `.env` e substitua as credenciais fictícias:

| Variável | Finalidade |
| --- | --- |
| `POSTGRES_PASSWORD` | Senha privada do PostgreSQL no Compose |
| `BRAPI_TOKEN` | Token válido para consultas de cotações |
| `JWT_SECRET` | Segredo aleatório de pelo menos 32 bytes |
| `POSTGRES_VOLUME_NAME` | Volume externo que armazena o banco |
| `CORS_ALLOWED_ORIGINS` | Origens autorizadas a acessar a API |

Mantenha `.env`, tokens e backups fora do Git.

### 2. Preparar o volume

**Somente para uma instalação nova, sem dados anteriores**, escolha um nome exclusivo e crie o volume:

```powershell
docker volume create fundo-cansil-dados-novo
```

Defina `POSTGRES_VOLUME_NAME=fundo-cansil-dados-novo` no `.env`. O volume é externo e precisa existir antes da inicialização. Para um banco existente, preserve volume e credenciais conforme o [guia Docker](DOCKER.md).

### 3. Iniciar a aplicação

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

| Serviço | Acesso padrão |
| --- | --- |
| Interface Angular | http://127.0.0.1:4200 |
| API REST | http://127.0.0.1:8080/api |
| PostgreSQL no host | `localhost:5433` |

Crie uma conta pela interface e cadastre uma corretora para começar. A carteira inicial fica vazia.

Para executar somente API e banco, após configurar credenciais e volume:

```powershell
docker compose up -d --build backend postgres
```

Para parar os serviços preservando os dados, use `docker compose stop`. O [guia Docker](DOCKER.md) detalha portas, logs e atualização dos serviços.

## Desenvolvimento e testes

Com JDK 17 instalado, use o Maven Wrapper no PowerShell:

```powershell
# Testes com perfil test, H2 e integrações externas simuladas
.\mvnw.cmd test

# Compilação, testes e empacotamento
.\mvnw.cmd verify

# Execução local, após configurar as variáveis do processo
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, substitua `.\mvnw.cmd` por `./mvnw`.

O Spring Boot não lê `.env` automaticamente. Para executar fora do Docker, configure `SPRING_PROFILES_ACTIVE=dev`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `BRAPI_TOKEN` e `JWT_SECRET` no ambiente do processo. Com o PostgreSQL Docker padrão, use `jdbc:postgresql://localhost:5433/cursodb`. Evite executar duas instâncias da API na mesma porta.

Os testes com respostas simuladas não exigem tokens reais e não comprovam disponibilidade ou cotas das integrações externas.

## Organização do código

```text
src/main/java/com/curso/
├── cadastro/       # Integração de CNPJ
├── config/         # Segurança, CORS e configuração
├── cotacao/        # Provedores e orquestração de cotações
├── domains/        # Entidades, DTOs e enums
├── repositories/   # Acesso aos dados
├── resources/      # Endpoints REST e tratamento de erros
├── security/       # Autenticação JWT
└── services/       # Regras de negócio e cálculos
```

As migrações ficam em `src/main/resources/db/changelog` e os testes em `src/test`.

## API REST

Cadastro e login são públicos. As demais rotas de investimentos exigem `Authorization: Bearer <token>`.

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/auth/cadastro` | Criar conta |
| `POST` | `/api/auth/login` | Autenticar e obter JWT |
| `GET` | `/api/usuarios/me` | Consultar usuário autenticado |
| `GET`, `POST` | `/api/corretoras` | Listar e cadastrar corretoras |
| `GET` | `/api/corretoras/cnpj/{cnpj}` | Consultar cadastro pelo CNPJ |
| `DELETE` | `/api/corretoras/{id}` | Excluir corretora e suas transações |
| `GET` | `/api/ativos` | Pesquisar catálogo de ativos |
| `GET`, `POST` | `/api/transacoes` | Listar e registrar operações |
| `GET` | `/api/carteira` | Consultar posições consolidadas |
| `GET` | `/api/carteira/corretoras` | Consultar posições por corretora |
| `GET` | `/api/carteira/avaliacao` | Avaliar posições com cotações |
| `GET` | `/api/cotacoes/{simbolo}` | Consultar cotação de um ativo |
| `GET` | `/api/cotacoes/historico` | Consultar histórico de cotações |

Consulte os contratos e exemplos no [guia de integração REST](FRONTEND.md).

## Regras e limitações

- Vendas são limitadas ao saldo da corretora escolhida. Valores precisam ser positivos, com até seis casas decimais.
- O servidor atribui o horário da operação no fuso `America/Sao_Paulo`.
- Excluir uma corretora exclui permanentemente suas transações e recalcula as posições. Não há edição ou exclusão individual de operações na API.
- A moeda é derivada do mercado (`BR` → `BRL`; `USA` → `USD`), sem conversão cambial. A cobertura atual da integração principal é B3; selecionar um mercado não garante disponibilidade de dados.
- Preço médio, custo e avaliação são informações gerenciais, sem apuração fiscal. As bases por corretora podem diferir da visão consolidada.
- Cotações dependem da disponibilidade, das permissões e das cotas dos provedores.
- O domínio legado de produtos e grupos permanece no backend. Suas rotas públicas exigem revisão antes de uma publicação na internet, junto à configuração de HTTPS, CORS e exposição das portas.

## Documentação técnica

- [DOCKER.md](DOCKER.md): configuração, execução e preservação de dados.
- [FRONTEND.md](FRONTEND.md): contratos REST, autenticação e integração com a interface.
- [INVESTIMENTOS.md](INVESTIMENTOS.md): regras, testes e limitações dos provedores.
- [ENTREGA.md](ENTREGA.md): empacotamento somente do backend e alcance das verificações.

### Material de estudo

- [Aula 10 — Variáveis de ambiente](docs/aulas/AULA-10-VARIAVEIS-DE-AMBIENTE.md)
- [Aula 11 — Liquibase](docs/aulas/AULA-11-LIQUIBASE.md)
- [Aula 12 — Docker e Docker Compose](docs/aulas/AULA-12-DOCKER-E-CONTAINERS.md)

## Licença

Ainda não há um arquivo de licença definido para o projeto.

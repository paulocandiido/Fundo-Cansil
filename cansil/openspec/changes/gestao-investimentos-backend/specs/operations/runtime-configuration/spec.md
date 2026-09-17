## Purpose

Assegurar execução reproduzível em desenvolvimento, testes e Docker, mantendo todos os segredos fora do código e o schema sob controle do Liquibase.

## ADDED Requirements

### Requirement: Integração de navegador com origens explícitas
O sistema SHALL permitir CORS em `/api/**` somente para origens configuradas, processando preflight antes da autenticação e sem habilitar credenciais por cookie.

#### Scenario: Origem local autorizada
- **WHEN** um preflight solicita Authorization e Content-Type de origem permitida
- **THEN** recebe permissão CORS sem exigir JWT

#### Scenario: Origem não autorizada
- **WHEN** uma origem fora da lista solicita preflight
- **THEN** recebe rejeição sem Access-Control-Allow-Origin

### Requirement: Segredos externos
O sistema MUST obter senha do banco, `BRAPI_TOKEN`, `ALPHAVANTAGE_TOKEN`, `HGFINANCE_TOKEN` e segredo JWT de variáveis externas, sem valor real ou default versionado.

Tokens de Alpha e HG são obrigatórios somente quando a respectiva fonte está habilitada. Desativá-las MUST dispensar seus tokens sem dispensar BRAPI_TOKEN, senha do banco ou segredo JWT.

#### Scenario: Fonte desativada sem token
- **WHEN** Alpha ou HG está desativada e seu token está ausente
- **THEN** a inicialização prossegue sem criar seu provider

#### Scenario: Segredo obrigatório ausente
- **WHEN** dev ou Docker inicia sem uma credencial exigida
- **THEN** a inicialização falha indicando a variável, sem adotar segredo padrão

#### Scenario: Arquivo de exemplo
- **WHEN** `.env.example` documenta as variáveis
- **THEN** contém somente valores falsos e `.env` permanece ignorado

### Requirement: Liquibase como fonte do schema
O sistema SHALL criar tabelas, sequências, constraints e índices por changelogs versionados e manter Hibernate em validação.

#### Scenario: Banco vazio
- **WHEN** a aplicação inicia em banco suportado vazio
- **THEN** Liquibase aplica mudanças e Hibernate valida as entidades

### Requirement: Docker reproduzível
O sistema SHALL propagar tokens, URLs e configuração JWT pelo Compose, aguardar PostgreSQL saudável e preservar build multi-stage, usuário não-root, healthcheck e volume.

#### Scenario: Subida completa
- **WHEN** um `.env` válido é fornecido e `docker compose up -d --build` é executado
- **THEN** aplicação e banco iniciam sem configuração manual adicional

### Requirement: Testes isolados
O sistema SHALL usar H2 no perfil de testes e doubles dos três providers, sem rede ou tokens reais.

#### Scenario: Suíte Maven
- **WHEN** testes são executados sem serviços externos
- **THEN** usam H2 e respostas controladas dos providers

## Context

O projeto é um monólito Spring Boot 3.5.5/Java 17 organizado em domínios, repositories, services, resources e mappers. Liquibase controla o schema; PostgreSQL atende dev/produção e H2 atende testes. Não há segurança nem integração HTTP atual. As três APIs de cotação possuem autenticação e formatos distintos, mas o restante do domínio não deve depender desses contratos externos.

## Goals / Non-Goals

**Goals:**

- Adicionar o domínio financeiro sem quebrar produtos existentes.
- Isolar cada fonte externa atrás de contrato comum extensível.
- Tornar fallback, cálculos monetários, identidade e erros determinísticos e testáveis.
- Manter segredos externos e execução reproduzível em H2/PostgreSQL/Docker.

**Non-Goals:**

- Frontend, ordens reais, custódia, dividendos, impostos, taxas, desdobramentos, múltiplas moedas ou streaming.
- Cache distribuído, revogação de JWT ou snapshots persistidos da carteira.
- Commits, tags, push ou publicação sem autorização específica posterior.

## Decisions

### Operação temporária autorizada somente com brapi

Após confirmar que a conta Twelve Data Basic não resolve o acesso à B3, o usuário autorizou prosseguir com brapi e desativar temporariamente Alpha/HG. `ALPHAVANTAGE_ENABLED` e `HGFINANCE_ENABLED` têm padrão false em dev/Compose. Providers desativados não integram a lista do orquestrador e não exigem token. Habilitar exige token não vazio; testes mantêm as três fontes para preservar cobertura. DTOs, endpoints, histórico existente e implementações são preservados. A prova real de fallback continua pendente, não dispensada. Não integrar Twelve Data nesta etapa.

### 1. Modelo persistente e carteira derivada

Criar `Usuario`, `Ativo`, `Transacao` e `Consulta` por changelog aditivo. E-mail, CPF e símbolo terão constraints adequadas. Valores e quantidades usarão `BigDecimal`. `Consulta` armazenará também a fonte. A posição será reduzida do histórico ordenado por data e id; venda será transacional com lock para impedir saldo negativo concorrente.

Alternativa de tabela de saldo foi rejeitada por duplicar a fonte de verdade.

### 2. Estratégia de providers

Definir `CotacaoProvider` com identificação da fonte e método que retorna um `CotacaoAtual` interno. Implementar `BrapiProvider`, `AlphaVantageProvider` e `HgFinanceProvider`, cada um com propriedades, DTOs e mapeamento próprios. O orquestrador receberá a lista ordenada explicitamente e tentará o próximo provider para falha técnica, não-2xx ou ausência de dado.

Adicionar uma fonte exigirá nova implementação e configuração de ordem, sem reescrever o algoritmo. Uma cadeia hardcoded dentro de um único client foi rejeitada por acoplamento e baixa testabilidade.

### 3. Cliente HTTP existente

Usar `RestClient` do Spring Web, sem adicionar biblioteca HTTP. Cada provider configura URL, token, timeouts e autenticação conforme a API. A brapi envia bearer token e desserializa o envelope para retornar `results[0].data`. DTOs toleram campos desconhecidos, mas não usam `Map` ou `Object` como contrato.

### 4. Classificação de resultado e erro agregado

Providers converterão falha de autenticação, limite, timeout/transporte, resposta não-2xx, corpo inválido e símbolo ausente em exceções de provider que não contêm segredos. O orquestrador acumulará fonte e categoria de cada tentativa e, se todas falharem, lançará `CotacaoIndisponivelException` com resumo seguro. O advice mapeará o erro para resposta estável.

### 5. Histórico após sucesso

O serviço de cotação chama o orquestrador, localiza/cria o ativo e grava histórico somente depois de uma cotação válida. O log estruturado e a entidade `Consulta` recebem a mesma enum de fonte. Falhas totais não criam consulta bem-sucedida.

### 6. Cálculo financeiro puro

Um componente puro reduz transações: compras atualizam custo e média; vendas reduzem quantidade mantendo média; saldo zero zera custo e média. Cálculos usam `BigDecimal` e `RoundingMode.HALF_UP`. Avaliação chama o mesmo orquestrador para cada posição e falha sem consolidado parcial se algum ativo esgotar todas as fontes.

### 7. Segurança e DTOs

Adicionar Spring Security, BCrypt e JWT stateless. Cadastro/login serão públicos; domínio financeiro será protegido e obterá o usuário do principal, nunca de `usuarioId` fornecido pelo cliente. Entidades não serão retornadas diretamente; DTOs explícitos omitirão CPF, senha, hash e credenciais.

### 8. Testes

JUnit/Mockito cobrirá cada provider, o orquestrador e services. Testes do orquestrador verificarão ordem, short-circuit, resposta vazia e falha total. MockMvc/H2 substituirá os três providers por doubles e cobrirá autenticação, endpoints, persistência da fonte e ausência de dados sensíveis. Nenhum teste chamará APIs reais.

### 9. Configuração e Docker

Adicionar URLs e tokens das três fontes, além de segredo/expiração JWT, ao perfil dev e Compose. `.env.example` terá somente placeholders falsos autorizados. Segredos não terão default; valores não sensíveis podem usar `${VAR:-padrao}`. Dockerfile, healthcheck e volume serão preservados.

## Risks / Trade-offs

- [Limites diferentes entre APIs] → classificar rate limit e seguir fallback; registrar somente metadados seguros.
- [Símbolos/formas de mercado divergentes] → normalizar símbolo e mapear apenas campos comuns necessários.
- [Fallback aumenta latência] → configurar timeouts por provider e interromper a cadeia no primeiro sucesso.
- [Todas as fontes podem falhar] → erro agregado explícito, sem resposta parcial enganosa.
- [Concorrência em vendas] → transação e lock no conjunto usuário/ativo.
- [Diferenças H2/PostgreSQL] → H2 em modo PostgreSQL e validação adicional pelo Compose.
- [JWT roubado vale até expirar] → validade curta configurável e ausência de tokens em logs.

## Migration Plan

### Preparação para frontend autorizada em 02/09/2026

Sem criar telas ou escolher framework, adicionar CORS com lista explícita de origens locais configurável por `CORS_ALLOWED_ORIGINS`, sem cookies cross-origin. JWT continua em Authorization. Padronizar erros 401/403 de segurança em `StandardError` e disponibilizar `/api/usuarios/me` derivado do principal. Fornecer cliente TypeScript de referência com token apenas em memória, exemplos HTTP e guia dos contratos, erros e limitações. Validar testes e aplicação Docker com volume preservado. A prova de fallback real permanece independente e pendente conforme verificação existente.

1. Aplicar changelog aditivo sem remover tabelas legadas.
2. Configurar os três tokens, URLs, JWT e banco no ambiente.
3. Implantar e validar healthcheck, cadastro/login, cotação primária, fallback e operação.
4. Em rollback de código, manter tabelas novas inativas; remoção física exige changelog futuro deliberado.

## 1. Modelagem e Liquibase

- [x] 1.1 Criar enums e entidades `Usuario`, `Ativo`, `Transacao` e `Consulta`, incluindo a fonte da cotação, e verificar compilação e testes de domínio.
- [x] 1.2 Criar repositories isolados por usuário, ordenação e lock de operações; verificar testes H2 de constraints e consultas.
- [x] 1.3 Criar changelog `002` com sequências, tabelas, FKs, unicidades e índices e incluí-lo no master; verificar inicialização H2 com `ddl-auto=validate`.

## 2. Provider tipado da brapi

- [x] 2.1 Definir contrato comum `CotacaoProvider`, DTO interno e propriedades externas; verificar compilação e binding sem token hardcoded.
- [x] 2.2 Implementar DTOs tipados e `BrapiProvider` com `RestClient`, bearer token e extração de `results[0].data`; verificar testes de URL, header e resposta válida.
- [x] 2.3 Mapear timeout, transporte, não-2xx, corpo inválido e resultado vazio para falhas seguras do provider; verificar testes unitários sem vazamento do token.

## 3. Providers de fallback e orquestração

- [x] 3.1 Implementar `AlphaVantageProvider` com DTOs e mapeamento para o modelo interno; verificar sucesso, erro e símbolo ausente com fixtures controladas.
- [x] 3.2 Implementar `HgFinanceProvider` com DTOs e mapeamento para o modelo interno; verificar sucesso, erro e símbolo ausente com fixtures controladas.
- [x] 3.3 Implementar orquestrador na ordem brapi → Alpha Vantage → HG Finance; verificar ordem, short-circuit e fallback para exceção, não-2xx e resposta 2xx vazia.
- [x] 3.4 Implementar erro agregado quando todas falharem e logging seguro da fonte bem-sucedida; verificar causas preservadas, mensagem clara e ausência de tokens.

## 4. Regras de compra, venda e carteira

- [x] 4.1 Implementar calculadora pura com `BigDecimal`/`HALF_UP`; verificar compras ponderadas, venda parcial e zeragem.
- [x] 4.2 Implementar lançamento transacional de compra/venda e rejeição de saldo insuficiente sob lock; verificar testes Mockito de sucesso e falha.
- [x] 4.3 Implementar histórico e posições abertas por usuário; verificar ordem determinística e isolamento.
- [x] 4.4 Implementar avaliação via orquestrador com custo, valor e lucro/prejuízo; verificar fonte usada, lucro, prejuízo e falha total sem consolidado parcial.

## 5. Endpoints REST e histórico de consultas

- [x] 5.1 Criar DTOs/mappers de usuário, autenticação, cotação, histórico, transação e carteira; verificar serialização sem CPF, senha, hash ou tokens.
- [x] 5.2 Implementar serviço que persiste ativo e consulta com a fonte somente após sucesso; verificar histórico decrescente e nenhuma gravação em falha total.
- [x] 5.3 Criar endpoints de cotação, histórico, transações e carteira derivados do principal; verificar contratos com MockMvc sem `usuarioId` de autorização.
- [x] 5.4 Ampliar advice/`StandardError` para validação, conflito, regra de negócio, provider e falha total; verificar status e mensagens padronizados.

## 6. Segurança

- [x] 6.1 Adicionar Spring Security/JWT ao Maven e configurar BCrypt, sessão stateless e política das rotas novas/legadas; verificar build e acesso público/protegido.
- [x] 6.2 Implementar cadastro com unicidade e hash; verificar banco sem senha pura e resposta sem CPF/hash.
- [x] 6.3 Implementar login, emissão/validação JWT e filtro com segredo/expiração externos; verificar token válido, expirado, inválido e login incorreto.
- [x] 6.4 Verificar isolamento integral com dois usuários em testes de service e API.

## 7. Testes caixa-branca

- [x] 7.1 Completar JUnit/Mockito de autenticação, consulta, transação, cálculo e avaliação; verificar todos os ramos críticos sem rede.
- [x] 7.2 Cobrir individualmente os três providers e o orquestrador, incluindo token inválido, símbolo ausente, timeout, não-2xx, ordem e falha total.
- [x] 7.3 Atualizar a suíte agregadora quando necessário e executar `./mvnw test`; verificar ausência de regressão no domínio legado.

## 8. Testes caixa-preta

- [x] 8.1 Configurar MockMvc/H2/Liquibase com doubles dos três providers; verificar contexto sem PostgreSQL, rede ou tokens reais.
- [x] 8.2 Testar cadastro, login, 401, isolamento e ausência de dados sensíveis.
- [x] 8.3 Testar cotação primária, fallback 1, fallback 2, resposta vazia e falha total; verificar fonte persistida e ausência de histórico em falha.
- [x] 8.4 Testar compras, vendas, saldo insuficiente, preço médio e avaliação; verificar payload e estado H2.
- [x] 8.5 Executar `./mvnw clean test` até obter build verde.

## 9. Docker, configuração e versionamento

- [x] 9.1 Adicionar propriedades das três fontes e JWT em dev/test sem defaults secretos; verificar binding e falha clara para variável obrigatória ausente.
- [x] 9.2 Atualizar `.env.example` com placeholders falsos e Compose com `BRAPI_TOKEN`, `ALPHAVANTAGE_TOKEN`, `HGFINANCE_TOKEN` e JWT; verificar `docker compose config` sem segredo real.
- [ ] 9.3 Executar `docker compose up -d --build` e smoke test autenticado com fallback, preservando Dockerfile, healthcheck e volume.
- [x] 9.4 Revisar diff e segredos e preparar nove commits/tags sugeridos; verificar `git status` antes de qualquer commit, tag ou push, que exigem autorização separada.

## 10. Preparação para integração do frontend solicitada pelo usuário

Nota: a suspensão temporária dos fallbacks foi autorizada posteriormente e está descrita na seção 11; não representa conclusão da prova real prevista em 9.3.

- [x] 10.1 Configurar CORS por origens explícitas e erros JSON de segurança; testar preflight, origem rejeitada e 401 legível.
- [x] 10.2 Disponibilizar GET `/api/usuarios/me` autenticado com DTO seguro; testar identidade e ausência de dados sensíveis.
- [x] 10.3 Entregar guia de integração, exemplos HTTP e cliente TypeScript independente de framework, sem implementar telas.
- [x] 10.4 Executar suíte completa, atualizar aplicação Docker preservando banco e verificar CORS/identidade e migração 003 no PostgreSQL.

## 11. Operação temporária somente com brapi autorizada pelo usuário

- [x] 11.1 Permitir desativar Alpha/HG sem tokens, preservar implementações e testar ausência de chamadas e reativação.
- [x] 11.2 Documentar os controles e manter contratos do frontend; executar suíte e atualizar Docker sem remover dados.

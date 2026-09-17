## Why

O projeto precisa evoluir para um back-end de gestão de investimentos seguro e reproduzível, capaz de manter operações por usuário e avaliar carteiras mesmo quando uma fonte de cotação estiver indisponível. A consulta deve usar a brapi como fonte primária e recorrer automaticamente à Alpha Vantage e à HG Finance.

## What Changes

- Adicionar o domínio persistente de usuários, ativos, transações e consultas por Liquibase.
- Integrar brapi, Alpha Vantage e HG Finance por providers tipados que normalizam respostas distintas em um DTO interno único.
- Orquestrar fallback na ordem brapi → Alpha Vantage → HG Finance para erro, timeout, não-2xx ou ausência do dado solicitado.
- Registrar a fonte bem-sucedida no log e no histórico e retornar erro específico quando todas as fontes falharem.
- Registrar compras e vendas, impedir venda sem saldo e calcular quantidade e preço médio ponderado das posições abertas.
- Avaliar carteira com cotação atual, custo médio e lucro/prejuízo absoluto e percentual.
- Disponibilizar APIs REST com DTOs que nunca exponham CPF, senha, hash, tokens ou credenciais.
- Proteger APIs com Spring Security, BCrypt e JWT.
- Cobrir services/providers com JUnit/Mockito e endpoints com MockMvc/H2.
- Atualizar configuração e Docker com placeholders falsos para os três tokens e demais segredos autorizados.
- Manter `GrupoProduto` e `Produto` compatíveis e não implementar frontend.

## Capabilities

### New Capabilities

- `identity/user-authentication`: cadastro seguro, autenticação JWT e isolamento de dados por usuário.
- `market/stock-quotes`: providers tipados, normalização, fallback ordenado e histórico da fonte usada.
- `portfolio/transactions`: compras, vendas, validação de saldo e cálculo da posição.
- `portfolio/valuation`: avaliação atual da carteira e métricas de lucro/prejuízo.
- `operations/runtime-configuration`: configuração externa, Liquibase, H2 e Docker reproduzível.

### Modified Capabilities

Nenhuma. O repositório ainda não possui especificações OpenSpec consolidadas.

## Impact

- Novas entidades, enums, repositories, DTOs, providers, services, resources e exceções em `src/main/java`.
- Novo changelog Liquibase e inclusão no master.
- Dependências Maven de Spring Security e JWT; clientes HTTP usarão recursos já disponíveis no Spring Web.
- Novas propriedades e variáveis `BRAPI_TOKEN`, `ALPHAVANTAGE_TOKEN`, `HGFINANCE_TOKEN`, URLs das fontes e configuração JWT.
- Ampliação dos testes unitários e MockMvc/H2, incluindo falha individual e total dos providers.
- Alterações em `.env.example` e `compose.yaml` somente com valores falsos/placeholders, conforme autorização expressa.


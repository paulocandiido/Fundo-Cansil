## Why

O usuário precisa cadastrar corretoras existentes por CNPJ e razão social verificável, escolhendo um único mercado para suas operações.

## What Changes

- Consulta cadastral explícita por CNPJ via BrasilAPI; nome fornecido pelo servidor, sem texto livre.
- **BREAKING** Cadastro e listagem de corretoras passam a ser exclusivos de cada usuário, com CNPJ único por conta e mercado BR ou USA fixo.
- Migração preserva operações antigas como corretoras pendentes de regularização; vendas continuam possíveis.
- Tela de corretoras, seleção de mercado, validação de duplicidade e bloqueio de compras incompatíveis.
- Ativos continuam vindo da brapi/B3. Cadastro USA permitido; operações USA indisponíveis enquanto não houver catálogo/cotações compatíveis, sem tratar BDRs como mercado USA.

## Capabilities

### New Capabilities

- `portfolio/broker-registration`: Cadastro verificado de corretoras, isolamento por usuário e mercado único.

### Modified Capabilities

Nenhuma spec principal foi arquivada até esta mudança. A evolução complementa selecionar-ativos-corretoras.

## Impact

Backend Spring, migração aditiva 005, API autenticada, interface Angular, testes e documentação. Consulta de dados cadastrais públicos pela BrasilAPI. Sem novas chaves ou dependências. Os artefatos de cada projeto permanecem em sua raiz OpenSpec local.


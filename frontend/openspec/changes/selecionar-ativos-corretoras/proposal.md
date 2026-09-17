## Why

O frontend precisa aplicar a parte Angular da mudança já aprovada no projeto coordenador do backend, permitindo selecionar ativo e corretora e acompanhar posições por instituição sem ultrapassar o escopo de edição definido pelo OpenSpec.

## What Changes

- Consumir os novos contratos autenticados de corretoras, catálogo de ativos e posições por corretora.
- Exigir seleção explícita de ativo e corretora no registro de compra ou venda.
- Exibir a carteira gerencial por corretora nas seis colunas aprovadas, com alternância para a visão consolidada.
- Atualizar cotações somente por ação explícita, reaproveitando a avaliação consolidada por símbolo.
- Preservar autenticação em memória, tratamento de resultado incerto, acessibilidade e responsividade existentes.

## Capabilities

### New Capabilities
- `investment-selection`: seleção assistida de ativo e corretora e visualização gerencial por instituição.

### Modified Capabilities
- Nenhuma especificação-base local existente.

## Impact

- Código Angular em `src/app/core` e `src/app/pages`.
- Contrato HTTP com o backend local em `/api/corretoras`, `/api/ativos`, `/api/carteira/corretoras` e endpoints já existentes.
- Sem dependências novas, publicação ou credenciais de provedores no frontend.

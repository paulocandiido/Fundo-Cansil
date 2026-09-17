## Why

O usuário aprovou selecionar ativo e corretora ao registrar operações, controlar o saldo por corretora e apresentar uma carteira mais clara. Hoje a corretora não é persistida e as posições são calculadas somente por usuário e ativo.

## What Changes

- Oferecer seleção pesquisável de ativos pelo catálogo da brapi, sem expor chaves e sem consultar uma cotação a cada tecla.
- Disponibilizar catálogo de corretoras e persistir a corretora em cada compra/venda, exibindo-a no histórico.
- **BREAKING**: exigir `corretoraId` nos novos registros de transações; atualizar o frontend e os exemplos de consumo em conjunto.
- Validar saldo histórico por usuário, ativo e corretora, preservando a validação consolidada e o bloqueio concorrente.
- Acrescentar posições por corretora sem substituir os endpoints consolidados existentes.
- Exibir exatamente as colunas aprovadas: **Ativo → Corretora → Quantidade → Valor investido → Preço médio → Cotação atual**, sem coluna Nome. Atalhos de operação podem ficar no detalhe da linha.
- Apresentar o custo da posição restante e seu preço médio gerencial por corretora; manter separado o cálculo consolidado por ativo, sem promessa de apuração fiscal.
- Consultar cotações explicitamente, uma vez por símbolo distinto, com fonte e horário de consulta, sem promessa de tempo real. Uma indisponibilidade não deve ocultar posições e custos já conhecidos.
- Migrar as operações existentes para uma categoria técnica **Corretora não informada**, sem inventar instituição ou modificar valores, datas e titularidade.

## Capabilities

### New Capabilities

- `market/asset-selection`: catálogo pesquisável de símbolos, proteção das credenciais, cache e seleção sem gravar consultas de preço.
- `portfolio/broker-positions`: corretoras, atribuição de operações, saldo histórico e custo gerencial por corretora, preservação do consolidado e do legado.
- `ui/investment-selection`: formulários de seleção, histórico com corretora e tabela de posições no Angular existente.

### Modified Capabilities

Nenhuma especificação consolidada existe em `openspec/specs`. Estas capacidades complementam o backend já implementado em `gestao-investimentos-backend`, sem marcar sua validação de fallback real como concluída.

## Impact

- Backend: entidade/catálogo de corretoras, novo changelog Liquibase aditivo, transações/DTOs/repositories/services, cliente de catálogo brapi e novos endpoints autenticados.
- Frontend Angular existente na pasta irmã `../frontend`: contratos HTTP, seleção, carteira, histórico e testes. A aplicação deve respeitar o escopo de edição do contexto OpenSpec; a localização irmã deve ser resolvida antes das edições de frontend, sem mover o projeto ou sobrescrever configurações.
- Testes JUnit/Mockito/MockMvc/H2 e Angular, documentação de integração e compatibilidade da migração com PostgreSQL.
- Sem novas dependências previstas, sem novos segredos, sem alteração de `.env`, sem reativar Alpha/HG e sem remover dados.

## Non-goals

- Envio de ordens reais, conexão com contas de corretoras, importação B3, apuração fiscal, taxas, dividendos ou eventos societários.
- Transferências de custódia nesta entrega: exigirão fluxo e registro próprios; nunca simular transferência como compra/venda para contornar saldo.
- CRUD administrativo de corretoras, catálogo exaustivo de todas as instituições, edição/exclusão de operações, publicação na internet ou mudanças de provedor/plano.

## Approval

O usuário aprovou o desenho funcional nesta conversa. Este conjunto formal de artefatos será apresentado antes da aplicação, conforme o fluxo `openspec-propose`; nenhuma implementação ou migração é executada nesta etapa.

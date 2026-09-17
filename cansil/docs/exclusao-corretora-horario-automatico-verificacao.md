# Exclusão de corretora e horário automático — 08/09/2026

## Entrega

- O formulário de compra/venda não contém campo de data ou hora.
- O backend ignora a data de clientes antigos e grava `dataOperacao` com seu relógio no fuso `America/Sao_Paulo` no momento da confirmação.
- O histórico continua mostrando o dia e o horário retornados pelo backend.
- A exclusão da corretora é permanente. Após validar autenticação e propriedade, o backend bloqueia o usuário, exclui todas as transações dessa corretora e então exclui a corretora na mesma transação de banco.
- As posições relacionadas desaparecem por serem calculadas das transações. Consultas de cotação e ativos do catálogo não são apagados, pois não pertencem à corretora.
- A tela exige confirmação e informa claramente que compras, vendas e posições serão excluídas. A mensagem anterior de preservação foi removida.

## Verificação

- Backend no Docker: **190 testes encontrados, 189 executados, 1 condicional ignorado, zero falhas e zero erros**.
- Angular: **67 testes aprovados em 7 arquivos**.
- Cliente TypeScript de referência: **8 testes aprovados**.
- Build Angular Docker: `main-VW7SAER7.js`, 402,19 kB iniciais e estimativa de transferência de 103,49 kB.
- O teste unitário do serviço fixa o relógio e confirma exatamente o horário de Brasília gravado.
- Os testes de integração confirmam exclusão da corretora, operações e posições, isolamento entre contas e impedimento de reutilizar o ID excluído.
- O site servido em `http://127.0.0.1:4200` não contém o controle `data-operacao`, contém a informação de horário automático e o aviso de exclusão permanente. A API sem token retorna 401, conforme esperado.

## Segurança dos dados durante a implantação

Antes da ativação foi criado o backup privado `.private-backups/postgres-before-exclusao-corretora-20260908.dump`, com 23.576 bytes e índice validado de 74 linhas.

SHA-256: `E215127D4D367EBFF5304BEB192B84447E9AE9C7FB4277AA96E2D3E3AFF940FF`.

As assinaturas antes e depois da implantação são idênticas: permaneceram 5 transações, 6 corretoras, 4 ativos, 9 consultas e 13 usuários. Nenhum registro real foi excluído na implantação. A remoção só ocorrerá após confirmação do usuário na interface.

Backend, frontend e PostgreSQL terminaram saudáveis. Nenhum commit, push ou publicação externa foi realizado.

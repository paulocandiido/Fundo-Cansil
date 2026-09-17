# Corretoras sem restrição de mercado — entrega de 08/09/2026

> Registro histórico da primeira entrega do dia. A regra de preservação descrita abaixo foi substituída posteriormente: a versão atual exclui definitivamente a corretora e suas transações, e registra automaticamente a data/hora das novas operações.

## Comportamento entregue

- Cadastro por CNPJ e razão social verificada, sem escolha de mercado. O campo legado `mercado` é aceito por compatibilidade, mas não autoriza nem impede operações.
- Uma mesma corretora pode receber ativos em moedas distintas. O CNPJ continua único por conta; o isolamento entre usuários foi preservado.
- Remover uma corretora exige confirmação na interface e autorização de propriedade no backend. A remoção é lógica, com resposta 204, sem apagar posições ou transações.
- Corretoras removidas não aceitam novas compras. A venda continua limitada ao saldo existente, inclusive no seletor e nos atalhos da carteira. Cadastrar novamente o mesmo CNPJ revalida a instituição e restaura o mesmo identificador.
- A moeda ISO 4217 pertence ao ativo e aparece nas respostas e telas. O usuário seleciona BR ou USA na operação, e o servidor define respectivamente BRL ou USD. Valores antigos permanecem BRL. Divergências entre ativo e mercado são rejeitadas; não há conversão cambial ou totalização entre moedas diferentes.
- Símbolos admitem 1–20 caracteres, incluindo ponto e hífen após o primeiro caractere alfanumérico. Um código fora do catálogo pode ser informado, mas precisa ser reconhecido pela fonte antes da criação de um novo ativo/operação.

## Limite de cobertura

A remoção da regra BR/USA não habilita novos provedores. O catálogo brapi e os adapters configurados mantêm sua cobertura existente. O fluxo USD foi verificado com respostas controladas, não com cotações internacionais reais. A integração atual da brapi consulta [ações e ativos da B3](https://brapi.dev/docs/acoes). A disponibilidade por símbolo e plano continua sendo externa. Nenhuma chave foi alterada e nenhuma ordem real é executada.

## Verificação automatizada

- Backend: Docker executou `mvn -q verify`: **189 testes encontrados, 188 executados, 1 condicional ignorado, zero falhas e zero erros**.
- Angular: **67 testes aprovados em 7 arquivos**, incluindo cadastro sem mercado, remoção confirmada, prevenção de repetição, resposta incerta e venda de saldo em corretora removida.
- Cliente TypeScript de referência: **8 testes aprovados**, incluindo DELETE com resposta 204 sem JSON.
- Build `production,docker`: **403,47 kB** iniciais, transferência estimada **103,89 kB**; bundle `main-LNITPXWC.js`.
- O teste de integração `CorretoraLivreIntegrationTest` usa a mesma corretora para BRL/USD, verifica isolamento, remoção repetida, histórico/saldo preservados, venda, restauração do mesmo ID e rejeição de moeda incompatível. Usa banco isolado e provedores simulados.
- Relatórios e JAR foram exportados para `.private-backups/build-corretoras-livres-20260907/`. Não publicar essa pasta privada.

## Aplicação no Docker e preservação

Em 08/09/2026, os serviços estavam parados. Foi iniciado somente o PostgreSQL existente para criar o backup. Depois foram atualizados backend/frontend, sem excluir ou trocar o volume `cansil_postgres_data` e sem modificar o PostgreSQL nativo do Windows.

Backup privado: `.private-backups/postgres-before-corretoras-livres-20260908.dump`, **23.292 bytes**. O índice foi validado com `pg_restore --list` (74 linhas). Não foi realizado ensaio de restauração completo.

SHA-256: `F1E26646FFE48E8C8DD3066AF75DF24365C7F7AA0C5EFB58F990367702FEF1D5`.

A migração aditiva 006 acrescenta `corretora.removida=false` e `ativo.moeda=BRL`. As assinaturas dos dados anteriores, calculadas com `scripts/Database-Fingerprint-Business.sql`, são iguais antes/depois em todas as tabelas de negócio. A comparação exclui apenas as duas colunas novas e os metadados Liquibase; sequências também permaneceram iguais.

Contagens preservadas: 4 ativos, 8 consultas, 6 corretoras, **5 transações**, 13 usuários, 2 grupos e 4 produtos. Nenhuma conta, operação, consulta ou remoção de corretora foi criada no banco real durante a verificação.

Os três serviços terminaram saudáveis: backend 8080, frontend 4200 e PostgreSQL 5433 no Windows. HTTP do site e `/healthz` responderam 200; `/api/corretoras` via proxy respondeu 401 sem token, conforme esperado. O bundle servido é o compilado nesta entrega. Healthchecks não comprovam disponibilidade de provedores externos.

## Uso

Abra `http://127.0.0.1:4200`, atualize a página e entre novamente. Em **Corretoras**, use **Remover** e confirme. Para voltar a comprar pela mesma instituição, cadastre novamente seu CNPJ. Para vender saldo antigo, use o atalho **Vender** da carteira ou selecione **Venda** nas operações.

Não foram criados commits, pushes ou publicação na internet. Os registros OpenSpec anteriores permanecem históricos e não foram reclassificados como verificação desta nova regra.

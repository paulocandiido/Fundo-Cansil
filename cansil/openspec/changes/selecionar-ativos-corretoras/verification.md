# Verificação — seleção de ativos e corretoras

Data da verificação: 06/09/2026.

## Escopo e referências

- A mudança principal foi aplicada no escopo OpenSpec do backend `cansil`.
- Como o frontend irmão não pertence ao `allowedEditRoots` do backend, foi criado e validado um escopo OpenSpec local em `../frontend/openspec`, sem mover projetos nem contornar o limite de edição.
- Os códigos estáveis `XP`, `INTER` e `NAO_INFORMADA` foram documentados. A identificação comercial foi conferida nos sites oficiais da [XP](https://www.xpi.com.br/) e do [Inter DTVM](https://inter.co/inter-dtvm/). A categoria `NAO_INFORMADA` é exclusivamente técnica e não atribui corretora real às operações antigas.
- O contrato da cotação e do catálogo foi comparado com a [documentação oficial da brapi](https://brapi.dev/docs). Nenhuma chave real foi copiada para código, documentação ou frontend.

## Testes automatizados

- Maven: 171 testes encontrados, 0 falhas e 0 erros; 170 executados com sucesso e 1 teste PostgreSQL condicional ignorado na suíte H2 padrão.
- O teste condicional de upgrade da migration 004 e o teste de duas vendas concorrentes foram executados separadamente contra PostgreSQL 17 descartável e passaram.
- Angular: 54 testes em 6 arquivos, todos aprovados. A cobertura inclui seleção explícita, controles nativos focalizáveis, atalho de venda sem autoridade nem preço automático, flags de corretora, fallback para ativos próprios, seis colunas e atualização explícita de cotação.
- Build Angular de produção concluído: 387,73 kB brutos e estimativa de transferência de 100,83 kB.
- Cliente TypeScript de referência: 6 testes em Node, todos aprovados.
- Os testes usam respostas simuladas para fornecedores externos; eles não comprovam cota, plano ou disponibilidade de credenciais reais.

## Migração e Docker

- O upgrade foi exercitado primeiro em H2 e depois em PostgreSQL 17 descartável com dados fictícios anteriores à migration. Identificadores, valores, datas, saldos e sequência foram preservados; os vínculos antigos receberam somente `NAO_INFORMADA`.
- O contêiner e o volume anônimo usados no teste descartável foram removidos ao final; continham somente dados fictícios.
- Antes da atualização local, foi criado `.private-backups/postgres-before-004-20260906.dump`, com 18.870 bytes e SHA-256 `5D10647359087000790F6BC63CC5399B8BFCBFC70ADCF87D7972074D0C9DAEDA`. A listagem com `pg_restore` foi validada.
- O backup está em pasta ignorada pelo Git e usa as permissões herdadas do usuário/OneDrive. O endurecimento explícito de ACL do Windows não foi aplicado, portanto não se afirma proteção adicional de ACL.
- A aplicação Docker local foi reconstruída preservando o volume principal. Após a inicialização, PostgreSQL ficou saudável, a migration 004 possuía 2 changesets registrados, havia 3 corretoras, nenhuma transação com vínculo nulo e 3 transações legadas vinculadas à categoria técnica.
- Uma requisição sem JWT a `GET /api/corretoras` retornou 401, confirmando a proteção do endpoint sem imprimir credenciais.

## Revisão e limitações

- `git diff --check` do backend não encontrou erro de whitespace. O frontend separado não possui raiz Git local própria; sua revisão foi feita por inspeção de arquivos, testes e build, sem incluir arquivos do perfil do usuário no escopo.
- Os dois escopos OpenSpec passaram em `validate --strict`.
- Não houve commit, push, publicação externa, ordem de negociação, integração fiscal, transferência de custódia ou alteração destrutiva do volume principal.
- Não foi executado teste visual/interativo em navegador real. O servidor Angular foi compilado, respondeu localmente e permanece disponível para validação manual em `http://127.0.0.1:4200/`.
- A cotação atual depende da brapi no modo local adotado. Não é promessa de tempo real, e falha completa da fonte continua retornando 503 sem apresentar resultado parcial como completo.

# Verificação — Fundo Cansil no Docker

Data: 07/09/2026. Implementação e atualização local concluídas, sem commit, push, arquivamento da mudança ou publicação na internet.

## Organização entregue

- Projeto Compose `fundo-cansil`; serviços e contêineres `backend`, `frontend` e `postgres`.
- Imagens `backend:local`, `frontend:local` e `postgres:17-alpine`; os três ficaram saudáveis.
- Frontend na porta 4200, API na 8080, PostgreSQL Docker na 5433 do Windows (5432 interna). O PostgreSQL do Windows na 5432 não foi interrompido.
- Site, cabeçalho/FC, rodapé, login, favicon e títulos usam Fundo Cansil. Maven/Spring e artefatos de entrega usam o identificador `fundo-cansil`.
- Pastas, pacotes Java e histórico Liquibase não foram renomeados. Volume físico externo `cansil_postgres_data` preservado explicitamente; sua ausência causa erro, não criação silenciosa de outro banco.
- Segredos continuam no backend. Apenas `POSTGRES_PORT` foi alterado de 5432 para 5433 no `.env` real; nenhuma credencial foi substituída.

## Builds e testes

| Verificação | Resultado |
| --- | --- |
| `docker compose config --quiet` | Aprovado sem avisos |
| `docker compose build backend` | Aprovado; executou `mvn -q verify` no Linux antes de gerar a imagem |
| Relatórios Java exportados do estágio de build | 185 testes encontrados, 184 executados, 1 condicional ignorado; 0 falhas/erros |
| `docker compose build frontend` | Aprovado; build `production,docker`, 401,26 kB iniciais e transferência estimada 103,60 kB |
| Testes Angular | 64 aprovados em 7 arquivos; inclui marca/títulos e login/bearer same-origin |
| Builds Angular locais | Produção padrão 401,29 kB; desenvolvimento 1,80 MB; configuração Docker também aprovada |
| Cliente TypeScript de referência | 7 testes aprovados |
| `nginx -t` no frontend | Sintaxe e configuração aprovadas |
| Pacote de verificação sem JAR | `Prepare-Delivery.ps1` gerou ZIP com nome novo, 174 arquivos e verificações internas de exclusão aprovadas; não é entrega completa do frontend |
| OpenSpec estrito | Validado nas duas raízes locais |
| `git diff --check` backend | Sem erros de whitespace; avisos de conversão LF/CRLF |

O comando local `mvnw.cmd -q clean verify` falhou ao limpar `target/classes` por bloqueio de arquivos no OneDrive. Não foram apagadas pastas à força nem alteradas permissões. A verificação efetiva foi feita dentro do Docker; a etapa `verification-artifacts` permite exportar JAR/relatórios sem depender do build local. Os testes Java usam H2 e fontes controladas; o teste PostgreSQL condicional não foi habilitado nesta etapa, que não introduz migração funcional.

## Backup e preservação

O banco antigo estava parado. Um contêiner temporário, sem rede, abriu exclusivamente o volume para gerar o backup; foi parado/removido antes de iniciar o novo PostgreSQL. Nunca foram executados dois PostgreSQL simultâneos sobre esse volume.

- Backup novo: `.private-backups/postgres-before-fundo-cansil-20260907.dump`, 23.200 bytes.
- SHA-256: `98BD7A40AA5A87A359DBA393ADDD8EE82F5713A2FB64C33888C06D3B304C1875`.
- `pg_restore --list` validou a listagem do dump (74 linhas); não foi feito ensaio completo de restauração deste backup.
- A pasta de backup é ignorada no Git e no contexto Docker. O dump contém dados locais e não deve ser compartilhado; não foi alterada sua política de permissões/criptografia.
- `scripts/Database-Fingerprint.sql` foi executado em transação somente leitura antes e depois da troca. Assinaturas são agregados de todas as colunas, incluindo vínculos, não exposição dos registros. MD5 aqui é comparação operacional, não mecanismo de segurança.

| Tabela | Linhas antes/depois | Assinatura igual antes/depois |
| --- | --- | --- |
| ativo | 4 | `820ffad150faaf00863acb899a95b982` |
| catalogo_ativo_lock | 1 | `f3e56c602771e9541aef61d502562b89` |
| consulta | 8 | `d3d8e20d001dd7bcdce585281979e974` |
| corretora | 5 | `caa4234ffcb011acec215bcdc687cc80` |
| databasechangelog | 9 | `57afa9c2489fd08291031dbae5db7aa0` |
| databasechangeloglock | 1 | `6c0af3df169765ea321b53b8a14f1221` |
| grupoproduto | 2 | `29ff41228ce10bec3f8d862c70fb849f` |
| produto | 4 | `2d0cdc669eb644ad2d0ac7baeca68179` |
| transacao | 4 | `b8fb04a92fe19ab3c154e260e64f6d52` |
| usuario | 13 | `55f9625e3ecee6b456b0ef16c547a79d` |

Sequências preservadas: ativo 4, consulta 40, corretora 11, grupoproduto 2, produto 4, transacao 4 e usuario 45. Nenhuma conta/operação/cotação foi criada no banco principal nesta etapa.

Após confirmar os três serviços e os dados, foram removidos apenas os contêineres antigos parados `cansil-aplicacao-1`, `cansil-postgres-1` e a rede vazia `cansil_default`. Nenhum volume foi removido; contêineres são recriáveis a partir da configuração/imagens. O nome técnico antigo do volume foi conservado de propósito.

## HTTP, interface e reinício independente

- GET `/`, `/login` e `/corretoras`: 200 HTML com título Fundo Cansil. Favicon e arquivos JS/CSS: 200.
- Arquivo JS inexistente e `/.env`: 404, sem fallback SPA de sucesso.
- GET `/api/usuarios/me` anônimo: 401 JSON pela origem do frontend. Token falso continua rejeitado com 401 nas origens localhost/127.0.0.1:4200.
- POST `/api/auth/login` com objeto vazio: 400 JSON, sem criação de conta ou sessão. Origin externa não permitida: 403.
- Bundle `main-PMASNZPE.js` sem URL `http://localhost:8080` nem nome interno `backend:8080`; o cliente usa `/api` no build Docker. Configuração local permanece disponível para npm.
- Navegador real: a abertura de `/` direcionou para `/login`; título `Entrar | Fundo Cansil`, cabeçalho, mensagem de boas-vindas e rodapé confirmados pela árvore de acessibilidade e screenshot. Não houve login real nem operações pela interface.
- `docker compose up -d --no-deps --force-recreate backend`: backend mudou de contêiner e ficou saudável; frontend manteve seu identificador e continuou respondendo 401 via proxy. Nesta execução o Docker reutilizou o IP do backend. Resolução dinâmica para mudanças de IP foi revisada na configuração Nginx, mas não foi forçada uma troca de IP nesse teste.

## Limitações e referências

Healthcheck do frontend verifica Nginx; o do backend verifica resposta HTTP/Security, não disponibilidade dos provedores. Portas continuam publicadas em todas as interfaces; HTTPS, restrições de produção, limites e legado público seguem as ressalvas de `ENTREGA.md`. Nome de contêiner fixo atende ao pedido local e impede múltiplas réplicas com o mesmo nome. Operações USA continuam bloqueadas; nenhuma fonte financeira foi alterada.

As decisões foram conferidas em documentação oficial: [projetos Compose](https://docs.docker.com/compose/how-tos/project-name/), [volumes externos](https://docs.docker.com/reference/compose-file/volumes/), [rede Compose](https://docs.docker.com/compose/how-tos/networking/), [ambientes Angular](https://angular.dev/tools/cli/environments), [proxy Nginx](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass) e [resolução de upstream Nginx](https://nginx.org/en/docs/http/ngx_http_upstream_module.html#server).

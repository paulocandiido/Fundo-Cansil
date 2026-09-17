# Fundo Cansil — Frontend Angular

Entrega de 08/09/2026: corretoras sem restrição de mercado, moeda derivada automaticamente do mercado e horário automático nas operações. Excluir uma corretora também exclui permanentemente suas compras, vendas e posições. Docker atualizado; 69 testes Angular aprovados.

Implementado em Angular 22 com componentes standalone, formulários reativos, rotas e HttpClient. Interface em português, adaptável a telas pequenas, com navegação por teclado, rótulos de campos e mensagens de carregamento/erro.

## Executar tudo no Docker

O nome público do sistema é **Fundo Cansil**. No Docker Desktop, o grupo é `fundo-cansil`, com três contêineres independentes: `backend`, `frontend` e `postgres`. Os nomes das pastas existentes não mudaram; o Compose e o `.env` permanecem em `GestaoAcoes/cansil`.

Na pasta `GestaoAcoes/cansil`, depois da configuração/migração descrita no guia do backend:

```powershell
docker compose up -d --build
docker compose ps
```

Abra `http://127.0.0.1:4200`. O Nginx serve o Angular compilado e encaminha `/api` para `backend:8080` pela rede interna; o navegador não precisa conhecer esse nome interno. O PostgreSQL usa 5432 internamente e, por padrão, 5433 no Windows para não conflitar com o PostgreSQL instalado na máquina. A API continua acessível diretamente em `http://127.0.0.1:8080`.

O frontend não tem hot reload no Docker. Depois de alterar a interface:

```powershell
docker compose up -d --build frontend
```

Para reiniciar somente um serviço, use `docker compose restart frontend` (ou `backend`/`postgres`). Reiniciar o banco interrompe temporariamente as requisições; os dados permanecem no volume. Nunca use `docker compose down -v` nem apague o volume histórico `cansil_postgres_data`: seu nome físico foi preservado de propósito, não é uma marca exibida no site. Uma instalação nova precisa configurar/criar seu próprio volume conforme o guia do backend.

As portas podem ser configuradas no Compose. A porta 4200 só pode ser ocupada por um frontend de cada vez; para alternar entre Docker e desenvolvimento, siga abaixo. Se mudar a origem/porta do site, ajuste também as origens CORS permitidas do backend.

## Desenvolver com npm start

Use este modo para atualizar a interface automaticamente enquanto edita o código. Primeiro, na pasta `GestaoAcoes/cansil`, libere a porta usada pelo frontend Docker e mantenha apenas a API e o banco:

```powershell
docker compose stop frontend
docker compose up -d backend postgres
```

Use Node 24.19 ou compatível com Angular 22. Na pasta `GestaoAcoes/frontend`:

```powershell
npm.cmd ci --no-audit --no-fund
npm.cmd start
```

As dependências já foram instaladas nesta máquina; para uso diário basta `npm.cmd start`. Abra `http://127.0.0.1:4200`. Para encerrar, Ctrl+C no terminal. Para voltar ao Docker, encerre o `npm start` antes de executar `docker compose up -d --build frontend` na pasta do Compose. Se ocorrer bloqueio de leitura do OneDrive no ambiente restrito do agente, executar o comando no PowerShell normal resolve a restrição observada, sem alterar permissões da pasta.

## Fluxo disponível

1. Abra `/cadastro`, preencha nome, e-mail, CPF, senha e confirmação.
2. Após cadastrar, entre em `/login` com a conta criada.
3. `/carteira` abre na visão por corretora com Ativo, Corretora, Quantidade, Valor investido, Preço médio e Cotação atual; permite alternar para a visão consolidada.
4. `/corretoras` consulta CNPJ e exibe razão social não editável, sem escolha de mercado; permite remover corretoras mediante confirmação.
5. `/operacoes` seleciona mercado, corretora e ativo. O mercado BR define BRL e o mercado USA define USD automaticamente; a corretora pode operar nos dois.
6. `/cotacoes` busca por código exato do ativo, como PETR4, e mostra fonte e histórico das consultas. Não há pesquisa por nome de empresa/autocomplete no contrato atual.
7. `/carteira` também oferece resumo, atualização manual de cotações, filtros combinados, consulta de operação por ID e extrato com preço médio e lucro/prejuízo. A antiga aba `/avaliacao` foi removida.
8. “Atualizar cotações” na carteira consulta a avaliação somente mediante clique e aplica o preço por símbolo a todas as corretoras. “Sair” encerra a sessão local.

Uma conta nova tem carteira vazia. Não são inseridas posições fictícias. As operações são registros do usuário: o sistema não envia ordens para corretoras. A brapi é a única fonte ativa por enquanto; Alpha/HG permanecem desativadas. Cotações/avaliação e o primeiro registro de um ativo ainda desconhecido pelo backend dependem de acesso ao provedor. Carteira e históricos existentes continuam acessíveis sem cotação.

### Compras, vendas e precisão

Use quantidade e preço positivos, com até 13 dígitos inteiros e 6 casas decimais. Pode usar vírgula ou ponto decimal, mas não separador de milhar. Os valores são normalizados para strings com ponto e enviados sem conversão para `Number`. Datas são enviadas como `LocalDateTime`, sem `Z`, offset ou conversão UTC. A exibição do histórico também preserva a data/hora recebida.

Venda acima do saldo da corretora escolhida retorna 422; não é possível vender em uma instituição usando o saldo mantido em outra. Os campos são mantidos para correção. O backend verifica a ordem histórica local e consolidada, inclusive operações retroativas. Após sucesso, o formulário é limpo e o histórico é atualizado separadamente: uma falha na leitura do histórico não transforma uma gravação confirmada em falha de envio.

Na tela “Corretoras”, consulte um CNPJ real e confira a razão social. O backend reconsulta a BrasilAPI ao salvar, sem confiar em nome livre. Não há vinculação nem bloqueio BR/USA por corretora. O mesmo CNPJ identifica uma única instituição por conta, utilizável em todos os mercados. O campo legado `mercado` permanece somente por compatibilidade, é opcional na entrada e não tem efeito nas operações. A consulta cadastral não comprova autorização regulatória.

A liberdade de selecionar corretoras não cria cobertura de dados. O catálogo e a integração brapi atual cobrem B3; os adapters Alpha/HG existentes continuam com sua cobertura configurada. Nenhum novo provedor foi ativado. É possível informar um código fora do catálogo, mas um novo ativo exige resposta válida da fonte; caso contrário, o registro falha sem criar a operação. Não há execução de ordens reais. Corretoras inativas ou legadas não aceitam novas compras.

Os vínculos anteriores mantêm o histórico. Use “Regularizar” para confirmar o CNPJ da instituição antiga; o ID é preservado. Não deduza o CNPJ de uma corretora não informada. Antes da regularização, vendas de saldo próprio continuam permitidas.

`DELETE /api/corretoras/{id}` exige autenticação e propriedade. A exclusão é definitiva: remove todas as compras e vendas vinculadas e depois a corretora. As posições correspondentes desaparecem porque são calculadas dessas operações. Cotações e ativos do catálogo não são apagados, pois não pertencem à corretora. A interface exige confirmação explícita.

A compra/venda não recebe data ou hora do usuário. O backend registra automaticamente o momento da confirmação no horário de Brasília (`America/Sao_Paulo`) e devolve `dataOperacao` para o histórico.

Ativos, cotações, consultas, operações, posições e avaliações informam `moeda` ISO 4217 nas respostas. Os registros anteriores permanecem BRL. No POST de transação, `mercado` é obrigatório (`BR` ou `USA`); o backend deriva BRL ou USD e não aceita escolha manual de moeda. Divergências entre o ativo e o mercado são rejeitadas (422), sem conversão cambial ou soma entre moedas. A moeda do ativo permanece fixa. Símbolos aceitam 1–20 caracteres, iniciados por letra/número, seguidos de letras, números, ponto ou hífen.

Se alterar o CNPJ, a consulta anterior é invalidada. Durante o cadastro, os controles e a navegação ficam bloqueados para evitar repetição; em resposta incerta, atualize a lista e confira o resultado antes de reenviar. Falha na fonte cadastral não inventa uma razão social nem grava cadastro parcial.

“Valor investido” significa custo da posição restante, não soma de aportes históricos. A visão por corretora usa bases gerenciais separadas; a consolidada usa a sequência histórica global e pode produzir preço médio/custo diferente da simples soma das linhas locais. Esses valores não constituem cálculo fiscal.

O botão, a navegação interna e a saída da conta ficam bloqueados durante o POST; não existe repetição automática. Se a resposta da gravação for incerta (rede, timeout ou erro de servidor), o usuário precisa atualizar o histórico e confirmar que a operação não está nele antes de liberar outro envio. Isso reduz duplicação nesta tela, mas **não substitui idempotência no backend**: recarregar, sair da tela após o término da requisição ou usar várias abas pode contornar o estado local. Confira o histórico antes de repetir qualquer envio sem confirmação. A API não oferece edição/exclusão de operações.

## Configuração pública da API

`src/environments/environment.ts` define `apiUrl: 'http://localhost:8080'` para `npm start`, testes e build padrão. O backend permite CORS para `http://127.0.0.1:4200` e `http://localhost:4200`. Se mudar a porta direta do backend para desenvolvimento, ajuste essa URL pública.

O Dockerfile executa `npm run build -- --configuration production,docker`. A configuração `docker` substitui esse arquivo por `environment.docker.ts`, com `apiUrl: ''`, e as chamadas ficam relativas à origem do site (`/api/...`). Não use esse build com um servidor estático sem o proxy de API. O build padrão continua disponível sem a substituição.

O Nginx usa a porta interna 8080 sem usuário root. Resolve o backend novamente pelo DNS do Docker, conserva prefixo `/api`, autenticação, método e corpo; não repete requisições nem armazena respostas da API em cache. Rotas Angular podem ser abertas diretamente; arquivos ausentes e erros da API não recebem `index.html` com sucesso falso. O HTML principal não é armazenado em cache e arquivos ocultos são bloqueados. `/healthz` comprova somente o servidor estático, não a disponibilidade do banco ou dos provedores.

Nunca copie o `.env` do backend para cá. Senha do PostgreSQL, JWT_SECRET e chaves brapi/Alpha/HG não pertencem ao frontend. Nenhuma chave de provedor foi adicionada.

Rotas consumidas: POST `/api/auth/cadastro`, POST `/api/auth/login`, GET `/api/usuarios/me`, GET/POST `/api/corretoras`, DELETE `/api/corretoras/{id}`, GET `/api/corretoras/cnpj/{cnpj}`, GET `/api/ativos`, GET `/api/carteira`, GET `/api/carteira/corretoras`, POST/GET `/api/transacoes`, GET `/api/cotacoes/{simbolo}`, GET `/api/cotacoes/historico` e GET `/api/carteira/avaliacao`. Esta última alimenta o botão “Atualizar cotações” dentro da carteira; não existe mais uma página separada de avaliação.

## Segurança e limites

- JWT e identidade somente em memória. Recarregar a página exige novo login; não há refresh token.
- Rotas Angular organizam a navegação; autorização e isolamento reais continuam no Spring Security.
- 401 em chamada protegida limpa a sessão e retorna ao login. 503 não apaga a sessão.
- Em compras/vendas, se o histórico responder 401 durante um registro, o retorno ao login aguarda o POST terminar, sem cancelá-lo ou repeti-lo. Uma resposta antiga do histórico também trata expiração, mas não elimina uma nova sessão com outro token.
- Requisições não usam cookies cross-origin nem repetição automática. Cadastro duplicado retorna mensagem de conflito. Cotações e avaliação não são atualizadas automaticamente, para evitar consumo contínuo das cotas externas.
- Senhas respeitam o limite de 72 bytes UTF-8 do BCrypt. CPF segue a validação de formato do backend, não dos dígitos verificadores.
- Erros externos não são inseridos como HTML nem reproduzidos integralmente. O usuário recebe mensagens locais por status.
- A carteira exibe os valores recebidos, sem recalcular preço médio/custo. Cotações anteriores são limpas antes de uma nova tentativa e as posições permanecem em falha. O horário exibido é do recebimento local, não horário de mercado ou garantia de tempo real.
- Publicação na internet não foi realizada. Exige HTTPS, URL pública de API, CORS restrito à origem publicada, revisão do legado público e demais cuidados descritos na entrega do backend.

## Organização

- `src/app/core`: contratos, cliente HTTP, sessão, proteção de rotas e validadores.
- `src/app/pages`: login, cadastro, corretoras, carteira, operações, cotações e avaliação.
- `src/app/app.routes.ts`: rotas e títulos das páginas.
- `src/theme.css` e `src/operacoes.css`: cores, tipografia, formulários, tabelas e layout responsivo.
- `src/environments/environment.ts`: URL pública do backend.
- `src/environments/environment.docker.ts`: conexão same-origin usada apenas no build Docker.
- `Dockerfile`, `.dockerignore` e `nginx/`: compilação em duas etapas, exclusões de arquivos locais e servidor/proxy de execução.

A pasta `frontend` é irmã de `cansil`. O versionamento do projeto completo fica na raiz `GestaoAcoes`, incluindo frontend, backend e documentação, no repositório [Fundo-Cansil](https://github.com/paulocandiido/Fundo-Cansil), branch `main`.

## Verificação

Em **07/09/2026**, a mudança de marca/Docker passou nos **64 testes Angular** em 7 arquivos. Foram compilados os modos `production,docker` (401,26 kB iniciais), produção padrão (401,29 kB) e desenvolvimento (1,80 MB). O artefato Docker contém a marca Fundo Cansil e não contém a URL local `localhost:8080` nem o nome interno `backend:8080`; os outros dois builds preservam a API local. Os testes novos verificam marca, títulos, login e autenticação com URL same-origin. A validação OpenSpec estrita também passou. Configuração Nginx e integração dos contêineres devem ser conferidas nas evidências desta mudança: `openspec/changes/docker-fundo-cansil/verification.md`.

Em 06/09/2026: **62 testes passaram** em 7 arquivos; o build de produção foi concluído sem erros (401,38 kB iniciais, estimativa de transferência 103,51 kB). Os testes específicos cobrem cadastro por CNPJ, razão social da consulta, respostas antigas, regularização BR, cadastro USA com aviso e bloqueio de operação USA, falhas externas, envio duplicado e resultado incerto. As regressões cobrem catálogo e bearer, invalidação de seleção, controles nativos focalizáveis por teclado, atalho de venda sem autoridade nem preço automático, fallback para ativos próprios, corretoras permitidas, seis colunas, duas instituições para um símbolo, consulta explícita única e limpeza de preços em falha. Essas verificações usam respostas simuladas e não constituem teste em navegador real. Evidências: `openspec/changes/cadastrar-corretoras-cnpj-mercado/verification.md`.

```powershell
npm test -- --watch=false
npm run build
```

Os testes usam HttpTestingController e dados falsos; não acessam provedores nem alteram o PostgreSQL. Cobrem login, bearer, limpeza/preservação de sessão, validação, cadastro sem repetição, rotas protegidas, carteira vazia, formatação pt-BR e expiração. Também verificam precisão dos valores enviados, datas sem conversão UTC, compras/vendas, bloqueio durante envio, recuperação de resultado incerto, ordenação do histórico, busca explícita de cotação e avaliação sem atualização automática.

A navegação assistida WebMCP é opcional e só existe em navegadores com suporte. A ferramenta `abrir_carteira` exige sessão prévia, não aceita identificadores nem credenciais e apenas navega. O contrato foi testado com contexto simulado; não foi validado em uma implementação real de WebMCP.

Cinco testes de regressão cobrem expiração concorrente ao registro, resposta antiga de histórico, preservação de novo token, compra confirmada após expiração e redirecionamento com RouterTestingHarness. Dois deles reproduziram as falhas antes da correção. O teste do roteador usa HTTP simulado e não constitui teste em navegador real.

Em 07/09/2026, a tela de entrada foi conferida em navegador real, com título, cabeçalho e marca Fundo Cansil, sem entrar em conta nem registrar dados. Essa inspeção não equivale a um teste autenticado completo. Validação manual recomendada: usar uma conta de desenvolvimento para conferir cadastro, login, compra, venda, histórico e carteira; consultar cotação e avaliação considerando as cotas do provedor; conferir saída da conta, recarregamento e telas pequenas. Os registros desse teste manual serão persistidos e a API atual não permite editá-los ou excluí-los.

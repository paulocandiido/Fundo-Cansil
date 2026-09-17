# Fundo Cansil — integração com o frontend

Este repositório contém o backend. O frontend Angular na pasta irmã `../frontend` inclui cadastro, login, corretoras por CNPJ, carteira, compras/vendas, histórico e cotações. A avaliação separada foi incorporada à carteira por meio do botão “Atualizar cotações”.

## 1. Ambiente local

O site se chama **Fundo Cansil**. O grupo Docker é `fundo-cansil`, com contêineres separados `backend`, `frontend` e `postgres`. A pasta do backend é `cansil`. Consulte [DOCKER.md](DOCKER.md) antes da primeira inicialização ou da migração do grupo antigo: o volume externo deve existir e não pode ser usado por dois PostgreSQL ao mesmo tempo.

Com o volume/configuração já preparados e as pastas irmãs `cansil` e `frontend` disponíveis, mantenha o Docker Desktop aberto e execute na pasta `cansil`:

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

O primeiro comando não imprime nada quando a configuração é válida. Não compartilhe `docker compose config` sem `--quiet`, pois ele pode exibir segredos. Não remova volumes do banco; o volume externo usa, por padrão, o nome físico `cansil_postgres_data` para preservar os dados existentes.

- Site: `http://127.0.0.1:4200` ou `http://localhost:4200` (se `FRONTEND_PORT` não tiver sido alterada).
- API direta: `http://localhost:8080` (se `APP_PORT` não tiver sido alterada). No frontend Docker, o navegador usa `/api` na mesma origem do site; o Nginx encaminha para `backend:8080`.
- PostgreSQL: porta **5433 no Windows**, encaminhada à 5432 no contêiner. O backend acessa `postgres:5432` na rede Docker; o navegador nunca acessa o banco. O PostgreSQL do Windows na 5432 não precisa ser interrompido.
- O front está na pasta irmã `GestaoAcoes/frontend`. Não coloque os arquivos dele em `src/main/java`.

Se escolher outra origem, inclusive mudando `FRONTEND_PORT`, adicione a origem exata a `CORS_ALLOWED_ORIGINS` no `.env` do backend e execute `docker compose up -d backend frontend`. A lista substitui as origens padrão; use vírgulas para mais de uma, sem barra final, sem caminho e sem `*`. Por exemplo, para a porta 4300: `http://localhost:4300,http://127.0.0.1:4300`. Alterações em Java exigem `docker compose up -d --build backend`; alterações no Angular exigem `docker compose up -d --build frontend`.

Para desenvolver com atualização automática do Angular, pare somente o frontend Docker e mantenha os outros serviços:

```powershell
# Na pasta cansil:
docker compose stop frontend
docker compose up -d backend postgres
Set-Location ..\frontend
npm.cmd start
```

Não execute `npm.cmd start` e o frontend Docker simultaneamente na porta 4200. O modo local do Angular continua acessando a API na 8080; não use os nomes internos `backend` ou `postgres` como endereços do navegador. Para voltar ao site Docker, encerre o Angular com `Ctrl+C`, retorne à pasta `cansil` e execute `docker compose up -d --build frontend`.

No front, configure apenas a URL pública da API. **Não copie o `.env` do backend:** JWT_SECRET, senhas do banco e chaves brapi/Alpha/HG nunca podem ir para o navegador. O JWT recebido no login é diferente de JWT_SECRET.

## 2. Ordem sugerida das telas

1. Cadastro e login.
2. Cabeçalho com nome do usuário obtido de `/api/usuarios/me` e botão sair.
3. Cadastro de corretora: consultar CNPJ e conferir razão social, sem escolher mercado.
4. Carteira e histórico de transações (funcionam sem cotação externa).
5. Formulário de compra/venda com identificação da moeda, sem restrição por mercado da corretora.
6. Consulta de cotação, histórico de consultas e atualização de valores diretamente na carteira.

Cadastre um usuário próprio de desenvolvimento. Não use contas fictícias deixadas pelos testes anteriores. Cadastro não faz login automaticamente.

## 3. Contratos da API

Todas as rotas abaixo usam JSON. Envie `Content-Type: application/json` nos POSTs. Exceto cadastro/login, envie `Authorization: Bearer <token>`.

| Método e rota | Resposta de sucesso |
| --- | --- |
| POST `/api/auth/cadastro` | 201, `{id,nome,email}` |
| POST `/api/auth/login` | 200, `{token,tipo,expiraEmSegundos}` |
| GET `/api/usuarios/me` | 200, `{id,nome,email}` |
| GET `/api/corretoras` | 200, lista própria não removida `{id,codigo,nome,ativa,legada,cnpj,mercado,verificadoEm,removida}` ordenada por nome |
| GET `/api/corretoras/cnpj/{cnpj}` | 200, `{cnpj,razaoSocial,situacaoCadastral,ativa,consultadoEm}`; envie CNPJ normalizado sem máscara |
| POST `/api/corretoras` | 201, cadastro ou regularização; entrada `{cnpj,corretoraLegadaId?}` |
| DELETE `/api/corretoras/{id}` | 204, exclui definitivamente a corretora própria e suas transações |
| GET `/api/ativos?busca=PETR&pagina=0&tamanho=20` | 200, `{itens,pagina,tamanho,total,atualizadoEm,desatualizado}` |
| GET `/api/carteira` | 200, lista `{simbolo,quantidade,precoMedio,custoTotal}` |
| GET `/api/carteira/corretoras` | 200, lista `{simbolo,corretora,quantidade,precoMedio,custoTotal}` |
| POST `/api/transacoes` | Envie `{mercado,simbolo,corretoraId,tipo,quantidade,valorUnitario}`; retorna 201 com moeda e horário calculados |
| GET `/api/transacoes` | 200, lista de transações, data/id crescentes |
| GET `/api/cotacoes/{simbolo}` | 200, `{simbolo,nome,bolsa,preco,fonte}`; grava consulta |
| GET `/api/cotacoes/historico` | 200, lista `{id,simbolo,valor,fonte,consultadoEm}`, data/id decrescentes |
| GET `/api/carteira/avaliacao` | 200, lista `{simbolo,quantidade,precoMedio,cotacaoAtual,fonte,custoTotal,valorAtual,lucroPrejuizo,lucroPrejuizoPercentual}` |

Listas vazias retornam `[]`, não 404. Não há paginação nem edição/exclusão de transações. Nunca envie `usuarioId`: a identidade vem do JWT. A carteira lista somente posições abertas. As rotas legadas de produtos não fazem parte do fluxo financeiro.

Cadastro: `{nome,email,cpf,senha}`; nome até 120 caracteres, email até 160, CPF de 11 dígitos ou formato `000.000.000-00`. O formato do CPF é validado, não os dígitos verificadores. Senha com pelo menos 8 caracteres e no máximo 72 bytes UTF-8 (acentos podem ocupar mais de um byte).

Corretora: a BrasilAPI é consultada novamente no POST; não envie nome livre. Não há vinculação nem bloqueio BR/USA por corretora. O mesmo CNPJ identifica uma única instituição por conta, utilizável em todos os mercados. O campo legado `mercado` permanece somente por compatibilidade, é opcional na entrada e não tem efeito nas operações. Consulta cadastral confirma existência, não autorização regulatória.

Regularização: envie o ID da corretora legada em `corretoraLegadaId` e seu CNPJ confirmado. `DELETE /api/corretoras/{id}` exige autenticação e propriedade e exclui definitivamente suas compras, vendas e posições derivadas. A interface exige confirmação. Cotações e ativos independentes não são apagados.

Ativos, cotações, consultas, operações, posições e avaliações informam `moeda` ISO 4217 nas respostas. Os registros anteriores permanecem BRL. No POST de transação, `mercado` é obrigatório: `BR` define BRL e `USA` define USD. O usuário não escolhe moeda. O backend rejeita divergências entre ativo e mercado (422), não converte preços nem soma moedas diferentes. A moeda do ativo permanece fixa. Símbolos aceitam 1–20 caracteres, iniciados por letra/número, seguidos de letras, números, ponto ou hífen.

A liberdade de selecionar corretoras não cria cobertura de dados. O catálogo e a integração brapi atual cobrem B3; os adapters Alpha/HG existentes continuam com sua cobertura configurada. Nenhum novo provedor foi ativado. É possível informar um código fora do catálogo, mas um novo ativo exige resposta válida da fonte; caso contrário, o registro falha sem criar a operação. Não há execução de ordens reais.

Transação: `{mercado,simbolo,corretoraId,tipo,quantidade,valorUnitario}`. Não envie moeda nem data/hora: o backend deriva a moeda do mercado e registra automaticamente a confirmação no horário de Brasília. Use `COMPRA` ou `VENDA`, quantidade/preço positivos com até 13 dígitos inteiros e 6 decimais. Strings decimais com ponto, como `"20.50"`, são aceitas.

O fluxo recomendado é cadastrar corretora/CNPJ → selecionar mercado → corretora → ativo → quantidade → valor realmente negociado. Pesquise explicitamente o catálogo, mas mantenha os ativos das posições próprias como alternativa quando a fonte estiver indisponível. A carteira consolida posições, resumo e extrato; filtros de tipo/ativo e consulta local por ID usam o histórico autenticado já carregado. Atualize preços somente por botão, usando uma chamada de avaliação e mapeando por símbolo. O horário exibido é da consulta local, não horário de mercado nem promessa de tempo real.

`dataOperacao` existe somente na resposta e no histórico. É gerada pelo backend no fuso `America/Sao_Paulo`; o usuário não a edita. Valores de resposta são números JSON; deixe os cálculos financeiros oficiais no backend.

Fontes possíveis: `BRAPI`, `ALPHA_VANTAGE`, `HG_FINANCE`. A API pode não informar nome/bolsa; a interface deve tolerar valores nulos. Cotação não tem garantia de tempo real nem horário de mercado no contrato atual.

## 4. Cliente e exemplos prontos

Copie `docs/frontend/api-client.ts` para o futuro projeto TypeScript. Ele é independente de React, Angular ou Vue. Os tipos refletem o contrato, mas não fazem validação de schema em execução.

```typescript
import { ApiClient, ApiError } from './api-client';
const api = new ApiClient('http://localhost:8080'); // desenvolvimento local; use '' no site com proxy /api

try {
  await api.login({ email, senha }); // valores do formulário; não registre o retorno em logs
  const usuario = await api.me();
  const posicoes = await api.carteira();
  // Atualize o estado da interface com usuario e posicoes.
} catch (erro) {
  if (erro instanceof ApiError) {
    // Exiba erro.message como texto, nunca como HTML.
    // erro.status === 401: solicite login; 503: cotação temporariamente indisponível.
  }
}
```

JWT fica apenas em memória. Ao recarregar a página será necessário entrar novamente; não há refresh token. `api.logout()` remove o token local; ele não revoga tokens já emitidos no servidor. Evite localStorage e cookies improvisados. Desabilite botões durante envio de login e transações e não repita POST automaticamente após falha de rede: confira o histórico antes de reenviar para evitar duplicação.

`docs/frontend/requisicoes.http` contém exemplos copiáveis para um cliente HTTP. Os dados são falsos; use cópia privada se inserir credenciais reais. A compra de exemplo grava no banco.

Os testes do cliente podem ser executados na raiz do backend com Node 24 ou superior, sem instalar dependências:

```powershell
node --test docs/frontend/api-client.test.mjs
```

São testes de execução com respostas simuladas, não uma compilação estática pelo TypeScript. Ao copiar o cliente para o front, inclua-o na checagem de tipos do framework escolhido.

## 5. Estados e erros que as telas devem tratar

Formato habitual: `{timeStamp,status,error,message,path}`. `timeStamp` é epoch em milissegundos. Use o status para decidir a ação; não dependa do texto exato.

| Status | Tratamento |
| --- | --- |
| 400 | Corrigir campos ou JSON e manter formulário preenchido, sem registrar senha |
| 401 | Login inválido ou sessão ausente/expirada; pedir autenticação |
| 403 | Acesso negado; se o navegador bloquear a resposta, conferir CORS |
| 404 | CNPJ não encontrado na fonte cadastral ou recurso inexistente |
| 409 | Email/CPF já cadastrado ou CNPJ já vinculado à conta |
| 422 | Regra de negócio, por exemplo venda acima do saldo |
| 503 | Cotação ou consulta cadastral indisponível, conforme a rota; manter carteira e histórico acessíveis |
| Erro de rede | Verificar API/Docker/CORS; não assumir que uma transação falhou antes de consultar histórico |

Inclua estados de carregamento, lista vazia, erro e sucesso. Rejeição CORS e respostas de infraestrutura podem não conter JSON; o cliente inclui mensagem alternativa.

No cadastro de corretora, invalide a consulta quando o CNPJ mudar e não repita POST automaticamente. Em resultado incerto, atualize a lista própria antes de reenviar. BrasilAPI não exige token nessa integração; suas falhas devem impedir novos cadastros, sem inventar nomes nem afetar o histórico. A validação local aceita CNPJ numérico e alfanumérico; o suporte efetivo de consulta depende da fonte.

## 6. Limitações conhecidas

### Configuração temporária: somente brapi

Alpha Vantage e HG Finance estão desativadas por padrão no perfil dev e no Compose. Nenhuma chamada é feita a elas, mesmo que as chaves antigas continuem no `.env`. Não foi adicionada a Twelve Data. Se a brapi falhar, a API retorna 503; não há uma segunda fonte ativa neste modo.

Para reativar futuramente uma fonte existente, depois de resolver acesso/cota, configure no `.env` do backend:

```dotenv
ALPHAVANTAGE_ENABLED=true
HGFINANCE_ENABLED=true
```

Habilite somente a fonte desejada e forneça seu token correspondente. Execute `docker compose up -d backend` para recriar a aplicação com as variáveis atualizadas. Deixar `false` dispensa o token da respectiva fonte. Não precisa apagar as chaves antigas; elas não são usadas enquanto a fonte estiver desativada.

Adicionar um provedor diferente exige implementação e testes no backend, não apenas uma nova chave. Mantendo o contrato REST, o front não precisa ser refeito. Preserve a exibição dinâmica do campo `fonte` e o tratamento de 503. O histórico antigo pode continuar contendo Alpha/HG.

A brapi respondeu nos testes reais anteriores. Alpha Vantage apresentou limite de consultas e HG Finance restrição de plano; sucesso de fallback real ainda não foi comprovado. Isso não impede construir cadastro, login, operações e carteira. Teste visualmente o estado 503 e não faça consultas automáticas frequentes às fontes. A atualização de cotações é completa ou retorna erro, sem total parcial apresentado como completo.

O ambiente está destinado a desenvolvimento local. HTTPS, política das rotas legadas, restrição de portas, backups, limites de requisições e auditoria de produção ainda exigem preparação separada. Consulte [ENTREGA.md](ENTREGA.md) e o registro da mudança Docker em `openspec/changes/docker-fundo-cansil/verification.md` para o alcance da verificação atual. Os registros das evoluções anteriores permanecem nas respectivas pastas OpenSpec, sem reclassificá-los como testes da configuração atual.

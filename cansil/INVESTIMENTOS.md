# Fundo Cansil — backend de investimentos

Para começar a interface, leia [FRONTEND.md](FRONTEND.md): contratos da API, configuração CORS, cliente TypeScript e exemplos HTTP.

Java 17, Spring Boot, Liquibase, PostgreSQL e testes isolados com H2. O frontend Angular fica em `../frontend`. O produto se chama Fundo Cansil; a pasta é `cansil` e o pacote principal é `com.curso.cansil`. Os IDs de migrations são mantidos por compatibilidade. O domínio legado de produtos permanece disponível. Não há execução de ordens reais.

## Executar testes sem Docker

No diretório do projeto, execute `./mvnw test` (Windows: `.\mvnw.cmd test`). Para recompilar do zero, use `./mvnw clean test`.

Os testes ativam o perfil `test`, aplicam Liquibase e validam o schema com Hibernate. As integrações externas usam respostas simuladas; não são necessários tokens reais nem PostgreSQL. Os valores fictícios de teste nunca devem ser usados em produção.

## Executar em desenvolvimento

Configure as variáveis documentadas em `.env.example` no ambiente do processo. O Spring Boot não carrega `.env` automaticamente: esse arquivo é consumido pelo Docker Compose. Para execução local, exporte as variáveis antes de usar `./mvnw spring-boot:run`.

Se o Java rodar no Windows usando o PostgreSQL Docker, a URL padrão correspondente é `jdbc:postgresql://localhost:5433/cursodb` (ajuste porta e nome do banco aos valores configurados). Dentro do Compose, o backend usa `postgres:5432`; essa URL interna não deve ser usada pelo Java executado fora da rede Docker. Um PostgreSQL próprio do Windows pode continuar na porta 5432.

São obrigatórios `DB_PASSWORD`, `BRAPI_TOKEN` e `JWT_SECRET` no modo atual. `ALPHAVANTAGE_TOKEN` e `HGFINANCE_TOKEN` são necessários somente se suas fontes forem habilitadas. Use segredo JWT aleatório de pelo menos 32 bytes; não compartilhe nem versione credenciais. A expiração padrão é 3.600 segundos. As URLs podem ser sobrescritas pelas variáveis `*_BASE_URL` para ambientes controlados.

## API

Cadastro e login são públicos; as demais rotas abaixo exigem `Authorization: Bearer <JWT>`.

| Método | Rota | Finalidade |
| --- | --- | --- |
| POST | `/api/auth/cadastro` | Cadastro: nome, email, cpf e senha |
| POST | `/api/auth/login` | Login: email e senha; retorna JWT |
| GET | `/api/cotacoes/{simbolo}` | Consulta cotação e registra fonte no histórico |
| GET | `/api/cotacoes/historico` | Histórico do usuário, mais recente primeiro |
| GET | `/api/corretoras` | Corretoras não removidas do usuário, com CNPJ e flags ativa/legada/removida |
| GET | `/api/corretoras/cnpj/{cnpj}` | Consulta cadastral via BrasilAPI; CNPJ normalizado sem máscara |
| POST | `/api/corretoras` | Cadastro/regularização: CNPJ e corretoraLegadaId opcional |
| DELETE | `/api/corretoras/{id}` | Exclusão definitiva da corretora própria e de todas as suas transações |
| GET | `/api/ativos?busca=PETR&pagina=0&tamanho=20` | Catálogo paginado de símbolos da brapi |
| POST | `/api/transacoes` | Registra compra ou venda |
| GET | `/api/transacoes` | Operações do usuário em ordem cronológica |
| GET | `/api/carteira` | Posições consolidadas, preço médio e custo restante |
| GET | `/api/carteira/corretoras` | Posições gerenciais separadas por corretora |
| GET | `/api/carteira/avaliacao` | Custo, valor atual, fonte e lucro/prejuízo |

Exemplo de operação:

```json
{
  "simbolo": "PETR4",
  "corretoraId": 1,
  "tipo": "COMPRA",
  "quantidade": 10,
  "valorUnitario": 20.00
}
```

O usuário sempre vem do JWT e o identificador da corretora vem de `GET /api/corretoras`; não fixe IDs na interface. Quantidade e valor precisam ser positivos, com até seis casas decimais. A venda consome somente o saldo da corretora escolhida e precisa manter válida a sequência consolidada. Uma venda mantém a média das unidades restantes; ao zerar, a posição deixa de aparecer. A data/hora é criada pelo servidor no momento do registro. O lock do usuário serializa lançamentos e exclusões concorrentes desse usuário.

As operações anteriores à migration 004 recebem exclusivamente a categoria técnica `NAO_INFORMADA`, sem tentativa de adivinhar a instituição. Ela não aceita novas compras, mas pode ser selecionada para vender saldo legado. O `custoTotal` por corretora é gerencial; na visão consolidada, o preço médio segue toda a sequência histórica do ativo e pode diferir da soma das bases locais. Nenhum desses valores é apuração fiscal.

## Cadastro de corretoras e mercado

A migration `005-cadastro-corretoras-cnpj.xml` separa vínculos antigos por usuário sem alterar IDs, valores ou datas das operações. Esses vínculos ficam pendentes de regularização, com CNPJ desconhecido e mercado BR histórico. Os antigos registros globais não são oferecidos para novas compras. A regularização explícita mantém o ID do vínculo, confirma o CNPJ com a fonte e substitui o nome antigo pela razão social obtida.

O backend valida o CNPJ e reconsulta a BrasilAPI no cadastro; nome livre não é utilizado. Persiste a identificação, situação e instante da consulta, sem sócios/endereço. Não há vinculação nem bloqueio BR/USA por corretora. O mesmo CNPJ identifica uma única instituição por conta, utilizável em todos os mercados. O campo legado `mercado` permanece somente por compatibilidade, é opcional na entrada e não tem efeito nas operações.

Cadastro de instituição inativa é permitido com aviso, mas novas compras exigem instituição ativa e verificada. Venda de saldo BR legado/inativo permanece permitida. Verificação cadastral não comprova autorização para intermediação financeira. BrasilAPI é a fonte de CNPJ; brapi continua sendo a fonte de ativos/cotações, não de corretoras.

A liberdade de selecionar corretoras não cria cobertura de dados. O catálogo e a integração brapi atual cobrem B3; os adapters Alpha/HG existentes continuam com sua cobertura configurada. Nenhum novo provedor foi ativado. É possível informar um código fora do catálogo, mas um novo ativo exige resposta válida da fonte; caso contrário, o registro falha sem criar a operação. Não há execução de ordens reais. Falha cadastral retorna 503; CNPJ não encontrado, 404; duplicado ativo na conta, 409.

`DELETE /api/corretoras/{id}` exige autenticação e propriedade. Dentro da mesma transação de banco, apaga todas as compras/vendas vinculadas e a corretora. As posições são recalculadas sem esses registros. Cotações e ativos permanecem independentes. O frontend exige confirmação e informa que a ação é permanente.

O cliente não envia `dataOperacao`. O backend usa o fuso `America/Sao_Paulo` e grava automaticamente o momento da confirmação. A resposta continua trazendo `dataOperacao` para o histórico.

Ativos, cotações, consultas, operações, posições e avaliações informam `moeda` ISO 4217 nas respostas. Os registros anteriores permanecem BRL. No POST de transação, `mercado` é obrigatório: `BR` define BRL e `USA` define USD. O usuário não escolhe moeda. O backend rejeita divergências entre ativo e mercado (422), não converte preços nem soma moedas diferentes. A moeda do ativo permanece fixa. Símbolos aceitam 1–20 caracteres, iniciados por letra/número, seguidos de letras, números, ponto ou hífen.

A migração aditiva `006-corretoras-sem-restricao.xml` inicializa `removida=false` e `moeda=BRL`. Não altera valores, datas ou vínculos das transações.

## Cotações e falhas

A ordem entre fontes habilitadas é brapi → Alpha Vantage → HG Finance. Temporariamente, por autorização do usuário, dev/Compose usam somente brapi: `ALPHAVANTAGE_ENABLED=false` e `HGFINANCE_ENABLED=false` por padrão. Tokens dos fallbacks são opcionais enquanto desativados. O perfil de testes mantém os três habilitados. Consulte `FRONTEND.md` para reativação; nenhum endpoint ou DTO foi alterado.

Cada provider possui contrato tipado e prioridade própria. A brapi usa bearer, extrai `results[0].data` e confere `results[0].symbol` antes de atribuir o preço ao código solicitado; Alpha Vantage resolve o símbolo BRL antes de consultar o preço; HG Finance lê os resultados indexados por símbolo. O cliente tem limites de conexão de 3 segundos e leitura de 5 segundos por requisição.

O catálogo público de ativos usa cache em memória por seis horas e não grava cotações nem operações. Se a atualização falhar, a cópia vencida continua disponível marcada com `desatualizado=true`; sem cópia válida, a API responde 503. A lista não garante que o plano de cotação contratado permita consultar todos os símbolos.

Erros HTTP, timeout, resposta inválida ou sem preço fazem avançar para a próxima fonte. A falha total retorna HTTP 503 com fontes/categorias seguras, sem registrar consulta bem-sucedida. Logs não incluem corpo HTTP, URL com chave ou tokens. Disponibilidade dos ativos, permissões e limites dependem do plano contratado em cada fonte; os testes simulados não comprovam acesso com credenciais reais.

Outros retornos: 400 para entrada inválida, 401 para autenticação inválida, 409 para cadastro duplicado e 422 para venda sem saldo. CPF e hash não são retornados pelos DTOs.

## Docker — execução e limites externos

Siga [DOCKER.md](DOCKER.md) para preparar o volume externo e migrar com segurança. O grupo `fundo-cansil` reúne `backend`, `frontend` e `postgres`; comandos antigos com o serviço `aplicacao` passam a usar `backend`. O Compose permanece na pasta `cansil` e precisa da pasta irmã `../frontend` para compilar o site. As imagens de aplicação são `backend:local` e `frontend:local`.

Com a configuração e o volume prontos, execute `docker compose config --quiet`, seguido de `docker compose up -d --build`. Não compartilhe a saída de `docker compose config` sem `--quiet`: ela pode revelar credenciais. Preserve o `.env` existente; a mudança de nome não exige substituir suas chaves ou senhas.

O site usa a porta 4200, a API direta a 8080 e o banco a 5433 no host, mantendo 5432 dentro da rede Docker. O Nginx do frontend encaminha `/api` ao backend sem expor o hostname interno ao navegador. Cada serviço pode ser reiniciado separadamente; alterações de código exigem rebuild da respectiva imagem. O volume externo mantém o nome físico anterior `cansil_postgres_data`, selecionável por `POSTGRES_VOLUME_NAME`. Se o volume não existir, a inicialização deve falhar em vez de criar silenciosamente um banco vazio. Nenhum volume deve ser removido para validar esta mudança.

Para manter a atualização automática do Angular, use `docker compose stop frontend` e `docker compose up -d backend postgres`; depois execute `npm.cmd start` na pasta `../frontend`. Não ocupe a porta 4200 simultaneamente com as duas opções. A verificação atual de imagens, proxy e preservação de dados é registrada em `openspec/changes/docker-fundo-cansil/verification.md`.

Registro histórico de 02/09/2026: PostgreSQL saudável, cadastro/login, proteção 401, consulta brapi e persistência do histórico. Em instância temporária com brapi indisponível, a Alpha Vantage respondeu limite de consultas e a HG Finance respondeu restrição de plano. A API retornou 503 sem gravar histórico. Esses testes pertencem à configuração anterior; o sucesso dos fallbacks reais continua dependendo da disponibilidade/cota e das permissões das contas externas.

O endpoint de ações da HG Finance exige Member Premium ou superior, conforme https://console.hgbrasil.com/documentation/finance. Uma chave válida não garante acesso a esse endpoint. Não é possível contornar a restrição pelo código. Nenhuma assinatura foi contratada.

A Alpha Vantage usa duas requisições por tentativa de cotação (busca do símbolo e consulta do preço). Respostas HTTP 200 com `Information`/`Note` de limite agora são classificadas como `LIMITE`, sem expor o texto externo. A HG Finance trata `valid_key`, `results.error` e `results.message` como campos tipados, distinguindo `AUTENTICACAO` e `PLANO` de JSON inválido. Evite repetir testes enquanto a cota não estiver disponível.

## Versionamento sugerido — não executado

Cada grupo pode receber um commit e uma tag próprios, após revisão e autorização:

1. `feat: modelagem e migrations de investimentos` — `investimentos-01-modelo`
2. `feat: provider tipado brapi` — `investimentos-02-brapi`
3. `feat: fallback alpha vantage e hg finance` — `investimentos-03-fallback`
4. `feat: transacoes e calculo de carteira` — `investimentos-04-carteira`
5. `feat: endpoints e historico de consultas` — `investimentos-05-api`
6. `feat: autenticacao jwt e bcrypt` — `investimentos-06-seguranca`
7. `test: regras e providers` — `investimentos-07-unitarios`
8. `test: api com h2 e liquibase` — `investimentos-08-integracao`
9. `chore: configuracao e documentacao docker` — `investimentos-09-runtime`

Não foram feitos commits, tags ou push.

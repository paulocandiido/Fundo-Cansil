# Fundo Cansil — execução no Docker

O Docker Desktop agrupa o ambiente como `fundo-cansil`. Os serviços são independentes e mantêm os nomes simples pedidos:

| Serviço / contêiner | Imagem | Porta no Windows | Destino interno |
| --- | --- | --- | --- |
| `frontend` | `frontend:local` | `4200` (`FRONTEND_PORT`) | Nginx na `8080` |
| `backend` | `backend:local` | `8080` (`APP_PORT`) | API na `8080` |
| `postgres` | `postgres:17-alpine` | `5433` (`POSTGRES_PORT`) | PostgreSQL na `5432` |

O site se chama **Fundo Cansil**. A pasta do backend é `cansil`, o pacote principal é `com.curso.cansil` e a classe de inicialização é `CansilApplication`. O volume ativo é `cansil_postgres_data`, migrado com backup e verificação. Os IDs das migrations permanecem intactos.

## 1. Antes de iniciar

Mantenha o Docker Desktop aberto e as duas pastas lado a lado:

```text
GestaoAcoes/
  cansil/     # backend, compose.yaml e .env privado
  frontend/          # Angular, Dockerfile e configuração Nginx
```

Execute os comandos Compose dentro de `cansil`. O build completo usa `../frontend`; copiar somente o backend não fornece o site. O `.env` real fica apenas no backend e nunca deve ser copiado para o frontend, incluído em imagens, anexos ou commits. Preserve suas chaves e senhas atuais.

Os nomes dos contêineres são globais no Docker. Antes de subir, confira `docker ps -a`: se já existir um `backend`, `frontend` ou `postgres` de outro projeto, não o remova. Resolva o conflito de nomes antes de continuar. Não use `-p cansil` nem um `COMPOSE_PROJECT_NAME` antigo para iniciar o novo agrupamento.

### Banco existente: preservar o volume

O Compose declara um volume **externo**, com nome físico explícito. Por padrão ele reutiliza `cansil_postgres_data`. Se esse volume não existir, a inicialização falha; isso evita apresentar um banco novo e vazio como se fosse o banco antigo.

Antes de migrar um ambiente antigo, confirme o nome do volume montado no PostgreSQL antigo, gere e valide um backup e registre contagens/assinaturas dos dados. Pare o PostgreSQL antigo antes de iniciar o novo. **Nunca inicie dois PostgreSQL usando simultaneamente o mesmo volume.** Compare os dados depois da troca de grupo e só remova os contêineres antigos após a conferência, sem remover volumes. Se houver falha, pare o novo banco antes de retomar o antigo com o mesmo volume.

Não execute comandos de remoção de volumes, limpeza geral do Docker ou `docker compose down -v` como parte da migração. O volume externo da configuração atual não é gerenciado pelo `down`, mas versões antigas da configuração podem declarar volumes internos removíveis. Renomear o grupo não exige apagar, copiar ou reinicializar o banco.

### Instalação nova, sem dados anteriores

Esta seção é somente para uma instalação nova. Não a use para corrigir o desaparecimento de um volume que deveria conter seus dados.

1. Crie um `.env` a partir de `.env.example` somente se ele ainda não existir e configure credenciais privadas válidas.
2. Escolha um nome de volume novo, confirme que não pertence a outro ambiente e crie-o explicitamente. Exemplo: `docker volume create fundo-cansil-dados-novo`.
3. No `.env`, defina exatamente `POSTGRES_VOLUME_NAME=fundo-cansil-dados-novo` para selecionar esse volume. O nome físico padrão antigo não muda sozinho.
4. Inicie o ambiente pelos comandos abaixo. O PostgreSQL inicializará o volume vazio e o backend aplicará as migrations.

Mudar `POSTGRES_PASSWORD` no `.env` não redefine automaticamente a senha de um banco já inicializado. A troca do nome do produto não exige mudar credenciais nem o nome do banco.

## 2. Iniciar os três contêineres

Depois de preparar o volume e garantir que o PostgreSQL antigo está parado:

```powershell
Set-Location C:\Users\prcan\OneDrive\Desktop\6s\GestaoAcoes\cansil
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

`config --quiet` não imprime nada quando a configuração é válida. Não compartilhe a saída de `docker compose config` sem `--quiet`, pois ela pode revelar segredos. Se a configuração emitir avisos sobre variáveis, corrija-os antes de iniciar.

Abra [Fundo Cansil](http://127.0.0.1:4200). O frontend Nginx serve os arquivos compilados do Angular e encaminha `/api` para `backend:8080` na rede Docker, preservando o prefixo e a autenticação. O navegador usa a mesma origem do site; ele não precisa resolver o nome interno `backend`. A API continua acessível diretamente em `http://127.0.0.1:8080`.

O backend acessa o banco pelo endereço interno `postgres:5432`. Ferramentas no Windows acessam `localhost:5433`, usando o usuário, senha e banco configurados no `.env`. A porta 5433 evita conflito com o PostgreSQL do Windows na 5432; não é necessário interromper esse serviço.

Healthchecks verificam banco, API e servidor estático. Um contêiner saudável não garante que a brapi ou a BrasilAPI estejam disponíveis, nem que uma conta externa tenha cota. O primeiro build pode demorar para baixar imagens e dependências.

## 3. Atualizar ou reiniciar separadamente

Execute na pasta `cansil`:

```powershell
# Alterações no código Java:
docker compose up -d --build backend

# Alterações no código Angular:
docker compose up -d --build frontend

# Reiniciar um serviço sem recompilar nem trocar a configuração:
docker compose restart backend

# Ler os últimos logs de um serviço:
docker compose logs --tail=100 backend

# Parar o ambiente sem apagar dados:
docker compose stop
```

Troque `backend` por `frontend` ou `postgres` quando a ação se destinar ao outro serviço. Reiniciar ou recriar o backend causa indisponibilidade breve da API; o frontend pode continuar exibindo o site. O Nginx volta a resolver o nome do backend após sua recriação.

Depois de alterar variáveis do `.env`, use `docker compose up -d backend` para recriar a API com os novos valores. `restart` sozinho não aplica novas variáveis. Se a alteração envolver portas de outros serviços, inclua também esses serviços no `up`. Não altere senha/banco/volume para resolver uma falha de conexão sem conferir a configuração existente.

O serviço antigo `aplicacao` foi substituído por `backend`: comandos operacionais atuais devem usar o nome novo. Documentos de aulas e verificações anteriores podem conter o nome antigo por serem históricos.

## 4. Desenvolver o frontend com npm

A imagem Docker contém um build estático, sem atualização automática ao salvar fontes. Para trabalhar no Angular com atualização automática:

```powershell
# Na pasta cansil:
docker compose stop frontend
docker compose up -d backend postgres
Set-Location ..\frontend
npm.cmd start
```

Se for a primeira instalação local de dependências do frontend, execute `npm.cmd ci` antes de `npm.cmd start`. O frontend local continua em `http://127.0.0.1:4200` e acessa diretamente a API na 8080, conforme a configuração de desenvolvimento em `../frontend/INTEGRACAO.md`.

**Não execute npm e o frontend Docker simultaneamente na mesma porta.** Para voltar ao Docker, encerre `npm.cmd start` com `Ctrl+C` e execute:

```powershell
Set-Location ..\cansil
docker compose up -d --build frontend
```

## 5. Portas e segurança local

`FRONTEND_PORT`, `APP_PORT` e `POSTGRES_PORT` alteram somente as portas publicadas no host; a comunicação Docker continua usando `backend:8080` e `postgres:5432`. Caso o `.env` antigo contenha `POSTGRES_PORT=5432`, ajuste esse campo para `5433` neste ambiente, preservando os segredos.

Ao mudar `FRONTEND_PORT`, inclua a origem exata em `CORS_ALLOWED_ORIGINS`, inclusive no modo com proxy. Por exemplo, para 4300: `http://localhost:4300,http://127.0.0.1:4300`. A lista substitui as origens anteriores; mantenha também as origens de desenvolvimento necessárias. Não use `*`, caminho ou barra final. Recrie `backend` e `frontend` com `docker compose up -d backend frontend`.

Se mudar `APP_PORT`, o proxy do frontend Docker continua funcionando pela porta interna 8080; já o Angular executado por npm precisa ter a URL de desenvolvimento da API ajustada. Não coloque chaves de provedores, senha do banco ou `JWT_SECRET` na configuração Angular.

As portas são publicadas em todas as interfaces por padrão. Este ambiente é local, não uma publicação de produção. Antes de expô-lo a outras máquinas ou à internet, revise endereços de publicação, firewall, HTTPS, autenticação do legado, limites de requisições e política de backups. Não envie logs sem conferir se contêm dados privados.

## 6. Entrega e evidências

`scripts/Prepare-Delivery.ps1` entrega somente o backend em `dist/fundo-cansil-backend-candidato-*.zip`. O JAR produzido pelo Maven é `target/fundo-cansil-0.0.1-SNAPSHOT.jar`; quando incluído no ZIP, fica em `bin/fundo-cansil.jar`. Veja [ENTREGA.md](ENTREGA.md).

O ZIP não inclui o projeto Angular. Para o Compose completo, disponibilize separadamente a pasta irmã `frontend` com Dockerfile, configuração Nginx e fontes. Para API e banco apenas, use explicitamente `docker compose up -d --build backend postgres`, após preparar o volume externo e as credenciais.

O Dockerfile do backend executa `mvn verify` durante a construção. Se o OneDrive impedir a limpeza/compilação local, é possível exportar JAR e relatórios do build Linux sem alterar permissões ou apagar pastas à força:

```powershell
docker build --target verification-artifacts --output type=local,dest=dist/verificacao-docker .
```

Essa etapa opcional não substitui a imagem de execução e não contém credenciais reais. O JAR fica na raiz da pasta de saída e os relatórios em `surefire-reports`. Não confunda o ZIP somente do backend com um pacote completo do site.

As verificações efetivamente executadas nesta mudança, incluindo backup, preservação de dados, healthchecks, rotas Angular, proxy e recriação isolada do backend, são registradas em `openspec/changes/docker-fundo-cansil/verification.md`. Este guia descreve o procedimento e não substitui essas evidências. Os resultados das etapas anteriores permanecem nas respectivas pastas OpenSpec com suas datas originais.

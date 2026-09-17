## Context

Ver proposal.md. O Compose atual possui aplicação e PostgreSQL parados; o frontend usa ng serve. O volume real é `cansil_postgres_data`. A porta 5432 está ocupada por PostgreSQL do Windows, que não será interrompido.

## Goals / Non-Goals

**Goals:** executar o site/API/banco separadamente e preservar persistência, autenticação e desenvolvimento local.

**Non-Goals:** renomear diretórios/pacotes Java/IDs Liquibase, trocar credenciais, publicar na internet, modificar regras financeiras ou habilitar USA.

## Decisions

- Compose permanece na pasta backend existente para manter .env/caminhos. Projeto `fundo-cansil`, serviços e container_name `backend`, `frontend`, `postgres`. Imagens de aplicação `backend:local` e `frontend:local` sem prefixo Cansil.
- Volume PostgreSQL externo com nome físico explícito do volume atual; não criar volume vazio por mudança de project. Alternativa de copiar para nome novo foi descartada pelo risco desnecessário. Novas instalações exigem criar/selecionar seu próprio volume antes de subir.
- Frontend multi-stage Node + Nginx, artefatos estáticos sem dependências/segredos de desenvolvimento na imagem final. Build Docker usa base de API vazia, preservando o /api já montado pelo cliente; ng serve continua com localhost:8080.
- Nginx encaminha /api sem retirar o prefixo, preserva Authorization/Host (incluindo porta), sem cache/retry de POST, e resolve DNS Docker novamente após recriar backend. Rotas Angular usam index.html; arquivos ausentes e /api não viram HTML de sucesso.
- Portas locais: frontend 4200 e backend 8080; PostgreSQL 5433 no host e 5432 interno. Não parar PostgreSQL do Windows. Documentar exclusão mútua entre frontend Docker e npm start na mesma porta; permitir configurar portas.
- Nome Fundo Cansil no cabeçalho, rodapé, login, títulos de rota e index; Maven/Spring e pacote de entrega adotam slug fundo-cansil. Arquivos históricos e caminhos reais preservados e explicados no guia.
- Healthchecks verificam PostgreSQL pronto, API HTTP e frontend estático; não alegar disponibilidade de provedores financeiros.

## Risks / Trade-offs

- Nomes de contêiner globais → conferir conflitos; não remover contêiner alheio.
- Troca de agrupamento → backup novo, assinatura dos registros antes/depois, parar banco antigo antes de iniciar o novo e nunca montar o volume em dois PostgreSQL simultâneos.
- Volume com nome histórico → preservado explicitamente e explicado; não é nome de produto na interface.
- Proxy/Nginx → testar 401/400 JSON, Origin local, assets e links profundos; reconferir após recriação do backend.
- Imagem compilada não tem hot reload → documentar rebuild ou usar npm start com frontend Docker parado.

## Migration Plan

Conferir estado, preparar backup novo do banco com contêiner temporário sem porta publicada se necessário, validar o dump e registrar assinatura. Construir/testar imagens antes de remover contêineres antigos. Subir novo projeto com volume explícito, verificar integridade e comunicação, remover somente contêineres antigos sem volumes após sucesso. Em falha, parar o novo banco e retomar o antigo com o mesmo volume (sem acesso concorrente). Atualizar guias e evidências; sem commit/push.


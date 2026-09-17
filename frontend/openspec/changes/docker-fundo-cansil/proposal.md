## Why

O usuário quer executar a aplicação inteira no Docker, identificada como Fundo Cansil, com backend, frontend e PostgreSQL separados. A mudança deve conservar o banco existente e o fluxo de desenvolvimento Angular.

## What Changes

- Projeto Compose `fundo-cansil`, com contêineres `backend`, `frontend` e `postgres`; substitui o serviço `aplicacao` por `backend` (**BREAKING** para comandos antigos).
- Frontend compilado e servido por Nginx, com proxy same-origin para a API e suporte às rotas Angular.
- Nome público Fundo Cansil no site, títulos e metadados de aplicação/artefatos; identificadores técnicos usam `fundo-cansil`.
- Migração operacional protegida por backup e vínculo explícito ao volume atual. Pastas, pacotes Java e IDs de migrações permanecem para compatibilidade; documentos históricos não serão reescritos.

## Capabilities

### New Capabilities
- `deployment/container-runtime`: execução em três contêineres identificados, identidade pública e preservação do banco na mudança.

### Modified Capabilities
Nenhuma regra financeira ou contrato REST é alterado.

## Impact

Compose/Dockerfiles, proxy e build Angular, identificação visível, metadados Maven/Spring e documentação operacional. Não envolve mudança de credenciais, compra de serviços, publicação, alteração de transações nem renomeação de pastas. Backend e frontend têm raízes OpenSpec próprias e serão verificados separadamente.


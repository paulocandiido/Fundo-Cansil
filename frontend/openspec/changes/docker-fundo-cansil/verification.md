# Verificação — Fundo Cansil no frontend Docker

Data: 07/09/2026.

- Nome público Fundo Cansil aplicado a cabeçalho/FC, rodapé, login, favicon, título principal e títulos de rotas.
- Dockerfile Node 24 + Nginx 1.28 Alpine, execução não-root em 8080 interna, API same-origin, DNS Docker dinâmico, sem retry/cache de API e fallback SPA separado de arquivos.
- 64 testes Angular aprovados em 7 arquivos. Builds: produção padrão 401,29 kB, desenvolvimento 1,80 MB, production,docker 401,26 kB (103,60 kB de transferência estimada).
- `docker compose build frontend` e `nginx -t` aprovados. Bundle entregue sem localhost:8080/backend:8080; ng serve mantém a configuração local de API.
- Frontend saudável no grupo fundo-cansil, porta 4200. GET de site/login/corretoras/assets passou; arquivo ausente e .env retornam404; API sem token retorna401JSON, login vazio400JSON e Origin externa403.
- Navegador real confirmou título Entrar | Fundo Cansil e marca na tela de entrada, sem login ou criação de dados.
- Recriação isolada do backend concluída: o frontend manteve seu contêiner e a API voltou a responder por ele. O IP do backend foi reutilizado nesse teste; o suporte a IP novo está configurado/revisado, mas não foi forçada mudança de IP.
- OpenSpec validado com --strict. Sem commit, push, publicação ou arquivamento.

A conferência completa do banco, backup, build Java e limpeza dos contêineres antigos está em `../../../../cansil/openspec/changes/docker-fundo-cansil/verification.md`, a partir desta pasta. Volume, registros e sequências foram preservados. O guia `INTEGRACAO.md` explica a alternância entre frontend Docker e npm start na mesma porta.

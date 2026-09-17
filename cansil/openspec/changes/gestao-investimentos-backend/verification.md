# Verificação em 02/09/2026

- `mvnw.cmd -q clean test`: código de saída 0; 124 testes, zero erros, zero falhas, zero ignorados.
- Executado na cópia atualizada `C:\Users\prcan\AppData\Local\Temp\cansil-clean-verify`, devido à restrição de exclusão de arquivos gerados no OneDrive. Nenhuma ACL foi alterada.
- H2 em modo PostgreSQL; Liquibase aplicado e Hibernate em `validate`.
- Cobertura inclui legado, cadastro/JWT, DTOs, constraints, isolamento, compras/vendas, operação retroativa, cálculo/avaliação, contratos HTTP e cadeia de fallback pela API.
- Provedores simulados: nenhum token real ou chamada às APIs externas foi necessário.
- `openspec validate gestao-investimentos-backend --strict`: válido.
- `git diff --check`: sem erros de whitespace; somente avisos de conversão LF/CRLF do Git no Windows.
- 33/34 tarefas concluídas. Compose validado com `.env.example` e configuração real sem imprimir segredos. A tarefa 9.3 permanece parcial: Docker/PostgreSQL e brapi passaram, mas o sucesso de fallback real depende das contas externas.
- Instruções de uso e nove grupos sugeridos de commits/tags estão em `INVESTIMENTOS.md`. Nenhum commit, tag ou push executado.

## Correção do diagnóstico de provedores

- Build limpo posterior: 128 testes, zero falhas e zero erros; imagem Docker recompilada.
- HG Finance retornava envelope de erro tipado incorretamente como ação. Agora `valid_key`, `error` e `message` são tratados sem vazar texto externo ou credenciais.
- Alpha Vantage agora classifica mensagens `Information`, `Note` e `Error Message`, distinguindo limite, autenticação, plano, busca sem ativo e cotação ausente.
- Teste real com brapi desabilitada apenas na instância temporária: HTTP 503, categorias `BRAPI: TIMEOUT_OU_TRANSPORTE`, `ALPHA_VANTAGE: LIMITE`, `HG_FINANCE: PLANO`; histórico vazio.
- Não houve compra de plano, alteração das chaves ou remoção de dados. As contas fictícias de smoke test permanecem no banco, sem operações financeiras.

## Revisão e preparação da entrega

Nota histórica: esta seção descreve a revisão anterior. A atualização Docker posterior está registrada na seção de integração abaixo.

- `mvnw.cmd -q clean verify`: saída 0; 138 testes, zero erros, zero falhas e zero ignorados. JAR executável gerado na cópia temporária de build.
- Corrigidos: corrida de unicidade no cadastro, limite BCrypt em bytes no login, precisão decimal no serviço de transações, desempate de histórico e erro seguro para JSON malformado.
- Criada migration aditiva `003-serializar-criacao-ativos.xml` para serializar a primeira criação concorrente de ativos. Teste H2 com duas threads confirmou o mesmo identificador, sem duplicidade.
- `git diff --check` e validação estrita OpenSpec passaram. `.env` permanece ignorado e não versionado.
- Pacote candidato preparado por `scripts/Prepare-Delivery.ps1`, com fontes, testes, documentação e JAR; sem `.env` real, logs, Git ou dumps.
- Relatório de revisão, riscos residuais e passos de versionamento em `ENTREGA.md`.
- Nesta etapa não houve atualização dos contêineres existentes, execução da nova migration em PostgreSQL, commit, tag ou push. Homologação do novo lock em PostgreSQL e sucesso dos fallbacks reais permanecem pendentes.

## Preparação para frontend — 02/09/2026

- `mvnw.cmd -q clean verify`: saída 0, **145 testes**, sem falhas, erros ou ignorados. Build em cópia temporária atualizada, sem copiar `.env`, devido à restrição do OneDrive.
- CORS configurável por origens explícitas; testes de preflight autorizado, origem rejeitada, token inválido e erro 401 JSON acessível pelo front. Origens com curinga/caminho são rejeitadas na configuração.
- GET `/api/usuarios/me` autenticado, com DTO id/nome/email e identidade derivada do principal; teste garante ausência de CPF/hash e ignora tentativa de escolher usuarioId.
- `node --test docs/frontend/api-client.test.mjs`: **5 testes passaram**, cobrindo bearer, logout/401, preservação da sessão em 503, ausência de repetição de POST e resposta não JSON. Node 24.19.0; não equivale a checagem estática com `tsc`.
- `docker compose config --quiet` e `docker compose up -d --build aplicacao`: saída 0. Aplicação recriada, PostgreSQL saudável, volume preservado.
- Consulta PostgreSQL confirmou `catalogo_ativo_lock` id 1 e changeset `006-bloqueio-catalogo-ativos` aplicado. Teste concorrente de carga PostgreSQL ainda não foi executado.
- Smoke HTTP real: preflight 200, origem não autorizada 403, ausência de JWT 401 JSON, cadastro 201, login 200, identidade 200 e carteira vazia 200. Usuário fictício id 10 mantido, sem operações financeiras. Credenciais não impressas.
- Guia `FRONTEND.md`, cliente de referência e exemplos HTTP em `docs/frontend`; empacotador ampliado para incluir esses arquivos. Nenhuma tela, framework, commit, tag ou push criado.
- **37/38 tarefas concluídas**. A tarefa 9.3 permanece parcial exclusivamente quanto à prova de sucesso do fallback real; não foram consumidas novas cotas externas nesta preparação.

## Operação temporária somente brapi — 02/09/2026

- Alteração autorizada pelo usuário após discussão do plano Basic da Twelve Data: Alpha/HG desativadas por padrão em dev/Compose; Twelve Data não adicionada. Implementações, enums históricos, DTOs e endpoints preservados.
- Flags `ALPHAVANTAGE_ENABLED` e `HGFINANCE_ENABLED` removem os respectivos beans da cadeia. Tokens vazios são aceitos somente para fontes desativadas. A brapi permanece obrigatória.
- `mvnw.cmd -q clean verify`: saída 0, **151 testes**, zero falhas/erros/ignorados. A primeira execução identificou duplicidade de bean no novo teste; corrigida antes do build final.
- Novos testes verificam inicialização sem tokens inativos, rejeição de reativação sem token, presença de somente brapi, retorno dos três providers ao reativar e ausência de tentativa Alpha/HG quando brapi falha. A cobertura anterior de fallback continua ativa no perfil test.
- `node --test docs/frontend/api-client.test.mjs`: 5 testes passaram; cliente não alterado. OpenSpec estrito e `git diff --check` passaram.
- Compose validado sem imprimir credenciais, imagem reconstruída e aplicação recriada. Flags false confirmadas no contêiner; PostgreSQL permaneceu saudável, sem remoção de volume ou migrações adicionais.
- Smoke real: proteção 401, cadastro/login e identidade OK, GET PETR4 HTTP 200/fonte BRAPI, histórico com uma consulta, CORS localhost:5173. Usuário fictício id 11 mantido, sem operações financeiras. Uma consulta externa realizada; nenhuma chave alterada ou exposta.
- Reativação documentada em `FRONTEND.md` e `INVESTIMENTOS.md`. **39/40 tarefas concluídas**; 9.3 segue parcial quanto ao sucesso real dos fallbacks, agora temporariamente suspensos por decisão expressa. Nenhum commit, tag ou push.

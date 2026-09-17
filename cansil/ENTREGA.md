# Fundo Cansil — entrega técnica para desenvolvimento e homologação

O back-end está preparado para entrega técnica, não para declaração de produção sem ressalvas. O requisito de sucesso de fallback nas contas reais continua pendente: Alpha Vantage informou limite de consultas; HG Finance informou restrição de plano. Não foram contratados planos ou alteradas credenciais pela revisão.

Decisão posterior autorizada: usar somente brapi em dev/Compose por enquanto. Alpha/HG permanecem implementadas e testadas, mas desligadas por padrão via `ALPHAVANTAGE_ENABLED` e `HGFINANCE_ENABLED`. Fontes desligadas não exigem tokens nem recebem chamadas. Não há fallback ativo neste modo; erro da brapi resulta em 503. Twelve Data não foi integrada. Contratos do frontend e dados históricos permanecem intactos.

Registro histórico de 06/09/2026: **185 testes Java encontrados, sem falhas nem erros** (184 executados e 1 PostgreSQL condicional ignorado na suíte padrão), **62 testes Angular** e **7 testes do cliente TypeScript** aprovados. A migration 005 foi adicionalmente verificada em PostgreSQL 17 descartável; o build Angular passou. A aplicação Docker local foi reconstruída, com backup prévio e conferência de preservação das três operações existentes. Esse resultado está em `openspec/changes/cadastrar-corretoras-cnpj-mercado/verification.md`; as verificações anteriores da migration 004 e concorrência permanecem em `openspec/changes/selecionar-ativos-corretoras/verification.md`.

A evolução de 07/09/2026 adota o nome público **Fundo Cansil** e o grupo Compose `fundo-cansil`, com contêineres independentes `backend`, `frontend` e `postgres`. O frontend é compilado e servido por Nginx com proxy `/api`. A configuração preserva o volume físico `cansil_postgres_data` e usa a porta 5433 do Windows para o banco Docker, sem interromper o PostgreSQL do Windows na 5432. Instruções operacionais estão em [DOCKER.md](DOCKER.md); os resultados efetivamente executados nesta mudança são registrados em `openspec/changes/docker-fundo-cansil/verification.md`, não inferidos dos testes históricos acima.

Corretoras pertencem ao usuário e são cadastradas por CNPJ e razão social verificada, sem bloqueio BR/USA. Excluir uma corretora apaga definitivamente todas as suas transações e, portanto, suas posições. Cotações e ativos independentes permanecem. Compras e vendas recebem automaticamente a data e hora de Brasília no backend. A cobertura de ativos continua dependendo das fontes configuradas; não há execução de ordens reais.

Comece por `DOCKER.md` para executar o sistema completo e por `FRONTEND.md` para os contratos, roteiro das telas e limitações. O frontend Angular está em `../frontend`; `docs/frontend` fornece ainda um cliente TypeScript de referência, testes e exemplos HTTP. A pasta é `cansil` e o pacote principal é `com.curso.cansil`. Os IDs de migrations permanecem intactos para preservar o histórico do banco.

## Revisão de código

Correções realizadas nesta revisão:

- Cadastro: `saveAndFlush` permite converter a violação de unicidade concorrente em HTTP 409, sem retornar detalhes SQL ou CPF.
- Senhas: cadastro e login verificam o limite de 72 bytes UTF-8 do BCrypt; a validação não depende somente da quantidade de caracteres. Login normaliza espaços externos do e-mail.
- Transações: o serviço também rejeita valores fora de `DECIMAL(19,6)`, evitando que chamadas internas gravem quantidades arredondadas para zero ou estourem a coluna.
- Ativos: criação protegida por linha de bloqueio transacional persistente, com nova consulta após obter o lock. Isso protege também a primeira criação simultânea do mesmo símbolo.
- Histórico: consultas no mesmo instante são desempatadas pelo identificador decrescente.
- API: JSON malformado retorna erro 400 padronizado, sem repetir o conteúdo enviado.

O lock do catálogo exige a migration aditiva `003-serializar-criacao-ativos.xml`. Ela cria uma pequena tabela interna e uma linha de controle. Não modifica os changelogs já aplicados nem remove dados existentes. A aplicação desta migration já foi confirmada no PostgreSQL local.

## Verificações e alcance

Testes automatizados cobrem autenticação, isolamento, cadastro CNPJ, uso sem restrição de mercado, regularização, transações, posições por corretora, cálculos, providers com respostas controladas, migrations H2/PostgreSQL e regressões do legado. O build de entrega deve usar `mvnw.cmd clean verify` e produzir `target/fundo-cansil-0.0.1-SNAPSHOT.jar`. O alcance e as evidências da configuração atual ficam em `openspec/changes/docker-fundo-cansil/verification.md`. A fonte cadastral foi testada com respostas controladas; isso não garante disponibilidade externa no momento de uso. Healthchecks dos contêineres também não comprovam cota ou disponibilidade dos provedores externos.

Esta revisão não equivale a um pentest ou a uma auditoria completa de vulnerabilidades de dependências. A concorrência foi testada em H2 e PostgreSQL descartável; homologação com carga permanece recomendada antes de produção.

## Cuidados antes de publicação

- Os endpoints legados de produtos permanecem públicos para compatibilidade. Não expor a aplicação à internet sem definir a política de acesso do legado.
- O Compose publica, por padrão, 4200 (frontend), 8080 (backend) e 5433 (PostgreSQL) em todas as interfaces. Isso não é uma implantação de produção. Para uso estritamente local, revisar a publicação com endereço `127.0.0.1`; não alterar silenciosamente se outras máquinas precisarem acessar.
- Definir HTTPS, limites de requisições, backups e política de retenção antes de produção. As listagens atuais não são paginadas.
- Proteger o `.env` local e não incluí-lo em commits, anexos, logs ou ZIPs.
- Os usuários fictícios dos smoke tests permanecem no banco; a revisão não os excluiu.
- As cotações de terceiros podem ter atraso. A API ainda não apresenta horário/idade da cotação; não tratá-la como plataforma de negociação em tempo real.

## Pacote de entrega

`scripts/Prepare-Delivery.ps1` monta um ZIP **somente do backend** por lista positiva, com código, testes, migrations, configurações de exemplo, documentação e JAR opcional. Exclui `.env` real, Git, IDE, logs e dumps. O script imprime o SHA-256 para conferência. O frontend Angular não é incorporado nesse pacote.

Exemplo após um build local bem-sucedido:

```powershell
.\scripts\Prepare-Delivery.ps1 -JarPath .\target\fundo-cansil-0.0.1-SNAPSHOT.jar
```

O ZIP `fundo-cansil-backend-candidato-*.zip` fica em `dist`, já ignorado pelo Git. Se o JAR foi incluído, configure as variáveis de ambiente e execute `java -jar bin/fundo-cansil.jar`. O arquivo `.env` é consumido pelo Compose, não automaticamente pelo comando Java.

Para usar o Compose **completo**, é indispensável disponibilizar também a pasta irmã `../frontend`, com seu Dockerfile, configuração Nginx e fontes, conforme [DOCKER.md](DOCKER.md). O ZIP isolado do backend não é uma entrega completa do site. Para executar apenas API e banco, use explicitamente `docker compose up -d --build backend postgres`, depois de configurar as credenciais e criar/selecionar o volume externo correto.

## Plano de versionamento — ainda não executado

1. Revisar `git status --short` e conferir que `.env` não está versionado.
2. Criar uma branch `codex/gestao-investimentos-backend` somente quando autorizado.
3. Revisar os grupos de commits/tags sugeridos em `INVESTIMENTOS.md`. Como as classes têm dependências cruzadas, não presumir que cada grupo isolado compila sem organizar previamente os patches.
4. Executar o build completo na versão que será entregue e registrar o resultado.
5. Somente após autorização, fazer commits, criar a tag candidata e enviar ao remoto correto.

Nenhum commit, tag, push ou atualização automática do contêiner em execução é feito pelo empacotador.

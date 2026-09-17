# Verificação — cadastro de corretoras por CNPJ e mercado

Data: 06/09/2026. Alteração implementada no backend e no Angular irmão, sem commit, push, publicação ou arquivamento da mudança.

## Resultado funcional

- Consulta autenticada de CNPJ e cadastro com razão social obtida novamente pelo servidor na BrasilAPI. Validação de identidade, formato, dígitos verificadores e resposta externa; falhas não geram cadastro fictício.
- Corretoras isoladas por usuário, com unicidade de CNPJ por conta e mercado BR/USA imutável. Identificadores de outra conta não permitem uso ou regularização.
- Migração 005 cria vínculos legados por usuário e preserva as operações; regularização explícita mantém o identificador do vínculo e o mercado BR histórico. Novas compras exigem corretora ativa e verificada; vendas de saldo legado BR continuam permitidas.
- Nova tela Angular “Corretoras”, razão social não editável, seleção de mercado, regularização, proteção contra respostas antigas/envio duplicado e recuperação de resultado incerto. Operações selecionam primeiro corretora/mercado, depois ativo. Carteira mantém as seis colunas e as regressões por corretora.
- Cadastro USA permitido com aviso; operações USA rejeitadas antes de consultar/criar ativos. Não há integração de ordens reais nem mistura de posições em moedas diferentes.

## Testes e build

| Verificação | Resultado |
| --- | --- |
| `mvnw.cmd -q test` | 185 encontrados; 184 executados, 1 ignorado condicional; 0 falhas e 0 erros |
| `MigracaoCadastroCorretoraTest` em H2 | Upgrade 001–004 → 005, preservação, isolamento e unicidade aprovados na suíte |
| `MigracaoCadastroCorretoraTest` em PostgreSQL 17 descartável | Executado adicionalmente com propriedades `cnpj.test.jdbc-url/user/password`; passou sem ignorar |
| `npm.cmd test -- --watch=false` no frontend | 62 testes aprovados, 7 arquivos |
| `npm.cmd run build` no frontend | Produção aprovada; 401,38 kB iniciais, transferência estimada 103,51 kB |
| `node --test docs/frontend/api-client.test.mjs` | 7 testes aprovados |
| `openspec.cmd validate cadastrar-corretoras-cnpj-mercado --strict` | Válido em ambas as raízes locais |
| `git diff --check` no backend | Sem erros de whitespace; avisos de conversão LF/CRLF |

O teste condicional ignorado na suíte padrão é o PostgreSQL antigo; o teste PostgreSQL específico da migração 005 foi executado separadamente. As verificações históricas de migração 004/concorrência estão na mudança anterior, não foram repetidas como parte deste teste direcionado.

As respostas BrasilAPI e de cotação são controladas nos testes. A integração HTTP cobre autenticação, CNPJ repetido com máscara, rejeição de nome arbitrário, isolamento de contas, bloqueio USA e ausência de transação/cotação após rejeição. Os oito novos testes Angular cobrem o fluxo cadastral, erros, consulta antiga, regularização, incerteza e bloqueio USA.

## Atualização local e preservação

1. Backup PostgreSQL custom anterior à migração, validado com `pg_restore --list`, copiado para `.private-backups/postgres-before-005-20260906.dump` (21.769 bytes). A pasta é ignorada pelo Git e agora também pelo contexto Docker. O arquivo contém dados locais e não deve ser compartilhado.
2. SHA-256 do backup: `33F46EEE808351F867FE7E51266990E5E82F6F1D88DE10092C6D8F67EB8688F0`. A listagem do dump foi validada, mas não foi realizado ensaio completo de restauração deste backup.
3. `docker compose up -d --build aplicacao` concluído; PostgreSQL saudável e aplicação em execução. O volume principal foi preservado.
4. Confirmado changeset `008-cadastro-corretoras-por-usuario` da migration 005 no banco principal.
5. Antes e depois: 3 operações; mesma assinatura `99a451aeca66b8ff00444c666d9b7421`, calculada sobre IDs, usuário, ativo, tipo, quantidade, preço e data em ordem de ID. A assinatura é uma conferência de preservação, não um mecanismo criptográfico de segurança. O vínculo corretora_id muda intencionalmente para a cópia do usuário.
6. Após migração: zero vínculos de operação com titularidade divergente e um vínculo próprio legado pendente de CNPJ. Não foram criadas contas nem operações de teste no banco principal nesta etapa.
7. GET anônimo `/api/corretoras` retornou 401; frontend `http://127.0.0.1:4200/` retornou 200. Isso não equivale a teste visual ou fluxo autenticado manual no navegador.
8. Contêiner descartável `cansil-cnpj-teste` e seu volume anônimo foram removidos após os testes. Somente dados fictícios foram descartados; backup e volume principal permanecem.

## Limites e referências

- Consulta de CNPJ usa a [BrasilAPI, contrato oficial do projeto](https://github.com/BrasilAPI/BrasilAPI/blob/main/pages/docs/doc/cnpj.json), não a brapi. Não foi realizado teste real de disponibilidade da consulta cadastral nesta etapa. Dados podem estar defasados e não certificam autorização regulatória.
- O validador está preparado para CNPJ numérico e alfanumérico conforme o [manual de dígitos verificadores da Receita Federal](https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj/manual-dv-cnpj.pdf). A consulta alfanumérica depende do suporte efetivo da fonte; resposta inválida não libera cadastro.
- A fonte de ativos atual continua [brapi/B3](https://brapi.dev/docs). Não foi integrada fonte USA, nem conversão de moeda; Alpha/HG permanecem desativadas e Twelve Data não foi adicionada. Cadastrar mercado USA não libera negociação nem registro de operação USA.
- Não há troca de mercado, fusão de corretoras, transferência de posições ou edição/exclusão de operações. Regularizar CNPJ é confirmação explícita do usuário, não inferência do sistema.
- Não houve inspeção visual em navegador real nesta etapa. Recomenda-se conferir manualmente o fluxo em conta de desenvolvimento; registros confirmados são persistentes. Portas, HTTPS, limites e demais preparativos de produção seguem as ressalvas de `ENTREGA.md`.

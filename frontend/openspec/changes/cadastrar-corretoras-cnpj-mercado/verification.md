# Verificação — interface de corretoras

Data: 06/09/2026.

- Nova rota autenticada `/corretoras`: consulta explícita CNPJ, razão social não editável, mercado BR/USA, cadastro próprio e regularização de legado BR.
- Contratos e cliente HTTP atualizados; proteção contra resposta antiga após editar CNPJ, repetição durante envio, navegação enquanto o POST está pendente e reenvio após resultado incerto.
- Operações selecionam corretora antes do ativo; corretoras USA exibem limitação e não permitem consulta de catálogo nem POST de operação. Histórico informa mercado. Carteira mantém as seis colunas e o cálculo do backend.
- `npm.cmd test -- --watch=false`: 62 testes aprovados em 7 arquivos, incluindo oito testes novos de cadastro, regularização, erros e bloqueio USA.
- `npm.cmd run build`: aprovado; 401,38 kB iniciais, transferência estimada de 103,51 kB.
- `openspec.cmd validate cadastrar-corretoras-cnpj-mercado --strict`: válido.
- Servidor Angular existente respondeu HTTP 200 em `http://127.0.0.1:4200/`; não houve teste visual/interação em navegador real. Os testes Angular usam dados e HTTP simulados.

Backend e migração foram atualizados no Docker local, com backup e preservação conferida das três operações anteriores. A verificação completa está em `../../../../cansil/openspec/changes/cadastrar-corretoras-cnpj-mercado/verification.md` (a partir desta pasta de mudança).

Consulta cadastral depende da disponibilidade BrasilAPI; os testes não comprovam disponibilidade real da fonte. Cadastro USA é permitido, operações USA ainda não. Não há execução de ordens reais. Sem commit, push, publicação ou arquivamento da mudança.

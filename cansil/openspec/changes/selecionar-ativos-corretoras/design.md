## Context

Ver `proposal.md` para motivação e limites funcionais. O backend é Java 17/Spring Boot, com Liquibase, PostgreSQL e H2; o Angular fica em `../frontend`. As transações atuais não têm corretora, são isoladas por usuário e ordenadas por data/id. O serviço bloqueia o usuário antes de registrar e valida toda a sequência do ativo. O custo consolidado e a avaliação já existem; a brapi é a única fonte habilitada.

O OpenSpec está no repositório do backend, cujo `actionContext.allowedEditRoots` atual não inclui a pasta irmã. Nenhum arquivo de produto será editado nesta fase. Antes de aplicar tarefas do Angular, resolver um contexto de planejamento/edição que inclua o frontend de acordo com as ferramentas do projeto, ou obter orientação explícita se o contexto permanecer restrito. Não mover pastas, não alterar o escopo silenciosamente nem declarar a entrega concluída sem o frontend.

## Goals / Non-Goals

**Goals:** manter compatibilidade das leituras consolidadas, tornar obrigatória a corretora nas novas escritas, preservar todos os dados antigos e reaproveitar autenticação, cálculos, proteções de envio e stack existentes.

**Non-Goals:** não persistir preços médios derivados como fonte de verdade, não converter datas para UTC, não adicionar credenciais de corretoras, não realizar apuração fiscal e não implementar transferências de custódia nesta mudança. A comparação por corretora é gerencial.

## Decisions

### 1. Corretora como entidade de catálogo

Criar `corretora` com `id`, `codigo` único, `nome`, `ativa` e `legada`. Usar nomes comerciais, sem números de conta, tokens ou credenciais do usuário. O catálogo inicial mínimo conterá XP e Inter, usados no desenho aprovado, após conferir sua identificação em fontes oficiais; não alegar lista exaustiva. Ampliações serão dados versionados por migração, sem CRUD administrativo nesta entrega.

Criar uma entrada técnica reservada `NAO_INFORMADA`, nome `Corretora não informada`, `legada=true`, indisponível para compras novas. O identificador vem do backend; o Angular nunca o fixa em código.

Alternativas: texto livre não assegura seleção/identidade estável; enum exige alterar código para cada nova instituição. Entidade de catálogo permite seleção, referências e inativação preservando histórico.

### 2. Migração aditiva e legado explícito

Criar `004-adicionar-corretoras.xml`, sem modificar checksums dos changelogs anteriores. Criar catálogo e sua sequência; acrescentar `transacao.corretora_id` inicialmente anulável; preencher somente a categoria técnica para todas as linhas existentes; adicionar FK, índice de usuário/ativo/corretora/data/id e obrigatoriedade. Não deixar um default que aceite novas operações sem seleção. Garantir sequência consistente após inserir os registros iniciais.

Operações antigas não serão reatribuídas a XP/Inter. Uma venda do legado exigirá escolher explicitamente a categoria técnica e respeitar seu saldo; compras nela são rejeitadas. Corretora real inativa também fica impedida para novas compras, mas pode liquidar saldo existente.

### 3. Contratos HTTP

Todos os endpoints seguem JWT e mensagens seguras existentes, sem `usuarioId` como autoridade:

- `GET /api/corretoras`: lista `{id,codigo,nome,ativa,legada}`, ordenada por nome. Inclui categorias inativas/legada para identificar histórico; o frontend só as oferece para vendas de posições próprias existentes.
- `GET /api/ativos?busca=PETR&pagina=0&tamanho=20`: `{itens:[{simbolo}],pagina,tamanho,total,atualizadoEm,desatualizado}`. Página baseada em zero; tamanho 1–100; busca até 12 caracteres alfanuméricos, vazia permitida, normalizada. Erros de parâmetro retornam 400.
- `POST /api/transacoes`: acrescenta `corretoraId` obrigatório e positivo. Ausência, identificação inexistente ou tipo incompatível retornam 400; saldo insuficiente retorna 422. Mantém decimais de até 13 inteiros/6 casas e `LocalDateTime`.
- `GET/POST /api/transacoes`: resposta atual mais `corretora:{id,codigo,nome,ativa,legada}`.
- `GET /api/carteira/corretoras`: posições abertas `{simbolo,corretora:{...},quantidade,precoMedio,custoTotal}`, ordenadas por símbolo e corretora.
- `GET /api/carteira`: preserva campos/cálculo existentes e acrescenta `custoTotal` calculado no servidor, para a visão consolidada sem consulta de preço.
- `GET /api/carteira/avaliacao`: preserva cálculo e comportamento completo-ou-erro. Reutilizado para obter cotação por símbolo distinto; não criar outra rota de preço só para corretoras.

Mudança incompatível limitada à exigência de corretora nas escritas. Exemplos HTTP, cliente TypeScript de referência e seus testes precisam acompanhar a atualização; consultas anteriores permanecem utilizáveis.

### 4. Saldos e duas bases gerenciais

Manter o lock por usuário e a lista completa ordenada por ativo. Antes de salvar a candidata, validar a sequência filtrada pela corretora e também a sequência consolidada. Isso cobre compra inicial concorrente, venda retroativa e corridas de saldo sem trocar o esquema de locks existente.

Reusar a calculadora decimal existente para cada grupo `(ativo,corretora)` e para o grupo consolidado `(ativo)`. Usar `HALF_UP`, quantidade/preço médio com 6 casas e custo monetário com 2, conforme contratos existentes. Nenhum cálculo monetário oficial no Angular.

O custo restante exibido será `quantidade × precoMedio` calculado no backend na respectiva base. Não é o total histórico de aportes nem a receita de vendas. O consolidado continua seguindo a sequência global; não somar os custos gerenciais por corretora para fabricar um consolidado equivalente. O cenário numérico da especificação testa explicitamente a diferença após venda seletiva. Não alterar o comportamento consolidado histórico para forçar igualdade visual.

Alternativa rejeitada: repartir automaticamente o preço médio global por todas as corretoras, pois eliminaria a comparação gerencial por instituição aprovada. A interface explicará a diferença sem alegar finalidade tributária.

### 5. Catálogo de ativos no backend

Consultar o endpoint público observado `GET https://brapi.dev/api/available`, com DTO tipado para `stocks` e `indexes`. Não consultar cotação, não gravar ativos/consultas ao buscar e não copiar o arquivo estático `catalogos/ativos-brapi-2026-09-03.txt` para simular um catálogo atualizado.

Cache de catálogo em memória no backend por 6 horas, relógio injetável nos testes, uma atualização concorrente por vez e intervalo mínimo de 1 minuto entre tentativas após falha. Timeout limitado a 5 segundos. Filtrar símbolos incompatíveis com `[A-Z0-9]{4,12}`, normalizar, deduplicar e ordenar antes da paginação. Não fazer uma chamada externa por termo. O campo `indexes` não oferece opções no formulário atual.

Cache vencido pode servir a última cópia válida com `desatualizado=true` e a data original após falha de atualização. Sem cache, responder 503, não `[]` como se o catálogo fosse vazio. A seleção por posição da própria carteira é uma fonte independente para continuar usando ativos já registrados.

O catálogo não é prova de acesso ao preço. Ativo novo continua dependendo de cotação válida na primeira gravação; ativo já persistido não passa a depender de catálogo externo. O backend valida formato e identidade, não confia no controle Angular como segurança. No provider brapi, ampliar o DTO para conferir o símbolo efetivamente retornado: código divergente/resolvido não deve receber preço de outro ativo sob o nome solicitado. Cobrir respostas válidas, divergentes e incompletas com fixtures tipadas.

Fontes verificadas nesta conversa: https://brapi.dev/docs/acoes/list e https://brapi.dev/api/available. Revalidar o contrato se a implementação ocorrer após mudanças no provedor; nenhuma consulta autenticada em massa faz parte da entrega.

### 6. Angular: seleção e apresentação

Preservar Angular, tema, formulários reativos e JWT somente em memória. Implementar seleção de ativo com busca e lista paginada acessível, sem biblioteca nova: campo de pesquisa, botão Buscar e opções selecionáveis por controle nativo. Esse desenho evita construir um combobox ARIA complexo sem necessidade. Mudança no texto limpa a seleção; Enter/botão faz a busca, não cada tecla. Estados de carregamento/erro/vazio são distintos.

Corretoras vêm do backend em um seletor. Atalhos de venda preenchem símbolo e identificação da corretora a partir da posição validada pelo backend. Nunca tratar parâmetros da URL como autorização ou como indicação de que existe saldo. Oferecer posições próprias como alternativa quando o catálogo estiver indisponível. O preço realmente negociado continua um campo separado e não é preenchido pela cotação.

Carteira abre por corretora com as seis colunas aprovadas, sem Nome. Um controle permite alternar para a visão consolidada; nesta, `Corretora` mostra `Todas as corretoras` e os números vêm do endpoint consolidado, com explicação das bases. Na visão por corretora usar chave composta símbolo/id, nunca somente símbolo. Atalhos podem ficar dentro da célula do ativo, sem criar uma coluna extra de resultados financeiros.

### 7. Cotação explícita e independente do custo

Carregar posições sem consultar preços. O botão `Atualizar cotações` usa a avaliação consolidada existente, que já produz um resultado por símbolo distinto. Mapear apenas `cotacaoAtual` e `fonte` para as linhas de cada corretora. Não usar custos consolidados para preencher custos gerenciais.

Limpar cotações anteriores no início de nova tentativa e na atualização de posições. Em sucesso, exibir horário local de recebimento rotulado `Consulta`, não horário de negociação; em erro, mostrar `Indisponível` e manter quantidades/custos. Preservar o comportamento completo-ou-503 do endpoint de avaliação, sem apresentar preços parciais como avaliação completa. Sem polling, sem cotações automáticas ao digitar ou selecionar e sem inserir consultas individuais em massa no histórico.

### 8. Regressão e segurança

Preservar as proteções de envio: operação pendente bloqueia navegação interna/logout; resposta incerta bloqueia reenvio até conferência explícita de histórico recente; 401 paralelo espera POST terminar; 503 não limpa sessão. Os formulários não enviam `usuarioId`, credenciais de corretora ou tokens de provedor.

Atualizar testes Java de construtores/DTOs e fixtures HTTP, incluindo migração com dados anteriores, isolamento entre usuários e corretoras, saldo temporal/concorrente, categoria legada, duas bases de custo, contrato do catálogo e nenhuma escrita na busca. Atualizar testes Angular e cliente de referência, incluindo seleção invalidada, linhas duplicadas por símbolo, falha de catálogos/cotações e as regressões existentes. Nenhuma conta real ou operação financeira de usuário é criada pelos testes automatizados.

## Risks / Trade-offs

- [Custo gerencial pode divergir do consolidado] → rótulos e explicação explícita; teste numérico; não somar bases incompatíveis ou apresentar como IR.
- [Catálogo público não garante cobertura de preço e pode incluir códigos especiais] → aviso, validação de símbolos/respostas e manutenção dos fluxos de erro existentes.
- [Cache desaparece no reinício] → erro claro até obter catálogo; posições próprias e históricos continuam disponíveis; não inventar resultados.
- [Catálogo de corretoras inicial pequeno] → indicar que contém as instituições inicialmente cadastradas e ampliar por dados versionados quando solicitado, sem alegar exaustividade.
- [Frontend antigo não envia corretora] → entrega coordenada e atualização de todos os exemplos; falhar claramente com 400, sem escolher corretora por padrão.
- [Transferências reais não são modeladas] → não permitir uso cruzado de saldos; futuro fluxo próprio, sem recomendar registros fictícios de compra/venda.
- [Contexto OpenSpec cobre apenas backend] → resolver escopo do frontend antes de editá-lo; não ignorar o limite nem descartar as tarefas de UI.
- [Migração em volume usado] → testar upgrade e executar backup antes de atualizar a aplicação; nunca remover o volume.

## Migration Plan

1. Validar os artefatos e obter a solicitação de aplicação após esta apresentação; resolver contexto de edição do frontend.
2. Implementar e testar primeiro em H2 e em banco PostgreSQL descartável, com dados fictícios anteriores à nova migração. Conferir contagens, valores, vínculos, sequência e saldos antes/depois.
3. Compilar backend e frontend e atualizar documentação/exemplos como uma entrega coordenada. Registrar os testes reais executados, sem usar mocks como prova de PostgreSQL ou navegador.
4. Antes de aplicar no Docker local utilizado pelo usuário, realizar backup protegido e validar a configuração sem imprimir segredos. Atualizar somente a aplicação com o volume preservado; não executar `down -v`.
5. Verificar saúde, versão de migração, integridade do legado e endpoints de leitura. Testes de escrita usam contas de desenvolvimento claramente separadas, sem alterar posições reais.
6. Rollback operacional: preferir correção aditiva. Voltar apenas à imagem anterior não é seguro, pois ela não preenche `corretora_id`. Se necessário, interromper novas escritas e coordenar restauração do backup com o usuário, sem descartar operações criadas após a atualização. Nenhum rollback destrutivo automático.

## 1. Preparação e catálogo de corretoras

- [x] 1.1 Confirmar a solicitação de aplicação dos artefatos apresentados e resolver o contexto de edição do frontend irmão antes de alterá-lo; verificar escopos retornados pelo OpenSpec e registrar a resolução sem mover projetos ou ignorar `allowedEditRoots`.
- [x] 1.2 Conferir em fontes oficiais a identificação comercial das corretoras iniciais XP e Inter e definir códigos estáveis e a categoria técnica `NAO_INFORMADA`; verificar lista documentada e ausência de credenciais ou atribuições reais a operações antigas.
- [x] 1.3 Implementar entidade/repository/DTO e GET autenticado de corretoras com ordenação e flags de ativa/legada; verificar testes de contrato, 401, ordenação e ausência de campos sensíveis.

## 2. Migração e operações por corretora

- [x] 2.1 Criar migração aditiva 004 com catálogo, sequência, preenchimento técnico do legado, FK, índice e obrigatoriedade sem default permanente; testar upgrade H2 com registros anteriores, preservação de identificadores/valores/datas e integridade das sequências.
- [x] 2.2 Incluir corretora na entidade de transação e tornar `corretoraId` obrigatório no DTO de entrada, com respostas incluindo corretora; testar 400 para ausência/id inválido, rejeição de compra legada/inativa e venda de saldo legado explicitamente selecionado.
- [x] 2.3 Validar sequência temporal por usuário/ativo/corretora e sequência consolidada sob os locks existentes; testar venda usando saldo de outra instituição, retroatividade, isolamento entre usuários e duas vendas concorrentes.
- [x] 2.4 Atualizar histórico e mapeamentos de transações para carregar corretora sem alterar ordem nem expor entidades JPA; testar serialização, identificadores preservados e ordem por data/id.

## 3. Posições e custos

- [x] 3.1 Implementar GET de posições por corretora com quantidade, preço médio e custo restante decimal; testar compras ponderadas, vendas parciais, zeragem e grupos do mesmo símbolo em duas instituições.
- [x] 3.2 Acrescentar `custoTotal` à leitura consolidada sem alterar a regra existente; verificar o cenário A=10×20, B=10×40 e venda de 5 em A, com custos gerenciais 100/400 e custo consolidado 450.
- [x] 3.3 Preservar avaliação completa-ou-erro e consulta única por símbolo distinto, independentemente do número de corretoras; testar contagem de chamadas, ausência de consulta de preço no GET de posições e preservação de 503 seguro.

## 4. Catálogo e identidade dos ativos

- [x] 4.1 Implementar cliente tipado do catálogo público da brapi com timeout, normalização, deduplicação e filtro de símbolos compatíveis; testar respostas válidas/malformadas, índices excluídos e ausência de gravação de consultas/operações.
- [x] 4.2 Implementar cache de 6 horas, atualização única concorrente e intervalo mínimo de 1 minuto após falha; testar com relógio controlado buscas sem chamadas extras, cache vencido sinalizado e 503 sem cópia válida.
- [x] 4.3 Expor GET autenticado e paginado de ativos por código; testar normalização, paginação, limites, vazio legítimo, 400/401 e ausência de segredos no retorno/log.
- [x] 4.4 Verificar símbolo efetivamente devolvido na cotação brapi antes de usá-la; atualizar DTOs/fixtures e testar correspondência, símbolo divergente/resolvido, resposta incompleta e nenhuma gravação de ativo/preço incompatíveis.

## 5. Frontend Angular

- [x] 5.1 Atualizar contratos e cliente HTTP com corretoras, catálogo e posições por corretora; testar bearer, respostas tipadas, envio obrigatório de `corretoraId`, decimais como strings e datas sem conversão UTC.
- [x] 5.2 Implementar busca explícita paginada e seleção acessível de ativo, alternativas pelas posições próprias e seletor de corretora; testar mudança do texto invalidando seleção, falta de catálogo, flags legada/inativa e operação por teclado nos testes de componente.
- [x] 5.3 Atualizar formulário, atalhos de venda e histórico com corretora; testar preseleção a partir de posição, parâmetros de URL sem autoridade, ausência de preço automático e regressões de envio duplicado/incerto/expiração concorrente.
- [x] 5.4 Atualizar carteira com as seis colunas na ordem aprovada, chaves compostas e alternância gerencial/consolidada; testar duas corretoras para um símbolo, custo vindo do backend, legado, posições vazias e ausência de coluna Nome.
- [x] 5.5 Incluir atualização explícita de cotações na carteira com fonte/horário de consulta, limpeza dos preços anteriores e preservação de posições em erro; testar uma avaliação por clique, mapeamento por símbolo e nenhuma atualização automática.

## 6. Verificação e entrega coordenada

- [x] 6.1 Atualizar exemplos HTTP, cliente TypeScript de referência e guias de backend/frontend com corretora obrigatória, catálogos, duas bases de custo, limitações e migração; verificar exemplos e executar os testes do cliente de referência.
- [x] 6.2 Executar suíte completa Java, testes Angular e compilação de produção; verificar regressões existentes, todos os cenários desta mudança e ausência de novas dependências/chaves não autorizadas, registrando contagens e resultados reais.
- [x] 6.3 Testar upgrade e concorrência em PostgreSQL descartável com dados fictícios anteriores à migração; verificar contagens, vínculos, valores, sequência, preservação de saldos e ausência de operações em contas reais.
- [x] 6.4 Preparar backup protegido e atualizar aplicação Docker local preservando volume, após os testes e respeitando autorizações; verificar saúde, migração 004, leitura do legado e autenticação sem imprimir segredos, registrando qualquer etapa não executada como pendente.
- [x] 6.5 Revisar diff e validar OpenSpec; registrar limitações e instruções para teste manual da interface sem afirmar que houve teste em navegador real. Não fazer commit, push, publicação ou rollback destrutivo sem autorização correspondente.

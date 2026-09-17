## Context

Esta raiz OpenSpec local resolve o limite `allowedEditRoots`: o backend continua sendo alterado somente pelo contexto do projeto `cansil`, enquanto esta mudança coordenada governa exclusivamente o irmão `frontend`. O escopo funcional é o já aprovado no artefato `cansil/openspec/changes/selecionar-ativos-corretoras`; os projetos não são movidos e nenhuma raiz é ignorada.

## Goals / Non-Goals

**Goals:**
- Pesquisar e selecionar um símbolo válido sem manter catálogo estático no navegador.
- Selecionar uma corretora permitida conforme tipo de operação e saldo existente.
- Exibir posições por corretora e consolidadas com bases de custo claramente diferenciadas.
- Preservar as garantias de sessão, envio único e resultado incerto já existentes.

**Non-Goals:**
- Executar ordens reais, calcular impostos ou transferir custódia.
- Armazenar JWT, segredos ou chaves de provedores no navegador.
- Consultar cotações automaticamente ou alegar tempo real.
- Publicar ou hospedar a aplicação.

## Decisions

- A busca terá campo, botão explícito, lista nativa paginada e seleção invalidada quando o texto mudar.
- Ativos já presentes na carteira permanecem alternativas quando o catálogo externo estiver indisponível.
- A corretora vem sempre da API; compras não oferecem corretoras inativas ou legadas, e vendas oferecem somente instituições com saldo do ativo selecionado.
- A carteira inicia na visão por corretora, com colunas `Ativo`, `Corretora`, `Quantidade`, `Valor investido`, `Preço médio`, `Cotação atual`.
- A visão consolidada identifica a corretora como `Todas as corretoras` e explica que seu preço médio histórico pode diferir da soma dos custos gerenciais por instituição.
- `Atualizar cotações` executa uma única avaliação consolidada, limpa valores anteriores e mapeia preço e fonte por símbolo; falhas não removem as posições.

## Risks / Trade-offs

- A API de catálogo pode falhar; a interface sinaliza isso e mantém posições próprias selecionáveis.
- Parâmetros de URL aceleram o preenchimento, mas são revalidados e não concedem autorização.
- A migração do backend pode tornar o frontend antigo incompatível; o rollout local deve atualizar backend e frontend em conjunto.

## Migration Plan

1. Atualizar contratos e cliente HTTP.
2. Adaptar operações e histórico.
3. Adaptar carteira e atualização explícita de cotações.
4. Executar testes Angular e build de produção.

## Open Questions

- Nenhuma; decisões funcionais foram aprovadas no projeto coordenador.

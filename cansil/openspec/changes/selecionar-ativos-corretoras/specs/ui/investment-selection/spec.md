## Purpose

Oferecer seleção clara de ativo e corretora e apresentar custos, quantidades e preços de mercado em uma carteira acessível, preservando os fluxos de autenticação e registro já existentes.

## ADDED Requirements

### Requirement: Seleção pesquisável e coerente no formulário

O formulário SHALL permitir selecionar um ativo do catálogo ou das posições do próprio usuário e uma corretora compatível com a operação. Quantidade, preço efetivamente negociado e data SHALL continuar sendo informados pelo usuário. Custos, preço médio e cotações SHALL NOT ser preenchidos manualmente como resultados calculados.

#### Scenario: Compra com seleção
- **WHEN** o usuário escolhe ativo e corretora e preenche quantidade, preço negociado e data válidos
- **THEN** o envio contém os códigos selecionados e os dados informados, sem nome de corretora como identificador, sem identificador de usuário e sem substituir o preço negociado pela cotação

#### Scenario: Texto alterado após selecionar ativo
- **WHEN** o usuário altera a busca depois de selecionar um símbolo
- **THEN** a seleção anterior é invalidada até uma nova escolha, impedindo envio silencioso do código anterior

#### Scenario: Acesso por atalho de venda
- **WHEN** o usuário abre o registro de venda a partir de uma posição
- **THEN** o ativo e a corretora daquela linha ficam selecionados, mesmo com o catálogo externo indisponível

### Requirement: Tabela de carteira aprovada

A carteira por corretora SHALL exibir as colunas nesta ordem: `Ativo`, `Corretora`, `Quantidade`, `Valor investido`, `Preço médio`, `Cotação atual`. Ela SHALL NOT incluir coluna `Nome`. Valores monetários SHALL ter formatação pt-BR e rótulos que distingam custo restante, média por unidade e preço de mercado.

#### Scenario: Ativo em duas corretoras
- **WHEN** existem posições abertas do mesmo símbolo em duas corretoras
- **THEN** a carteira mostra duas linhas distintas, identificadas por ativo e corretora, sem sobrescrever ou juntar seus saldos

#### Scenario: Visualização consolidada
- **WHEN** o usuário escolhe a visão consolidada por ativo
- **THEN** a interface identifica `Todas as corretoras`, exibe o cálculo consolidado fornecido pelo servidor e explica que sua base difere do custo gerencial por instituição

### Requirement: Histórico com corretora e segurança de envio

O histórico SHALL identificar a corretora de cada transação e preservar ordem decrescente de data/identificador. Durante envio SHALL continuar bloqueada a repetição, a saída interna e o logout. Resultado incerto SHALL continuar exigindo conferência do histórico antes de liberar reenvio.

#### Scenario: Resultado incerto ou sessão expirada durante envio
- **WHEN** ocorre falha de rede na gravação ou expiração paralela da sessão
- **THEN** a inclusão da corretora não remove as proteções já existentes contra repetição ou cancelamento de operação pendente

### Requirement: Estados de disponibilidade e acessibilidade

A interface SHALL oferecer rótulos, navegação por teclado, foco identificável e estados distintos de carregamento, vazio e erro para ativos, corretoras, carteira e cotação. Falha de catálogo SHALL NOT ser confundida com ausência de ativos. Cotações SHALL ser atualizadas apenas mediante ação explícita, com indicação de fonte e horário de consulta.

#### Scenario: Navegação por teclado e tela estreita
- **WHEN** o usuário realiza a seleção sem mouse ou usa uma tela estreita
- **THEN** os controles permanecem operáveis, as opções e a seleção são identificáveis e a tabela pode ser percorrida sem ocultar permanentemente colunas

#### Scenario: Cotação indisponível
- **WHEN** a atualização de cotações falha
- **THEN** a coluna mostra indisponibilidade, não um preço fictício ou antigo rotulado como atual, e as demais informações da posição permanecem visíveis

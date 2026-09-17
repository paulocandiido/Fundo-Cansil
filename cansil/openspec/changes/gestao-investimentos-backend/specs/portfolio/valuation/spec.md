## Purpose

Avaliar posições abertas comparando seu custo médio com cotações atuais obtidas pelo mecanismo resiliente de múltiplas fontes.

## ADDED Requirements

### Requirement: Avaliação de posição
O sistema SHALL informar quantidade, preço médio, cotação atual, fonte da cotação, custo, valor atual e lucro/prejuízo absoluto e percentual por posição aberta.

#### Scenario: Posição com lucro
- **WHEN** há 10 unidades com média 20,00 e cotação 25,00
- **THEN** custo é 200,00, valor atual 250,00, resultado 50,00 e percentual 25,00%

#### Scenario: Posição com prejuízo
- **WHEN** há 10 unidades com média 20,00 e cotação 18,00
- **THEN** resultado é -20,00 e percentual -10,00%

### Requirement: Precisão decimal
O sistema MUST usar cálculo decimal, escala e arredondamento explícitos, sem ponto flutuante binário para dinheiro.

#### Scenario: Divisão não exata
- **WHEN** média ou percentual possui casas excedentes
- **THEN** o sistema usa `HALF_UP` de forma determinística

### Requirement: Avaliação completa
O sistema SHALL obter cada cotação pelo orquestrador de fallback e falhar a avaliação quando qualquer posição não puder ser cotada por nenhuma fonte.

#### Scenario: Fallback bem-sucedido
- **WHEN** a fonte primária falha mas um fallback retorna cotação
- **THEN** a avaliação usa essa cotação e identifica sua fonte

#### Scenario: Ativo sem cotação em todas as fontes
- **WHEN** uma posição não possui cotação em nenhum provider
- **THEN** o sistema retorna erro explícito sem apresentar total parcial como completo


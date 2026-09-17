## Purpose

Identificar a corretora das operações, impedir uso de saldo de outra instituição e apresentar posições gerenciais por corretora sem perder o histórico e a visão consolidada por ativo.

## ADDED Requirements

### Requirement: Seleção e identificação da corretora

O sistema SHALL oferecer catálogo autenticado de corretoras e exigir uma identificação válida de corretora em novos registros. O histórico SHALL incluir a corretora de cada operação. A identificação do usuário SHALL continuar sendo obtida exclusivamente da sessão autenticada.

#### Scenario: Compra com corretora válida
- **WHEN** um usuário registra uma compra informando uma corretora ativa e os demais dados válidos
- **THEN** a operação é associada a esse usuário, ativo e corretora e retorna a identificação da instituição

#### Scenario: Corretora ausente ou inválida
- **WHEN** um novo registro não informa corretora ou informa identificação inexistente
- **THEN** o sistema rejeita o pedido com 400 sem gravação ou escolha automática de corretora

#### Scenario: Corretora inativa
- **WHEN** o usuário tenta comprar em corretora inativa
- **THEN** a compra é rejeitada
- **AND** uma venda de posição existente nessa corretora permanece permitida se respeitar o saldo

### Requirement: Saldo histórico por corretora e isolamento

O sistema SHALL validar o saldo por usuário, ativo e corretora em toda a sequência cronológica de operações, inclusive inserções retroativas e concorrentes. Um saldo positivo em outra corretora ou de outro usuário SHALL NOT permitir a venda.

#### Scenario: Saldo em instituição diferente
- **WHEN** o usuário possui 10 PETR4 na corretora A e 5 na B e tenta vender 6 na B
- **THEN** a venda é rejeitada com 422 e nenhum saldo ou histórico é modificado

#### Scenario: Venda retroativa ou concorrente
- **WHEN** uma venda produziria saldo negativo na corretora em qualquer ponto do histórico, inclusive após outra gravação concorrente
- **THEN** o registro é rejeitado atomicamente, preservando a ordenação por data e identificador

#### Scenario: Dados pertencem a outro usuário
- **WHEN** um usuário consulta posições ou tenta registrar operação usando informações de outra conta
- **THEN** as posições e a validação consideram somente o usuário autenticado

### Requirement: Preço médio gerencial e custo restante

O sistema SHALL calcular quantidade, preço médio ponderado e custo da posição restante por ativo e corretora com aritmética decimal. Compras alteram o preço médio; vendas parciais reduzem quantidade e custo sem alterar o preço médio daquela posição. Posições zeradas SHALL sair da listagem de posições abertas, mantendo o histórico.

#### Scenario: Compras e venda parcial na mesma corretora
- **WHEN** o usuário compra 10 unidades a R$ 20 e 10 a R$ 30 na mesma corretora e vende 5
- **THEN** a posição mostra quantidade 15, preço médio R$ 25 e valor investido R$ 375
- **AND** o preço obtido por cotação não altera esses custos

#### Scenario: Encerramento de posição
- **WHEN** a quantidade de uma posição por corretora chega a zero
- **THEN** ela deixa a carteira aberta mas suas compras e vendas continuam no histórico

### Requirement: Visão consolidada independente

O sistema SHALL preservar o cálculo consolidado pela sequência completa de operações do usuário para cada ativo. O preço médio consolidado SHALL NOT ser calculado como média simples de preços médios por corretora. Valores gerenciais por corretora SHALL ser identificados separadamente e SHALL NOT ser apresentados como apuração fiscal.

#### Scenario: Custos gerenciais diferem do consolidado após venda seletiva
- **WHEN** o usuário compra 10 unidades a R$ 20 na A e depois 10 a R$ 40 na B, e vende 5 na A
- **THEN** as posições gerenciais mostram A com 5 unidades, preço médio R$ 20 e custo R$ 100, e B com 10 unidades, preço médio R$ 40 e custo R$ 400
- **AND** o cálculo consolidado existente mostra 15 unidades, preço médio R$ 30 e custo R$ 450
- **AND** a interface não apresenta R$ 500 como custo consolidado equivalente, nem esconde a diferença entre as bases de cálculo

### Requirement: Preservação das operações sem corretora

Todas as operações anteriores à migração SHALL permanecer com usuário, ativo, tipo, valores, datas e identificadores originais e SHALL ser identificadas como `Corretora não informada`. Nenhuma instituição real SHALL ser inferida. Essa categoria SHALL aceitar somente vendas de seu saldo existente, nunca novas compras.

#### Scenario: Atualização de banco já utilizado
- **WHEN** o banco é atualizado com operações existentes
- **THEN** todas elas permanecem consultáveis com corretora não informada e os saldos consolidados anteriores são preservados

#### Scenario: Venda do saldo legado
- **WHEN** o usuário seleciona explicitamente `Corretora não informada` para vender uma posição antiga
- **THEN** apenas o saldo histórico dessa categoria é considerado
- **AND** o sistema rejeita uma nova compra nessa categoria

### Requirement: Consulta de cotação separada dos registros

O sistema SHALL disponibilizar posições e custos sem depender de cotação externa. A atualização de cotações SHALL ser explícita e consultar cada símbolo distinto uma única vez por avaliação, reutilizando o mesmo resultado nas linhas das corretoras. Fonte e horário de consulta SHALL estar visíveis, sem alegação de tempo real.

#### Scenario: Um ativo em duas corretoras
- **WHEN** a carteira contém PETR4 em duas corretoras e o usuário atualiza cotações
- **THEN** uma única cotação de PETR4 atende ambas as linhas nessa atualização

#### Scenario: Falha de avaliação
- **WHEN** a avaliação de cotações falha
- **THEN** o sistema mantém a carteira e seus custos disponíveis, marca cotações como indisponíveis e não apresenta avaliação parcial como completa
- **AND** uma resposta 503 não encerra a sessão

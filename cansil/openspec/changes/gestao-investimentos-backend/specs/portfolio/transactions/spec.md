## Purpose

Registrar operações imutáveis por usuário e derivar uma posição consistente, impedindo valores inválidos e vendas superiores ao saldo disponível.

## ADDED Requirements

### Requirement: Registro de compra
O sistema SHALL registrar compra com usuário autenticado, ativo, quantidade positiva, valor unitário positivo e data.

#### Scenario: Compra válida
- **WHEN** uma compra válida é enviada
- **THEN** a transação é persistida para o usuário autenticado

#### Scenario: Valor não positivo
- **WHEN** quantidade ou valor unitário não é positivo
- **THEN** a operação é rejeitada sem persistência

### Requirement: Venda limitada ao saldo
O sistema SHALL aceitar venda somente quando a quantidade não exceder o saldo anterior do ativo, usando ordem determinística por data e identificador.

#### Scenario: Venda parcial
- **WHEN** a quantidade vendida é menor ou igual ao saldo
- **THEN** a venda é persistida e reduz a quantidade sem alterar o preço médio remanescente

#### Scenario: Saldo insuficiente
- **WHEN** a quantidade vendida excede o saldo
- **THEN** a venda é rejeitada integralmente

### Requirement: Preço médio ponderado
O sistema MUST calcular compras por `(quantidade anterior × média anterior + quantidade comprada × preço) / quantidade resultante`; vendas reduzem somente a quantidade e posição zerada tem média zero.

#### Scenario: Duas compras
- **WHEN** são compradas 10 unidades a 20,00 e 5 unidades a 26,00
- **THEN** a posição contém 15 unidades com preço médio 22,00

#### Scenario: Venda parcial e total
- **WHEN** uma venda reduz parcialmente ou zera uma posição
- **THEN** a média é preservada na posição parcial e zerada na posição encerrada

### Requirement: Histórico de operações
O sistema SHALL listar somente as transações do usuário autenticado com ordenação definida.

#### Scenario: Listagem isolada
- **WHEN** um usuário solicita suas operações
- **THEN** nenhuma transação de outro usuário é retornada


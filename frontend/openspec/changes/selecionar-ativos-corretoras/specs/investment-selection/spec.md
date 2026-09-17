## ADDED Requirements

### Requirement: Seleção explícita de ativo e corretora
The frontend SHALL exigir que o usuário pesquise e selecione um ativo e uma corretora válidos antes de registrar compra ou venda.

#### Scenario: Texto alterado após seleção
- **WHEN** o usuário altera o texto da busca depois de selecionar um ativo
- **THEN** a seleção anterior é invalidada e o envio permanece bloqueado até nova seleção

#### Scenario: Venda iniciada pela carteira
- **WHEN** o usuário aciona a venda de uma posição por corretora
- **THEN** o formulário recebe símbolo e corretora, revalida ambos com dados autenticados e não preenche o preço negociado

### Requirement: Carteira profissional por corretora
The frontend SHALL exibir por padrão as posições abertas por corretora com as colunas, na ordem, Ativo, Corretora, Quantidade, Valor investido, Preço médio e Cotação atual.

#### Scenario: Mesmo ativo em duas corretoras
- **WHEN** o backend devolve duas posições do mesmo símbolo em corretoras distintas
- **THEN** a interface mantém duas linhas independentes identificadas pela chave composta de símbolo e corretora

#### Scenario: Alternância consolidada
- **WHEN** o usuário escolhe a visão consolidada
- **THEN** a interface usa os totais consolidados do backend, identifica `Todas as corretoras` e explica a diferença entre as bases gerenciais

### Requirement: Cotações sob demanda
The frontend SHALL buscar preços somente quando o usuário acionar explicitamente `Atualizar cotações`.

#### Scenario: Atualização bem-sucedida
- **WHEN** a avaliação consolidada retorna com sucesso
- **THEN** preço e fonte são mapeados por símbolo para todas as linhas e o horário local é identificado como horário da consulta

#### Scenario: Falha completa
- **WHEN** a avaliação falha
- **THEN** cotações anteriores são removidas, custos e quantidades permanecem visíveis e nenhuma resposta parcial é apresentada

### Requirement: Preservação da segurança e resiliência
The frontend SHALL manter o JWT somente em memória, enviar um único POST por confirmação e preservar as proteções de resultado incerto e expiração concorrente da sessão.

#### Scenario: Resultado de envio incerto
- **WHEN** o resultado do POST não pode ser determinado
- **THEN** o formulário continua bloqueado até histórico novo e confirmação explícita, sem repetição automática

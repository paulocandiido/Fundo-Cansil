## Purpose

Permitir selecionar códigos de ativos existentes sem digitação livre como única opção, preservando credenciais e evitando consultas de preço desnecessárias durante a busca.

## ADDED Requirements

### Requirement: Catálogo pesquisável autenticado

O sistema SHALL disponibilizar aos usuários autenticados uma busca paginada por código, normalizada para maiúsculas e com resultados ordenados e sem duplicatas. Os resultados SHALL identificar a atualização e eventual desatualização do catálogo e SHALL respeitar o formato de símbolo aceito pelas operações.

#### Scenario: Busca por parte do código
- **WHEN** o usuário autenticado pesquisa ` petr `
- **THEN** o sistema retorna somente códigos correspondentes a `PETR`, sem duplicatas, com total e informações de paginação
- **AND** não apresenta índices com pontuação como opções válidas para registrar uma operação

#### Scenario: Busca sem autenticação ou paginação inválida
- **WHEN** a busca é feita sem autenticação ou com paginação fora dos limites
- **THEN** o sistema retorna respectivamente 401 ou 400, sem consultar preços ou expor configurações privadas

### Requirement: Busca não equivale a consulta de cotação

A seleção e a busca de ativos SHALL NOT criar operações ou registros no histórico de cotações. O sistema SHALL informar que presença no catálogo não garante preço disponível no plano do provedor.

#### Scenario: Usuário percorre sugestões
- **WHEN** o usuário pesquisa códigos e seleciona um ativo
- **THEN** nenhum preço é solicitado só para a seleção e nenhuma consulta financeira é gravada
- **AND** nenhuma chave de provedor é enviada ao navegador

### Requirement: Reutilização do catálogo e continuidade de acesso

O sistema SHALL reutilizar o catálogo obtido para atender buscas sem chamar o provedor a cada termo digitado. Em indisponibilidade externa SHALL usar a última cópia válida, identificando-a como desatualizada; sem cópia válida SHALL retornar erro seguro. A indisponibilidade SHALL NOT impedir acesso à carteira e aos ativos já registrados pelo usuário.

#### Scenario: Várias buscas sobre o mesmo catálogo
- **WHEN** diferentes termos são pesquisados enquanto o catálogo está válido
- **THEN** a fonte externa não é consultada para cada termo

#### Scenario: Provedor indisponível após obtenção do catálogo
- **WHEN** a atualização do catálogo falha e existe uma cópia anterior válida
- **THEN** os resultados dessa cópia continuam disponíveis com aviso de desatualização

#### Scenario: Provedor indisponível sem cópia anterior
- **WHEN** a busca depende de um catálogo nunca obtido e o provedor falha
- **THEN** a busca retorna 503 com mensagem segura
- **AND** os ativos das posições do próprio usuário continuam selecionáveis pelo fluxo da carteira, sem apresentar uma lista vazia como catálogo completo

### Requirement: Respostas de preço correspondem ao ativo escolhido

O sistema SHALL impedir que uma cotação de outro símbolo, inclusive resolvida por alteração de código, seja silenciosamente atribuída ao ativo solicitado.

#### Scenario: Provedor devolve código diferente
- **WHEN** a cotação responde com símbolo diferente do selecionado pelo usuário
- **THEN** o sistema não cria uma operação ou consulta com código e preço incompatíveis
- **AND** informa indisponibilidade ou necessidade de conferir o código, sem reescrever silenciosamente o histórico

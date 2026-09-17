## Purpose

Fornecer cotações confiáveis por três fontes externas ordenadas, normalizando seus contratos e mantendo histórico auditável da fonte que respondeu.

## ADDED Requirements

### Requirement: Providers tipados e normalizados
O sistema SHALL implementar um contrato comum de cotação com providers separados para brapi, Alpha Vantage e HG Finance, cada qual mapeando sua resposta tipada para o mesmo DTO interno.

#### Scenario: Resposta brapi válida
- **WHEN** a brapi retorna resultado válido
- **THEN** seu provider extrai especificamente `results[0].data` e devolve o DTO interno normalizado

#### Scenario: Resposta de fallback válida
- **WHEN** Alpha Vantage ou HG Finance retorna uma cotação válida
- **THEN** o provider correspondente converte seu formato específico no mesmo DTO interno

### Requirement: Autenticação externa segura
O sistema MUST obter cada token de variável própria e enviar a autenticação exigida pela respectiva fonte sem registrar nem retornar o segredo.

#### Scenario: Requisição à brapi
- **WHEN** o provider brapi consulta um símbolo parametrizado
- **THEN** envia `Authorization: Bearer <BRAPI_TOKEN>` para `/api/v2/stocks/quote`

#### Scenario: Requisição aos fallbacks
- **WHEN** um provider de fallback é acionado
- **THEN** usa exclusivamente o token configurado para sua própria fonte

### Requirement: Fallback ordenado
O sistema SHALL tentar as fontes habilitadas na ordem brapi, Alpha Vantage e HG Finance, avançando quando houver timeout, erro de transporte, resposta não-2xx ou resposta 2xx sem cotação utilizável. Na operação temporária autorizada, somente brapi estará habilitada; os cenários com fallback se aplicam quando as respectivas fontes estiverem habilitadas.

#### Scenario: Fallbacks desativados
- **WHEN** Alpha e HG estão desativadas e brapi falha
- **THEN** nenhuma chamada às fontes desativadas ocorre e o erro identifica somente a fonte tentada, sem mudar o contrato REST

#### Scenario: Fonte primária bem-sucedida
- **WHEN** a brapi retorna cotação válida
- **THEN** o sistema devolve a cotação sem chamar os providers seguintes

#### Scenario: brapi falha e Alpha Vantage responde
- **WHEN** a brapi falha ou não contém o símbolo e a Alpha Vantage retorna dado válido
- **THEN** o sistema devolve a cotação da Alpha Vantage e não chama a HG Finance

#### Scenario: Duas primeiras fontes falham
- **WHEN** brapi e Alpha Vantage falham e a HG Finance responde corretamente
- **THEN** o sistema devolve a cotação da HG Finance

### Requirement: Falha total explícita
O sistema SHALL retornar erro agregado e específico quando nenhum provider produzir cotação, preservando causas úteis sem expor tokens ou reduzir o problema à última exceção.

#### Scenario: Todas as fontes falham
- **WHEN** os três providers falham ou não encontram o símbolo
- **THEN** o sistema retorna erro de cotação indisponível que identifica as fontes tentadas de forma segura

### Requirement: Histórico e observabilidade da fonte
O sistema SHALL registrar em log a fonte bem-sucedida e persistir usuário, ativo, valor, fonte e instante para cada consulta concluída.

#### Scenario: Consulta bem-sucedida por fallback
- **WHEN** uma cotação é obtida pela Alpha Vantage ou HG Finance
- **THEN** o log e o histórico identificam exatamente essa fonte

#### Scenario: Consulta sem sucesso
- **WHEN** todas as fontes falham
- **THEN** nenhum histórico de consulta bem-sucedida é persistido

#### Scenario: Listagem do histórico
- **WHEN** um usuário autenticado solicita seu histórico
- **THEN** recebe somente suas consultas, incluindo a fonte, em ordem decrescente de data e hora

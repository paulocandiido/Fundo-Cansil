## Purpose

Permitir cadastro verificável de corretoras por CNPJ e escolha de mercado por usuário, preservando operações existentes e impedindo vínculos incompatíveis.

## ADDED Requirements

### Requirement: Cadastro verificável
O sistema SHALL aceitar somente CNPJ válido e existente na consulta cadastral, obtendo a razão social da fonte externa sem confiar no nome enviado pelo navegador.

#### Scenario: Confirmação cadastral
- **WHEN** o usuário informa CNPJ e mercado válidos
- **THEN** o cadastro usa a razão social verificada e o CNPJ normalizado

#### Scenario: Falha da fonte
- **WHEN** a fonte falha, retorna identidade divergente ou não encontra o cadastro
- **THEN** nenhum cadastro é gravado e um erro seguro é exibido

### Requirement: Titularidade e mercado único
O sistema SHALL isolar corretoras por usuário, rejeitar CNPJ duplicado na mesma conta e manter o mercado BR ou USA imutável após cadastro. A existência cadastral não SHALL ser apresentada como autorização regulatória.

#### Scenario: Tentativa de mistura
- **WHEN** uma corretora USA é enviada para comprar um ativo BR ou o mesmo CNPJ é cadastrado novamente em outro mercado
- **THEN** o sistema rejeita a operação sem gravar dados

#### Scenario: Outro usuário
- **WHEN** um usuário envia identificador pertencente a outra conta
- **THEN** não consegue acessar, regularizar nem operar com essa corretora

### Requirement: Preservação do legado
O sistema SHALL preservar identificadores, valores, datas e saldos das transações anteriores. Corretoras antigas sem CNPJ devem aceitar somente venda de saldo próprio até regularização explícita, que preserva o mercado BR histórico.

#### Scenario: Regularização
- **WHEN** o usuário confirma o CNPJ de uma corretora legada da própria conta
- **THEN** os vínculos são preservados e a razão social é atualizada com os dados cadastrais verificados

### Requirement: Disponibilidade por mercado
O sistema SHALL permitir cadastrar corretoras USA e informar que operações USA estão indisponíveis com a atual fonte brapi/B3; BDRs devem continuar no mercado BR.

#### Scenario: Mercado USA
- **WHEN** o usuário escolhe corretora USA para operar
- **THEN** a interface informa a indisponibilidade e o backend bloqueia o registro sem consultar ou gravar um ativo BR como USA

### Requirement: Interface e erros
A interface SHALL oferecer consulta explícita, razão social não editável, escolha de mercado e listagem própria, invalidando consultas antigas quando o CNPJ muda e protegendo contra envio repetido.

#### Scenario: CNPJ alterado
- **WHEN** chega a resposta de uma consulta anterior após o campo mudar
- **THEN** a resposta não habilita cadastro com nome de outro CNPJ


## Purpose

Garantir cadastro e autenticação seguros, isolamento dos dados financeiros e ausência de dados pessoais ou credenciais nas respostas da API.

## ADDED Requirements

### Requirement: Identidade para integração do frontend
O sistema SHALL retornar somente id, nome e e-mail em GET `/api/usuarios/me`, derivados do usuário autenticado, e erros JSON para autenticação ausente ou inválida.

#### Scenario: Consulta de identidade
- **WHEN** o frontend envia JWT válido para `/api/usuarios/me`
- **THEN** recebe o DTO do principal sem CPF, senha ou hash

#### Scenario: Identidade sem autenticação
- **WHEN** a rota é chamada sem JWT válido
- **THEN** recebe HTTP 401 no formato StandardError

### Requirement: Cadastro seguro
O sistema SHALL cadastrar e-mail e CPF únicos e persistir a senha exclusivamente como hash BCrypt.

#### Scenario: Cadastro válido
- **WHEN** nome, e-mail, CPF e senha válidos e inéditos são enviados
- **THEN** o usuário é criado e a resposta omite CPF, senha e hash

#### Scenario: Identificador duplicado
- **WHEN** e-mail ou CPF já cadastrado é enviado
- **THEN** o sistema retorna conflito sem alterar o registro existente

### Requirement: Autenticação JWT
O sistema SHALL autenticar credenciais e emitir JWT assinado, com validade limitada e sem dados sensíveis nas claims.

#### Scenario: Login válido
- **WHEN** e-mail e senha corretos são enviados
- **THEN** o sistema retorna token utilizável nas rotas protegidas

#### Scenario: Login inválido
- **WHEN** qualquer credencial está incorreta
- **THEN** o sistema retorna erro genérico de autenticação

### Requirement: Isolamento por identidade
O sistema MUST derivar o usuário do JWT e restringir consultas, transações e carteira aos dados desse usuário.

#### Scenario: Acesso autenticado
- **WHEN** um usuário solicita carteira, operações ou consultas
- **THEN** recebe somente seus próprios dados

#### Scenario: Acesso sem token válido
- **WHEN** uma rota protegida é acessada sem JWT válido
- **THEN** o sistema retorna HTTP 401 sem dados financeiros

### Requirement: Ausência de dados sensíveis
O sistema MUST usar DTOs de saída que jamais exponham CPF, senha, hash, segredos JWT, tokens externos ou credenciais do banco.

#### Scenario: Serialização de resposta
- **WHEN** qualquer endpoint produz resposta
- **THEN** nenhum campo sensível integra o payload

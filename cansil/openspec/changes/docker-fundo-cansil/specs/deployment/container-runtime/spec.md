## Purpose

Executar o Fundo Cansil em serviços independentes no Docker, com identidade consistente e preservação do banco já utilizado pelo usuário.

## ADDED Requirements

### Requirement: Organização e identidade
O sistema SHALL ser apresentado como Fundo Cansil no site e agrupar no Docker os contêineres backend, frontend e postgres sob fundo-cansil.

#### Scenario: Inicialização
- **WHEN** o ambiente Docker é iniciado com configuração válida
- **THEN** os três contêineres aparecem no grupo fundo-cansil e o site/títulos exibem Fundo Cansil

### Requirement: Acesso integrado
O frontend Docker SHALL acessar a API pela mesma origem, preservar autenticação e permitir abrir diretamente as rotas Angular sem confundir falhas de API/arquivos com HTML de sucesso.

#### Scenario: Rota protegida e API
- **WHEN** o usuário abre uma rota Angular diretamente ou solicita uma API protegida sem token
- **THEN** a rota carrega a aplicação e a API mantém sua rejeição autenticada, sem expor nomes internos/credenciais ao navegador

#### Scenario: Reinício independente
- **WHEN** o backend é recriado mantendo o frontend em execução
- **THEN** o frontend volta a acessar a API sem depender permanentemente do endereço IP anterior

### Requirement: Persistência e segurança
A troca de nome SHALL preservar o banco e seus registros, não criar silenciosamente um banco substituto vazio e não remover dados de serviços fora do projeto.

#### Scenario: Migração operacional
- **WHEN** o ambiente muda do nome antigo para fundo-cansil
- **THEN** o volume existente é reutilizado com backup prévio e os registros são conferidos antes/depois

### Requirement: Desenvolvimento preservado
O projeto SHALL continuar oferecendo execução Angular local para desenvolvimento e instruções claras sobre portas, rebuild, inicialização e preservação de dados.

#### Scenario: Desenvolvimento local
- **WHEN** o usuário escolhe npm start em vez do frontend Docker
- **THEN** pode manter backend e banco no Docker, com instruções para evitar conflito na porta do frontend


# Mercado e moeda automática

Entrega de 08/09/2026.

- O formulário de compras e vendas não possui campo para escolher moeda.
- O usuário seleciona o mercado: `BR` usa `BRL` e `USA` usa `USD` automaticamente.
- O POST `/api/transacoes` recebe `mercado`, nunca `moeda`; o backend realiza o mapeamento e rejeita ativo incompatível com 422.
- Corretoras continuam livres para operar nos dois mercados.
- Em vendas, ativos e corretoras são filtrados pela moeda da posição para impedir mistura entre BRL e USD.
- Atalhos da carteira e de cotação transportam o mercado correspondente.
- O catálogo pesquisável da Brapi é usado para BR. Em USA, o ticker digitado é validado pelo provedor de cotação no registro.

Validação automatizada: 69 testes Angular, 191 testes Maven (190 executados e 1 condicional ignorado) e 8 testes do cliente TypeScript de referência, todos sem falhas. A implantação Docker não exige migração de banco, pois o contrato mudou apenas na entrada da API e na interface.

Os três contêineres ficaram saudáveis após a atualização. A assinatura de todas as tabelas de negócio e sequências permaneceu idêntica antes e depois da implantação.

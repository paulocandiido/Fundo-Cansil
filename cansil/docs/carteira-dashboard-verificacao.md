# Dashboard da carteira

Entrega de 08/09/2026.

- A área principal da carteira foi reorganizada em resumo, posições atuais, filtros, consulta por ID e extrato.
- As posições e o preço médio continuam vindo das regras consolidadas do backend. Vendas reduzem o saldo sem alterar o custo médio das unidades restantes.
- O resumo usa média ponderada: `soma(quantidade × preço) ÷ soma das quantidades`. Assim, 20 cotas a 316,22 resultam em média 316,22; 5 cotas a 5,00 resultam em média 5,00.
- O extrato calcula valor total, preço médio histórico e lucro/prejuízo realizado em vendas.
- Filtros de compra/venda e ativo funcionam em conjunto sobre os registros reais da conta.
- A consulta por ID usa o histórico autenticado já carregado e trata campo vazio, ID inválido e operação inexistente.
- BRL e USD nunca são somados; os indicadores financeiros são separados por moeda.
- “Atualizar” recarrega posições e operações. “Atualizar cotações” permanece uma ação separada e explícita.
- A aba e a rota de Avaliação foram removidas; sua consulta de preços foi incorporada à carteira.
- O layout usa cards compactos, tabelas com cabeçalho azul, badges e rolagem horizontal responsiva.

Cobertura específica: cálculo após venda, resultado realizado, filtros combinados, consulta por ID e separação entre moedas.

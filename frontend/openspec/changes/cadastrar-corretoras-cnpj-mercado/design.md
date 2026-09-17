## Context

Ver proposal.md. As corretoras existentes são globais e as transações usam seu identificador; ativos atuais pertencem ao mercado brasileiro. O frontend está em raiz local irmã.

## Goals / Non-Goals

**Goals:** validar identidade, isolar o cadastro por usuário, fixar mercado, preservar legado e comunicar cobertura real.

**Non-Goals:** certificar autorização da corretora, executar ordens reais, contratar outro provedor, liberar ações USA sem dados compatíveis, editar ou apagar transações.

## Decisions

- Corretora recebe usuario_id, cnpj, mercado e verificado_em. Unique(usuario_id,cnpj) e lock do usuário impedem duplicidades concorrentes; codigo interno usa UUID. O CNPJ não contém mercado.
- BrasilAPI /api/cnpj/v1/{cnpj} via backend com timeout, resposta tipada mínima e validação da identidade. GET autenticado de consulta e POST de cadastro com consulta novamente no servidor; somente nome real e status são persistidos. Não armazenar sócios/endereço.
- CNPJ: aceitar numérico formatado/sem formato e preparar validação alfanumérica de 12 posições com dois dígitos verificadores. Se a fonte não atender o formato, indicar indisponibilidade sem cadastro fictício.
- POST /api/corretoras recebe {cnpj,mercado,corretoraLegadaId?}. O id opcional regulariza uma corretora própria pendente mantendo BR. Não há endpoint para troca de mercado.
- Migração 005 copia cada corretora global usada para seu usuário, religa transações e desativa registros globais. Não infere CNPJ. Os ids das operações, quantidades, preços e datas não mudam.
- Operações buscam corretora do usuário e exigem mercado BR antes de cotação/gravação. Não introduzir preço em dólar em posição consolidada em reais.
- UI Corretoras com consulta e razão social somente leitura, mercado e regularização. Compras exigem corretora cadastrada ativa. Saldo legado pode ser vendido.

## Risks / Trade-offs

- Fonte pública pode estar indisponível ou defasada → erro explícito e nenhum cadastro inventado; verificado_em identifica consulta.
- Mercado USA sem fonte → cadastro permitido e operação bloqueada com explicação.
- Corretoras antigas não têm CNPJ → regularização é decisão explícita do dono, sem atribuição automática.

## Migration Plan

Validar migração e isolamento com dados fictícios em H2/PostgreSQL. Executar Java, Angular e build. Atualizar Docker local com backup novo e preservar volume. Registrar resultados e limitações. Não arquivar/commitar automaticamente.


-- Somente leitura. Compara os dados anteriores à migração 006 sem expor registros.
-- Ignora apenas as duas colunas novas e os metadados do Liquibase.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
DO $$
DECLARE
    tabela record;
    total bigint;
    assinatura text;
    expressao text;
BEGIN
    FOR tabela IN SELECT tablename FROM pg_tables WHERE schemaname = 'public'
        AND tablename NOT IN ('databasechangelog', 'databasechangeloglock') ORDER BY tablename LOOP
        expressao := CASE tabela.tablename
            WHEN 'ativo' THEN 'to_jsonb(t) - ''moeda'''
            WHEN 'corretora' THEN 'to_jsonb(t) - ''removida'''
            ELSE 'to_jsonb(t)' END;
        EXECUTE format('SELECT count(*), md5(coalesce(string_agg((%s)::text, chr(10) ORDER BY (%s)::text), '''')) FROM public.%I t', expressao, expressao, tabela.tablename)
            INTO total, assinatura;
        RAISE NOTICE '%|%|%', tabela.tablename, total, assinatura;
    END LOOP;
END $$;
SELECT sequencename, last_value FROM pg_sequences WHERE schemaname = 'public' ORDER BY sequencename;
COMMIT;

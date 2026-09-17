-- Conferência somente leitura: retorna contagens/assinaturas, nunca linhas pessoais.
-- Executar antes e depois da troca de contêiner, com gravações da aplicação paradas.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
DO $$
DECLARE
    tabela record;
    total bigint;
    assinatura text;
BEGIN
    FOR tabela IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename LOOP
        EXECUTE format('SELECT count(*), md5(coalesce(string_agg(to_jsonb(t)::text, chr(10) ORDER BY to_jsonb(t)::text), '''')) FROM public.%I t', tabela.tablename)
            INTO total, assinatura;
        RAISE NOTICE '%|%|%', tabela.tablename, total, assinatura;
    END LOOP;
END $$;
SELECT sequencename, last_value FROM pg_sequences WHERE schemaname = 'public' ORDER BY sequencename;
COMMIT;

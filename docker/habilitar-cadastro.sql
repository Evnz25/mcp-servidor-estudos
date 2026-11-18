-- Pode ser reaplicado em volumes existentes. Preserva o nome do usuário já configurado no Compose.
BEGIN;
GRANT INSERT ON TABLE public.questao TO leitor_estudo;
GRANT USAGE ON SEQUENCE public.questao_id_seq TO leitor_estudo;
COMMIT;

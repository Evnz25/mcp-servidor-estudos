-- O entrypoint do PostgreSQL executa este arquivo após criar a tabela questao.
\getenv senha_leitor BANCO_SENHA
BEGIN;

CREATE ROLE leitor_estudo LOGIN PASSWORD :'senha_leitor';
GRANT USAGE ON SCHEMA public TO leitor_estudo;
GRANT SELECT ON TABLE questao TO leitor_estudo;

INSERT INTO questao (enunciado, alternativas, gabarito)
VALUES ('Qual estrutura de dados segue a ordem FIFO?',
        ARRAY['Pilha', 'Fila', 'Árvore binária'], 2);

COMMIT;

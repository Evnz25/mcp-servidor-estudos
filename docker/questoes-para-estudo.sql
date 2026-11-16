BEGIN;

-- Permite reaplicar a carga em um volume existente, inclusive em execuções simultâneas.
LOCK TABLE questao IN SHARE ROW EXCLUSIVE MODE;

INSERT INTO questao (enunciado, alternativas, gabarito)
SELECT carga.enunciado, carga.alternativas, carga.gabarito
FROM (VALUES
    ('De acordo com o protocolo TCP, assinale a alternativa INCORRETA:',
     ARRAY[
         'O protocolo TCP apresenta como característica a transferência de dados confiáveis fim a fim.',
         'Quando cada segmento for transmitido, será adicionado um checksum e, quando estes são recebidos, eles são verificados. Se forem danificados, os pacotes são descartados e retransmitidos pela origem.',
         'O protocolo TCP é fundamental para as comunicações da internet desde os primórdios da rede.',
         'Para estabelecer uma conexão, o protocolo TCP usa handshake de quatro vias (SYN, SYN-ACK, ACK e ACK-SYN).',
         'O protocolo TCP esta na camada 4 do Modelo OSI.'
     ], 4),
    ('Analise as proposições abaixo e, em seguida, assinale a alternativa correta:
I. O Modelo OSI possui sete camadas, iniciando sua hierarquia pela camada mais baixa, chamada de camada física, até a sua camada superior, denominada camada de apresentação.
II. A camada de transporte garante que as mensagens sejam entregues sem erros, em sequência e sem perdas ou duplicações.
III. A camada de transporte realiza o roteamento entre redes, controla o tráfego e fornece o mapeamento de endereços lógicos e físicos.',
     ARRAY[
         'I, II e III estão corretas.',
         'Apenas II está correta.',
         'Apenas III está correta.',
         'Apenas I e III estão corretas.',
         'Apenas I e II estão corretas.'
     ], 2),
    ('Em relação às técnicas de programação, pode-se afirmar que são métodos, ou algoritmos, conhecidos de ordenação de dados por troca:',
     ARRAY[
         'Ordenação shell e Hashing.',
         'Busca por ordenação e Ordenação em camadas.',
         'Quicksort e Hashing.',
         'Hashing e Bubble sort.',
         'Bubble sort e Quicksort.'
     ], 5),
    ('Considere a seguinte tabela de um banco de dados relacional:
Venda (IDLoja, NomeLoja, ValorVenda, NumItens)
A consulta SQL que obtém o valor total em vendas, por loja, é:',
     ARRAY[
         'SELECT IDLoja, SUM (ValorVenda) FROM Venda ORDER BY IDLoja',
         'SELECT IDLoja, Count (ValorVenda) FROM Venda ORDER BY IDLoja',
         'SELECT IDLoja, SUM (ValorVenda) FROM Venda',
         'SELECT IDLoja, SUM (ValorVenda) FROM Venda GROUP BY IDLoja',
         'SELECT IDLoja, + (ValorVenda) FROM Venda WHERE IDLoja < > NULL'
     ], 4),
    ('Na linguagem SQL, o argumento que é responsável pela coleta de dados espalhados em diferentes tabelas de um banco de dados, é:',
     ARRAY[
         'select.',
         'group by.',
         'distinct.',
         'join.',
         'between.'
     ], 4),
    ('Em programação orientada a objetos, existe um recurso utilizado para definir uma espécie de “contrato” que deve ser seguido pela classe que o implementa. Quando uma classe implementa esse recurso, ela se compromete a realizar todos os comportamentos que o recurso disponibiliza. Esse recurso é chamado de:',
     ARRAY[
         'Herança.',
         'Encapsulamento.',
         'Polimorfismo.',
         'Interface.',
         'Visibilidade.'
     ], 4),
    ('QUESTÃO 37 – O .NET fornece um ambiente em tempo de execução que executa o código e fornece serviços que facilitam o processo de desenvolvimento. Como é chamado esse ambiente?',
     ARRAY[
         'JVM (Java Virtual Machine).',
         'CLR (Common Language Runtime).',
         'REC (Runtime Environment Compiler).',
         'RVM (Runtime Virtual Machine).',
         'CRL (Compiler Runtime Library).'
     ], 2)
) AS carga(enunciado, alternativas, gabarito)
WHERE NOT EXISTS (
    SELECT 1 FROM questao
    WHERE questao.enunciado = carga.enunciado AND questao.alternativas = carga.alternativas
);

COMMIT;

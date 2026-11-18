package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.UUID;

/** Mantém o sorteio e as respostas de uma única rodada. */
public final class Simulado {
   public static final int TOTAL = 5;
   public record Pergunta(String rodada, int numero, int total, QuestaoParaEstudo questao) {}
   public record Placar(int total, int acertos, int erros) {}

   private final String rodada = UUID.randomUUID().toString();
   private final List<QuestaoComGabarito> questoes;
   private int respondidas;
   private int ultimaAlternativa;
   private int acertos;

   /** Exige um sorteio completo antes de abrir a rodada. */
   public Simulado(List<QuestaoComGabarito> questoes) {
      if (questoes.size() != TOTAL) {
         throw new IllegalArgumentException("São necessárias cinco questões cadastradas para iniciar o simulado.");
      }
      this.questoes = List.copyOf(questoes);
   }

   /** Identifica a rodada e mostra os dados públicos da questão solicitada. */
   public Pergunta questaoAtual() {
      int numero = respondidas + 1;
      return new Pergunta(rodada, numero, TOTAL, questoes.get(numero - 1).questao());
   }

   /** Registra a escolha uma vez e permite repetir a última chamada se a resposta se perder. */
   public synchronized Object responder(int numero, int alternativa) {
      if (numero == respondidas + 1 && numero <= TOTAL) {
         var correcao = questoes.get(numero - 1).corrigir(alternativa);
         respondidas++;
         ultimaAlternativa = alternativa;
         if (correcao.acertou()) acertos++;
      } else if (respondidas == 0 || numero != respondidas || ultimaAlternativa != alternativa) {
         throw new IllegalArgumentException("Responda à questão atual. Cada questão aceita uma única resposta.");
      }
      if (respondidas == TOTAL) {
         var resultado = consultarResultado();
         return new Placar(resultado.total(), resultado.acertos(), resultado.erros());
      }
      return questaoAtual();
   }

   /** Lê os contadores de forma atômica, sem avançar a rodada nem divulgar o gabarito. */
   public synchronized ResultadoSimulado consultarResultado() {
      return new ResultadoSimulado(rodada, respondidas == TOTAL, TOTAL,
                                    respondidas, acertos, respondidas - acertos);
   }
}

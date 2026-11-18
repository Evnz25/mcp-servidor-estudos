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
      var registro = prepararResposta(numero, alternativa);
      if (numero == respondidas + 1 && numero <= TOTAL) {
         respondidas++;
         ultimaAlternativa = alternativa;
         if (registro.acertou()) acertos++;
      }
      if (respondidas == TOTAL) {
         var resultado = consultarResultado();
         return new Placar(resultado.total(), resultado.acertos(), resultado.erros());
      }
      return questaoAtual();
   }

   /** Valida a resposta sem avançar a rodada, permitindo salvar no arquivo antes de alterar o estado. */
   public synchronized RegistroResposta prepararResposta(int numero, int alternativa) {
      boolean nova = numero == respondidas + 1 && numero <= TOTAL;
      boolean repetida = respondidas > 0 && numero == respondidas && ultimaAlternativa == alternativa;
      if (!nova && !repetida) {
         throw new IllegalArgumentException("Responda à questão atual. Cada questão aceita uma única resposta.");
      }
      var questao = questoes.get(numero - 1);
      var correcao = questao.corrigir(alternativa);
      return new RegistroResposta("simulado:" + rodada + ":" + numero, questao.questao().id(), alternativa,
                                   correcao.acertou());
   }

   /** Lê os contadores de forma atômica, sem avançar a rodada nem divulgar o gabarito. */
   public synchronized ResultadoSimulado consultarResultado() {
      return new ResultadoSimulado(rodada, respondidas == TOTAL, TOTAL,
                                    respondidas, acertos, respondidas - acertos);
   }
}

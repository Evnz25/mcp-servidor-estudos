package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.UUID;

//Classe que define o simulado, é possivel aumentar a quantidade de rodadas no simulado pela váriavel TOTAL
public final class Simulado {
   public static final int TOTAL = 5;

   //utiliza um id para identificar a rodada
   private final String rodada = UUID.randomUUID().toString();
   private final List<Questao> questoes;
   private int respondidas;
   private int acertos;

   public Simulado(List<Questao> questoes) {
      this.questoes = questoes;
   }

   //Cria uma nova rodada para o simulado
   public RodadaSimulado questaoAtual() {
      int numero = respondidas + 1;
      return new RodadaSimulado(rodada, numero, TOTAL, questoes.get(numero - 1).paraEstudo());
   }

   //Registra se o usuário acertou para o placar, se for a ultima pergunta finaliza o simulado
   public synchronized Object responder(int numero, int alternativa) {
      var correcao = corrigirResposta(numero, alternativa);
      respondidas++;
      if (correcao.isAcertou()) acertos++;
      if (respondidas == TOTAL) {
         return consultarResultado();
      }
      return questaoAtual();
   }

   //Corrige a alternativa da questão informada
   public synchronized Correcao corrigirResposta(int numero, int alternativa) {
      var questao = questoes.get(numero - 1);
      return questao.corrigir(alternativa);
   }

   //Gera o relatório para o resultado do simulado
   public synchronized ResultadoSimulado consultarResultado() {
      return new ResultadoSimulado(rodada, TOTAL, acertos, TOTAL - acertos);
   }
}

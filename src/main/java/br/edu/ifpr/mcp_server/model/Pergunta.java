package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

/** Questão atual e identificação de uma rodada de simulado. */
public class Pergunta {
   private String rodada;
   private int numero;
   private int total;
   private QuestaoParaEstudo questao;

   public Pergunta() {}

   public Pergunta(String rodada, int numero, int total, QuestaoParaEstudo questao) {
      setRodada(rodada);
      setNumero(numero);
      setTotal(total);
      setQuestao(questao);
   }

   public String getRodada() { return rodada; }

   public void setRodada(String rodada) {
      this.rodada = rodada;
   }

   public int getNumero() { return numero; }

   public void setNumero(int numero) {
      this.numero = numero;
   }

   public int getTotal() { return total; }

   public void setTotal(int total) {
      this.total = total;
   }

   public QuestaoParaEstudo getQuestao() { return questao; }

   public void setQuestao(QuestaoParaEstudo questao) {
      this.questao = questao;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [rodada=" + rodada + ", numero=" + numero + ", total=" + total + ", questao=" + questao + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      Pergunta other = (Pergunta) obj;
      return Objects.equals(rodada, other.rodada)
            && Objects.equals(numero, other.numero)
            && Objects.equals(total, other.total)
            && Objects.equals(questao, other.questao);
   }

   @Override
   public int hashCode() {
      return Objects.hash(rodada, numero, total, questao);
   }
}

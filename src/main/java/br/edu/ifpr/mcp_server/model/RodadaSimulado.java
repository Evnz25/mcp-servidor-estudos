package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

//Modelo da classe que será utilizado para a ferramenta de simulado com a informações necessárias para auxiliar ai IA saber em qual rodada do simulado está
public class RodadaSimulado {
   private String rodada;
   private int numero;
   private int total;
   private QuestaoParaEstudo questao;

   public RodadaSimulado() {}

   public RodadaSimulado(String rodada, int numero, int total, QuestaoParaEstudo questao) {
      this.rodada = rodada;
      this.numero = numero;
      this.total = total;
      this.questao = questao;
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
   public boolean equals(Object object) {
      if (this == object) return true;
      if (object == null) return false;
      if (getClass() != object.getClass()) return false;
      RodadaSimulado other = (RodadaSimulado) object;
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

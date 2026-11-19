package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

/** ConcreteDecorator: acrescenta o gabarito e o comportamento de corrigir. */
public class QuestaoComGabarito extends QuestaoDecorator {
   private int gabarito;

   public QuestaoComGabarito(Questao questao, int gabarito) {
      super(questao);
      this.gabarito = gabarito;
   }

   public int getGabarito() { return gabarito; }

   public void setGabarito(int gabarito) {
      this.gabarito = gabarito;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [questao=" + questao + ", gabarito=" + gabarito + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      QuestaoComGabarito other = (QuestaoComGabarito) obj;
      return Objects.equals(questao, other.questao)
            && Objects.equals(gabarito, other.gabarito);
   }

   @Override
   public int hashCode() {
      return Objects.hash(questao, gabarito);
   }

   /** Compara a escolha com o gabarito usando posições que começam em 1. */
   public Correcao corrigir(int escolha) {
      int quantidade = getAlternativas().size();
      if (escolha < 1 || escolha > quantidade) {
         throw new IllegalArgumentException("Escolha uma alternativa entre 1 e " + quantidade + ".");
      }
      return new Correcao(getId(), getEnunciado(), escolha, gabarito,
                         escolha == gabarito);
   }
}

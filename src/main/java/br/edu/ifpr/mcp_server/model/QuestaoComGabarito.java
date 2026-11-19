package br.edu.ifpr.mcp_server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Associa o gabarito à sequência de alternativas exibida no sorteio. */
public class QuestaoComGabarito {
   private QuestaoParaEstudo questao;
   private int gabarito;

   public QuestaoComGabarito() {}

   public QuestaoComGabarito(QuestaoParaEstudo questao, int gabarito) {
      this.questao = questao;
      this.gabarito = gabarito;
   }

   public QuestaoParaEstudo getQuestao() { return questao; }

   public void setQuestao(QuestaoParaEstudo questao) {
      this.questao = questao;
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
      int quantidade = questao.getAlternativas().size();
      if (escolha < 1 || escolha > quantidade) {
         throw new IllegalArgumentException("Escolha uma alternativa entre 1 e " + quantidade + ".");
      }
      var alternativas = new ArrayList<AlternativaCorrigida>(quantidade);
      for (int indice = 0; indice < quantidade; indice++) {
         int posicao = indice + 1;
         alternativas.add(new AlternativaCorrigida(posicao, questao.getAlternativas().get(indice),
                                                   posicao == gabarito));
      }
      return new Correcao(questao.getId(), questao.getEnunciado(), escolha, gabarito,
                         escolha == gabarito, List.copyOf(alternativas));
   }
}

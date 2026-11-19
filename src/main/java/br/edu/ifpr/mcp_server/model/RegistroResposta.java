package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Uma resposta contabilizada no desempenho, mesmo quando a questão se repete. */
@JsonIgnoreProperties("tentativa") // Permite ler o histórico salvo pelo formato anterior.
public class RegistroResposta {
   private long questao;
   private int alternativa;
   private boolean acertou;

   public RegistroResposta() {}

   @JsonCreator
   public RegistroResposta(@JsonProperty("questao") long questao,
                          @JsonProperty("alternativa") int alternativa,
                          @JsonProperty("acertou") boolean acertou) {
      setQuestao(questao);
      setAlternativa(alternativa);
      setAcertou(acertou);
   }

   public long getQuestao() { return questao; }

   public void setQuestao(long questao) {
      if (questao < 1) {
         throw new IllegalArgumentException("Registro de resposta inválido.");
      }
      this.questao = questao;
   }

   public int getAlternativa() { return alternativa; }

   public void setAlternativa(int alternativa) {
      if (alternativa < 1) {
         throw new IllegalArgumentException("Registro de resposta inválido.");
      }
      this.alternativa = alternativa;
   }

   public boolean isAcertou() { return acertou; }

   public void setAcertou(boolean acertou) {
      this.acertou = acertou;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [questao=" + questao + ", alternativa=" + alternativa + ", acertou=" + acertou + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      RegistroResposta other = (RegistroResposta) obj;
      return Objects.equals(questao, other.questao)
            && Objects.equals(alternativa, other.alternativa)
            && Objects.equals(acertou, other.acertou);
   }

   @Override
   public int hashCode() {
      return Objects.hash(questao, alternativa, acertou);
   }
}

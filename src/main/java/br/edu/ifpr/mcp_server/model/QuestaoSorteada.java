package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** ConcreteDecorator: acrescenta a identificação da tentativa ao sorteio. */
public class QuestaoSorteada extends QuestaoDecorator {
   private String tentativa;

   public QuestaoSorteada(Questao questao, String tentativa) {
      super(questao);
      this.tentativa = tentativa;
   }

   /** Preserva o JSON plano usado pelo cliente, sem expor o componente envolvido. */
   @JsonCreator
   public QuestaoSorteada(@JsonProperty("id") long id,
                         @JsonProperty("enunciado") String enunciado,
                         @JsonProperty("alternativas") List<String> alternativas,
                         @JsonProperty("tentativa") String tentativa) {
      this(new QuestaoParaEstudo(id, enunciado, alternativas), tentativa);
   }

   public String getTentativa() { return tentativa; }

   public void setTentativa(String tentativa) {
      this.tentativa = tentativa;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [id=" + getId() + ", enunciado=" + getEnunciado() + ", alternativas=" + getAlternativas() + ", tentativa=" + tentativa + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      QuestaoSorteada other = (QuestaoSorteada) obj;
      return Objects.equals(getId(), other.getId())
            && Objects.equals(getEnunciado(), other.getEnunciado())
            && Objects.equals(getAlternativas(), other.getAlternativas())
            && Objects.equals(tentativa, other.tentativa);
   }

   @Override
   public int hashCode() {
      return Objects.hash(getId(), getEnunciado(), getAlternativas(), tentativa);
   }
}

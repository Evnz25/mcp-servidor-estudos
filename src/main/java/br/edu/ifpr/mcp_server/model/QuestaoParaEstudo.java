package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

//modelo de dado que o usuário recebe antes de responder a pergunta
public class QuestaoParaEstudo {
   private long id;
   private String enunciado;
   private List<String> alternativas;

   public QuestaoParaEstudo() {}

   @JsonCreator
   public QuestaoParaEstudo(@JsonProperty("id") long id,
                           @JsonProperty("enunciado") String enunciado,
                           @JsonProperty("alternativas") List<String> alternativas) {
      this.id = id;
      this.enunciado = enunciado;
      setAlternativas(alternativas);
   }

   public long getId() { return id; }
   public void setId(long id) {
      this.id = id;
   }

   public String getEnunciado() { return enunciado; }
   public void setEnunciado(String enunciado) {
      this.enunciado = enunciado;
   }

   public List<String> getAlternativas() { return alternativas; }
   public void setAlternativas(List<String> alternativas) {
      this.alternativas = List.copyOf(alternativas);
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [id=" + id + ", enunciado=" + enunciado + ", alternativas=" + alternativas + "]";
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) return true;
      if (object == null || getClass() != object.getClass()) return false;
      QuestaoParaEstudo other = (QuestaoParaEstudo) object;
      return Objects.equals(id, other.id)
         && Objects.equals(enunciado, other.enunciado)
         && Objects.equals(alternativas, other.alternativas);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, enunciado, alternativas);
   }
}

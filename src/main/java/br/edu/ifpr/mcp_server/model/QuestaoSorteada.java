package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.Objects;

/** Dados públicos do sorteio e identificador usado ao enviar a resposta. */
public class QuestaoSorteada {
   private long id;
   private String enunciado;
   private List<String> alternativas;
   private String tentativa;

   public QuestaoSorteada() {}

   public QuestaoSorteada(long id, String enunciado, List<String> alternativas, String tentativa) {
      setId(id);
      setEnunciado(enunciado);
      setAlternativas(alternativas);
      setTentativa(tentativa);
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
      this.alternativas = alternativas;
   }

   public String getTentativa() { return tentativa; }

   public void setTentativa(String tentativa) {
      this.tentativa = tentativa;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [id=" + id + ", enunciado=" + enunciado + ", alternativas=" + alternativas + ", tentativa=" + tentativa + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      QuestaoSorteada other = (QuestaoSorteada) obj;
      return Objects.equals(id, other.id)
            && Objects.equals(enunciado, other.enunciado)
            && Objects.equals(alternativas, other.alternativas)
            && Objects.equals(tentativa, other.tentativa);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, enunciado, alternativas, tentativa);
   }
}

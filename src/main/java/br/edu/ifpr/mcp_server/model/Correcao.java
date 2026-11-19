package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.Objects;

/** Resultado da resposta com o gabarito e as alternativas corrigidas. */
public class Correcao {
   private long id;
   private String enunciado;
   private int alternativaEscolhida;
   private int gabarito;
   private boolean acertou;
   private List<AlternativaCorrigida> alternativas;

   public Correcao() {}

   public Correcao(long id, String enunciado, int alternativaEscolhida, int gabarito, boolean acertou, List<AlternativaCorrigida> alternativas) {
      this.id = id;
      this.enunciado = enunciado;
      this.alternativaEscolhida = alternativaEscolhida;
      this.gabarito = gabarito;
      this.acertou = acertou;
      this.alternativas = alternativas;
   }

   public long getId() { return id; }

   public void setId(long id) {
      this.id = id;
   }

   public String getEnunciado() { return enunciado; }

   public void setEnunciado(String enunciado) {
      this.enunciado = enunciado;
   }

   public int getAlternativaEscolhida() { return alternativaEscolhida; }

   public void setAlternativaEscolhida(int alternativaEscolhida) {
      this.alternativaEscolhida = alternativaEscolhida;
   }

   public int getGabarito() { return gabarito; }

   public void setGabarito(int gabarito) {
      this.gabarito = gabarito;
   }

   public boolean isAcertou() { return acertou; }

   public void setAcertou(boolean acertou) {
      this.acertou = acertou;
   }

   public List<AlternativaCorrigida> getAlternativas() { return alternativas; }

   public void setAlternativas(List<AlternativaCorrigida> alternativas) {
      this.alternativas = alternativas;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [id=" + id + ", enunciado=" + enunciado + ", alternativaEscolhida=" + alternativaEscolhida + ", gabarito=" + gabarito + ", acertou=" + acertou + ", alternativas=" + alternativas + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false
      if (getClass() != obj.getClass()) return false;
      Correcao other = (Correcao) obj;
      return Objects.equals(id, other.id)
            && Objects.equals(enunciado, other.enunciado)
            && Objects.equals(alternativaEscolhida, other.alternativaEscolhida)
            && Objects.equals(gabarito, other.gabarito)
            && Objects.equals(acertou, other.acertou)
            && Objects.equals(alternativas, other.alternativas);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, enunciado, alternativaEscolhida, gabarito, acertou, alternativas);
   }
}

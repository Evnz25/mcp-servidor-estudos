package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

//Modelo da classe que possui a questão com o gabarito e a alternativa que o usário escolheu e guarda a informação se ele acertou ou errou
public class Correcao {
   private long id;
   private String enunciado;
   private int alternativaEscolhida;
   private int gabarito;
   private boolean acertou;

   public Correcao() {}

   public Correcao(long id, String enunciado, int alternativaEscolhida, int gabarito, boolean acertou) {
      this.id = id;
      this.enunciado = enunciado;
      this.alternativaEscolhida = alternativaEscolhida;
      this.gabarito = gabarito;
      this.acertou = acertou;
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

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [id=" + id + ", enunciado=" + enunciado + ", alternativaEscolhida=" + alternativaEscolhida + ", gabarito=" + gabarito + ", acertou=" + acertou + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Correcao other = (Correcao) obj;
      return Objects.equals(id, other.id)
            && Objects.equals(enunciado, other.enunciado)
            && Objects.equals(alternativaEscolhida, other.alternativaEscolhida)
            && Objects.equals(gabarito, other.gabarito)
            && Objects.equals(acertou, other.acertou);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, enunciado, alternativaEscolhida, gabarito, acertou);
   }
}

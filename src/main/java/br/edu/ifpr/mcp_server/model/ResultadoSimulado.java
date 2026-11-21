package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

//Informações do desempenho do usuário no simulado
public class ResultadoSimulado {
   private String rodada;
   private int total;
   private int acertos;
   private int erros;

   public ResultadoSimulado() {}

   public ResultadoSimulado(String rodada, int total, int acertos, int erros) {
      this.rodada = rodada;
      this.total = total;
      this.acertos = acertos;
      this.erros = erros;
   }

   public String getRodada() { return rodada; }

   public void setRodada(String rodada) {
      this.rodada = rodada;
   }

   public int getTotal() { return total; }

   public void setTotal(int total) {
      this.total = total;
   }

   public int getAcertos() { return acertos; }

   public void setAcertos(int acertos) {
      this.acertos = acertos;
   }

   public int getErros() { return erros; }

   public void setErros(int erros) {
      this.erros = erros;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [rodada=" + rodada + ", total=" + total + ", acertos=" + acertos + ", erros=" + erros + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      ResultadoSimulado other = (ResultadoSimulado) obj;
      return Objects.equals(rodada, other.rodada)
            && Objects.equals(total, other.total)
            && Objects.equals(acertos, other.acertos)
            && Objects.equals(erros, other.erros);
   }

   @Override
   public int hashCode() {
      return Objects.hash(rodada, total, acertos, erros);
   }
}

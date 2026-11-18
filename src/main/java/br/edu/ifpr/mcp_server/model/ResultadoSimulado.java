package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

/** Resumo do placar; questões ainda não respondidas não contam como erros. */
public class ResultadoSimulado {
   private String rodada;
   private boolean finalizado;
   private int total;
   private int respondidas;
   private int acertos;
   private int erros;

   public ResultadoSimulado() {}

   public ResultadoSimulado(String rodada, boolean finalizado, int total, int respondidas, int acertos, int erros) {
      setRodada(rodada);
      setFinalizado(finalizado);
      setTotal(total);
      setRespondidas(respondidas);
      setAcertos(acertos);
      setErros(erros);
   }

   public String getRodada() { return rodada; }

   public void setRodada(String rodada) {
      this.rodada = rodada;
   }

   public boolean isFinalizado() { return finalizado; }

   public void setFinalizado(boolean finalizado) {
      this.finalizado = finalizado;
   }

   public int getTotal() { return total; }

   public void setTotal(int total) {
      this.total = total;
   }

   public int getRespondidas() { return respondidas; }

   public void setRespondidas(int respondidas) {
      this.respondidas = respondidas;
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
      return getClass().getSimpleName() + " [rodada=" + rodada + ", finalizado=" + finalizado + ", total=" + total + ", respondidas=" + respondidas + ", acertos=" + acertos + ", erros=" + erros + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      ResultadoSimulado other = (ResultadoSimulado) obj;
      return Objects.equals(rodada, other.rodada)
            && Objects.equals(finalizado, other.finalizado)
            && Objects.equals(total, other.total)
            && Objects.equals(respondidas, other.respondidas)
            && Objects.equals(acertos, other.acertos)
            && Objects.equals(erros, other.erros);
   }

   @Override
   public int hashCode() {
      return Objects.hash(rodada, finalizado, total, respondidas, acertos, erros);
   }
}

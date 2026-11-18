package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

/** Resumo das respostas e do percentual de acertos. */
public class Desempenho {
   private long respondidas;
   private long acertos;
   private long erros;
   private double percentualAcertos;

   public Desempenho() {}

   public Desempenho(long respondidas, long acertos, long erros, double percentualAcertos) {
      setRespondidas(respondidas);
      setAcertos(acertos);
      setErros(erros);
      setPercentualAcertos(percentualAcertos);
   }

   public long getRespondidas() { return respondidas; }

   public void setRespondidas(long respondidas) {
      this.respondidas = respondidas;
   }

   public long getAcertos() { return acertos; }

   public void setAcertos(long acertos) {
      this.acertos = acertos;
   }

   public long getErros() { return erros; }

   public void setErros(long erros) {
      this.erros = erros;
   }

   public double getPercentualAcertos() { return percentualAcertos; }

   public void setPercentualAcertos(double percentualAcertos) {
      this.percentualAcertos = percentualAcertos;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [respondidas=" + respondidas + ", acertos=" + acertos + ", erros=" + erros + ", percentualAcertos=" + percentualAcertos + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      Desempenho other = (Desempenho) obj;
      return Objects.equals(respondidas, other.respondidas)
            && Objects.equals(acertos, other.acertos)
            && Objects.equals(erros, other.erros)
            && Objects.equals(percentualAcertos, other.percentualAcertos);
   }

   @Override
   public int hashCode() {
      return Objects.hash(respondidas, acertos, erros, percentualAcertos);
   }
}

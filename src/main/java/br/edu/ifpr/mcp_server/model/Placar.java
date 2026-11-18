package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

/** Totais apresentados ao concluir um simulado. */
public class Placar {
   private int total;
   private int acertos;
   private int erros;

   public Placar() {}

   public Placar(int total, int acertos, int erros) {
      setTotal(total);
      setAcertos(acertos);
      setErros(erros);
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
      return getClass().getSimpleName() + " [total=" + total + ", acertos=" + acertos + ", erros=" + erros + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      Placar other = (Placar) obj;
      return Objects.equals(total, other.total)
            && Objects.equals(acertos, other.acertos)
            && Objects.equals(erros, other.erros);
   }

   @Override
   public int hashCode() {
      return Objects.hash(total, acertos, erros);
   }
}

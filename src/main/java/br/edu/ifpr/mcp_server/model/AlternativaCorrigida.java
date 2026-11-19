package br.edu.ifpr.mcp_server.model;

import java.util.Objects;

//
public class AlternativaCorrigida {
   private int posicao;
   private String texto;
   private boolean correta;

   public AlternativaCorrigida() {}

   public AlternativaCorrigida(int posicao, String texto, boolean correta) {
      this.posicao = posicao;
      this.texto = texto;
      this.correta = correta;
   }

   public int getPosicao() { return posicao; }

   public void setPosicao(int posicao) {
      this.posicao = posicao;
   }

   public String getTexto() { return texto; }

   public void setTexto(String texto) {
      this.texto = texto;
   }

   public boolean isCorreta() { return correta; }

   public void setCorreta(boolean correta) {
      this.correta = correta;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [posicao=" + posicao + ", texto=" + texto + ", correta=" + correta + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AlternativaCorrigida other = (AlternativaCorrigida) obj;
      return Objects.equals(posicao, other.posicao)
            && Objects.equals(texto, other.texto)
            && Objects.equals(correta, other.correta);
   }

   @Override
   public int hashCode() {
      return Objects.hash(posicao, texto, correta);
   }
}

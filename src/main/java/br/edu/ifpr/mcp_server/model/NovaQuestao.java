package br.edu.ifpr.mcp_server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Dados validados de uma nova questão para cadastro. */
public class NovaQuestao {
   public static final int MIN_ALTERNATIVAS = 2;
   public static final int MAX_ALTERNATIVAS = 26;

   private String enunciado;
   private List<String> alternativas;
   private int gabarito;

   public NovaQuestao() {}

   @JsonCreator
   public NovaQuestao(@JsonProperty("enunciado") String enunciado,
                     @JsonProperty("alternativas") List<String> alternativas,
                     @JsonProperty("gabarito") int gabarito) {
      setEnunciado(enunciado);
      setAlternativas(alternativas);
      setGabarito(gabarito);
   }

   public String getEnunciado() { return enunciado; }

   public void setEnunciado(String enunciado) {
      this.enunciado = validarTexto(enunciado);
   }

   public List<String> getAlternativas() { return alternativas; }

   public void setAlternativas(List<String> alternativas) {
      if (alternativas == null || alternativas.size() < MIN_ALTERNATIVAS || alternativas.size() > MAX_ALTERNATIVAS) {
         throw new IllegalArgumentException("Informe entre 2 até 26 alternativas.");
      }
      if (gabarito > alternativas.size()) {
         throw new IllegalArgumentException("O gabarito deve indicar uma das alternativas.");
      }
      var alternativasValidadas = new ArrayList<String>();
      for (String alternativa : alternativas) {
         alternativasValidadas.add(validarTexto(alternativa));
      }
      this.alternativas = List.copyOf(alternativasValidadas);
   }

   public int getGabarito() { return gabarito; }

   public void setGabarito(int gabarito) {
      if (gabarito < 1 || gabarito > MAX_ALTERNATIVAS || (alternativas != null && gabarito > alternativas.size())) {
         throw new IllegalArgumentException("O gabarito deve indicar uma das alternativas.");
      }
      this.gabarito = gabarito;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [enunciado=" + enunciado + ", alternativas=" + alternativas + ", gabarito=" + gabarito + "]";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      NovaQuestao other = (NovaQuestao) obj;
      return Objects.equals(enunciado, other.enunciado)
            && Objects.equals(alternativas, other.alternativas)
            && Objects.equals(gabarito, other.gabarito);
   }

   @Override
   public int hashCode() {
      return Objects.hash(enunciado, alternativas, gabarito);
   }

   // Remove espaços desnecessários, impede texto vazio ou caracteres incompatíveis com PostgreSQL.
   private static String validarTexto(String texto) {
      if (texto == null || texto.isBlank() || texto.indexOf('\0') >= 0) {
         throw new IllegalArgumentException("O texto deve conter texto não vazio e sem caracteres nulos.");
      }
      return texto.strip();
   }

   /** Converte A/B/C ou uma posição textual para o índice de banco, que começa em 1. */
   public static int converterGabarito(String texto) {
      if (texto != null) {
         String valor = texto.strip().toUpperCase(Locale.ROOT);
         if (valor.matches("[A-Z]")) return valor.charAt(0) - 'A' + 1;
         if (valor.matches("[1-9][0-9]*")) {
            try {
               return Integer.parseInt(valor);
            } catch (NumberFormatException ex) {
               // Números fora do intervalo de int recebem a mesma orientação abaixo.
            }
         }
      }
      throw new IllegalArgumentException("Informe o gabarito como letra (A a Z) ou posição inteira começando em 1.");
   }
}

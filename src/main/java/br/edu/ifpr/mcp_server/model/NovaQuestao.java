package br.edu.ifpr.mcp_server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

//Modelo da classe para as novas questões que o usuário queira salvar no banco do PostgreSql
public class NovaQuestao {
   private String enunciado;
   private List<String> alternativas;
   private int gabarito;

   public static final int MIN_ALTERNATIVAS = 2;
   public static final int MAX_ALTERNATIVAS = 26;

   public NovaQuestao() {}
   
   public NovaQuestao(String enunciado, List<String> alternativas, int gabarito){
      //Utiliza o método de validarTexto para validar o enunciado da questão
      String enunciadoValidado = validarTexto(enunciado);

      //Verifica a quantidade de alternativas é compativel para ser salvo no banco
      if (alternativas == null || alternativas.size() < MIN_ALTERNATIVAS || alternativas.size() > MAX_ALTERNATIVAS) {
         throw new IllegalArgumentException("Informe entre 2 até 26 alternativas");
      }

      //Utiliza o método de validarTexto em cada alternativa da questão que vai ser salvo no banco
      var alternativasValidadas = new ArrayList<String>();
      for (String alternativa : alternativas) {
         alternativasValidadas.add(validarTexto(alternativa));
      }

      this.enunciado = enunciadoValidado;
      this.alternativas = alternativasValidadas;
      this.gabarito = gabarito;
   }

   public String getEnunciado() { return enunciado; }
   public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

   public List<String> getAlternativas() { return alternativas; }
   public void setAlternativas(List<String> alternativas) { this.alternativas = alternativas; }

   public int getGabarito() { return gabarito; }
   public void setGabarito(int gabarito) { this.gabarito = gabarito; }

   @Override
   public String toString() {
      return getClass().getSimpleName() + "[enunciado =" + enunciado + ", alternativas=" + alternativas + ", gabarito" + gabarito + "]";
   }

   @Override
   public boolean equals(Object obj){
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      NovaQuestao other = (NovaQuestao) obj;
      return Objects.equals(enunciado, other.enunciado) 
    		&& Objects.equals(alternativas, other.alternativas) 
    		&& Objects.equals(gabarito, other.gabarito);
   }

   @Override
   public int hashCode() { return Objects.hash(enunciado, alternativas, gabarito); }

   // Remove espaços desnecessários, impede texto vazio ou caracteres incompatíveis com PostgreSQL.
   private static String validarTexto(String texto) {
      if (texto == null || texto.isBlank() || texto.indexOf('\0') >= 0) {
         throw new IllegalArgumentException("O texto deve conter texto não vazio e sem caracteres nulos.");
      }
      return texto.strip();
   }

   /* Converte o gabarito, podendo ser letra ou posição da alternativa,
    * que o usuário enviou para o valor que será utilizado como indice no banco, que inicia pelo valor 1 */
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
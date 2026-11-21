package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.Objects;

//Classe para representar a questão que está salvo no banco
public class Questao {
   private long id;
   private String enunciado;
   private List<String> alternativas;
   private int gabarito;

   public Questao() {}

   //Questão que vai ser enviado para o usuário responder
   public Questao(QuestaoParaEstudo questao, int gabarito) {
      this(questao.getId(), questao.getEnunciado(), questao.getAlternativas(), gabarito);
   }

   public Questao(long id, String enunciado, List<String> alternativas, int gabarito){
      if (alternativas == null || alternativas.isEmpty()) {
         throw new IllegalArgumentException("A questão deve ter alternativas.");
      }

      if (gabarito < 1 || gabarito > alternativas.size()) {
         throw new IllegalArgumentException("Gabarito fora das alternativas.");
      }

      this.id = id;
      this.enunciado = enunciado;
      this.alternativas = alternativas;
      this.gabarito = gabarito;
   }

   public long getId() { return id; }

   public String getEnunciado() { return enunciado; }

   public List<String> getAlternativas() { return alternativas; }

   public int getGabarito() { return gabarito; }

   public Correcao corrigir(int escolha) {
      if (escolha < 1 || escolha > alternativas.size()){
         throw new IllegalArgumentException("Escolha uma alternativa entre 1 e " + alternativas.size() + ".");
      }

      return new Correcao(id, enunciado, escolha, gabarito, escolha==gabarito);
   }

   public QuestaoParaEstudo paraEstudo() {
      return new QuestaoParaEstudo(id, enunciado, alternativas);
   }

   @Override
   public String toString(){
      return getClass().getSimpleName() + "[id= " + id + ", enunciado= " + enunciado + ", alternativas= " + alternativas + ", gabarito= " + gabarito + "]";
   }

   @Override
   public boolean equals(Object object){
      if(this == object) return true;
      if(object == null) return false;
      if (getClass() != object.getClass()) return false;
      Questao other = (Questao) object;
      return Objects.equals(id, other.id);
   }

   @Override
   public int hashCode(){
      return Objects.hash(id);
   }
}

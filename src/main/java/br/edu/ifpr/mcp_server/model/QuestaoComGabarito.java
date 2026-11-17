package br.edu.ifpr.mcp_server.model;

import java.util.ArrayList;
import java.util.List;

/** Associa o gabarito cadastrado à mesma sequência de alternativas exibida no sorteio. */
public record QuestaoComGabarito(QuestaoParaEstudo questao, int gabarito) {
   public record AlternativaCorrigida(int posicao, String texto, boolean correta) {}
   public record Correcao(long id, String enunciado, int alternativaEscolhida, int gabarito,
                         boolean acertou, List<AlternativaCorrigida> alternativas) {}

   /** Compara a escolha com o gabarito usando posições que começam em 1. */
   public Correcao corrigir(int escolha) {
      int quantidade = questao.alternativas().size();
      if (escolha < 1 || escolha > quantidade) {
         throw new IllegalArgumentException("Escolha uma alternativa entre 1 e " + quantidade + ".");
      }
      var alternativas = new ArrayList<AlternativaCorrigida>(quantidade);
      for (int indice = 0; indice < quantidade; indice++) {
         int posicao = indice + 1;
         alternativas.add(new AlternativaCorrigida(posicao, questao.alternativas().get(indice),
                                                   posicao == gabarito));
      }
      return new Correcao(questao.id(), questao.enunciado(), escolha, gabarito,
                         escolha == gabarito, List.copyOf(alternativas));
   }
}

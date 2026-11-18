package br.edu.ifpr.mcp_server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class NovaQuestaoTest {
   /** A validação pertence ao modelo e também protege chamadas diretas ao repositório. */
   @Test
   void rejeitaDadosInvalidosNaConstrucao() {
      assertThrows(IllegalArgumentException.class, () -> new NovaQuestao(null, List.of("A", "B"), 1));
      assertThrows(IllegalArgumentException.class, () -> new NovaQuestao("P", null, 1));
      assertThrows(IllegalArgumentException.class, () -> new NovaQuestao("P", Arrays.asList("A", null), 1));
      assertThrows(IllegalArgumentException.class, () -> new NovaQuestao("P", List.of("A", "B"), 0));
      assertThrows(IllegalArgumentException.class, () -> new NovaQuestao("P", List.of("A", "B"), 3));
   }

   /** Alterações na lista original não afetam as alternativas armazenadas. */
   @Test
   void preservaCopiaImutavelDasAlternativas() {
      var alternativas = new ArrayList<>(List.of("A", "B"));
      var questao = new NovaQuestao("P", alternativas, 2);
      alternativas.set(1, "Alterada");
      assertEquals(List.of("A", "B"), questao.getAlternativas());
      assertThrows(UnsupportedOperationException.class, () -> questao.getAlternativas().add("C"));
   }

   /** Aceita a última letra disponível para uma questão de 26 alternativas. */
   @Test
   void aceitaAlternativaZ() {
      var alternativas = IntStream.rangeClosed(1, 26).mapToObj(i -> "Alternativa " + i).toList();
      assertEquals(26, new NovaQuestao("P", alternativas, NovaQuestao.converterGabarito("z")).getGabarito());
      assertThrows(IllegalArgumentException.class, () -> NovaQuestao.converterGabarito(null));
   }

   @Test
   void settersValidamDadosSemSubstituirValoresQuandoInvalidos() {
      var questao = new NovaQuestao();
      questao.setEnunciado(" P ");
      questao.setAlternativas(List.of(" A ", " B ", " C "));
      questao.setGabarito(3);

      assertThrows(IllegalArgumentException.class, () -> questao.setEnunciado(" "));
      assertThrows(IllegalArgumentException.class, () -> questao.setAlternativas(List.of("A", "B")));
      assertThrows(IllegalArgumentException.class, () -> questao.setGabarito(4));
      assertEquals(new NovaQuestao("P", List.of("A", "B", "C"), 3), questao);
   }
}

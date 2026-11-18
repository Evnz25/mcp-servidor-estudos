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

   /** A ordem e os textos validados não podem mudar após a criação da questão. */
   @Test
   void preservaCopiaImutavelDasAlternativas() {
      var alternativas = new ArrayList<>(List.of("A", "B"));
      var questao = new NovaQuestao("P", alternativas, 2);
      alternativas.set(1, "Alterada");
      assertEquals(List.of("A", "B"), questao.alternativas());
      assertThrows(UnsupportedOperationException.class, () -> questao.alternativas().add("C"));
   }

   /** Aceita a última letra disponível para uma questão de 26 alternativas. */
   @Test
   void aceitaAlternativaZ() {
      var alternativas = IntStream.rangeClosed(1, 26).mapToObj(i -> "Alternativa " + i).toList();
      assertEquals(26, new NovaQuestao("P", alternativas, NovaQuestao.converterGabarito("z")).gabarito());
      assertThrows(IllegalArgumentException.class, () -> NovaQuestao.converterGabarito(null));
   }
}

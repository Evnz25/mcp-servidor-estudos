package br.edu.ifpr.mcp_server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class QuestaoTest {
   private final QuestaoParaEstudo base = new QuestaoParaEstudo(7, "FIFO?", List.of("Pilha", "Fila"));
   private final JsonMapper json = new JsonMapper();

   @Test
   void corrigeEValidaAlternativas() {
      var questao = new Questao(base, 2);
      assertTrue(questao.corrigir(2).isAcertou());
      assertFalse(questao.corrigir(1).isAcertou());
      assertThrows(IllegalArgumentException.class, () -> questao.corrigir(0));
      assertThrows(IllegalArgumentException.class, () -> questao.corrigir(3));
      assertThrows(IllegalArgumentException.class, () -> new Questao(base, 3));
   }

   @Test
   void apresentacaoNaoExpoeGabaritoNemTentativa() {
      var publica = new Questao(base, 2).paraEstudo();
      var esperado = json.readTree("""
            {"id":7,"enunciado":"FIFO?","alternativas":["Pilha","Fila"]}
            """);
      assertEquals(esperado, json.readTree(json.writeValueAsString(publica)));
      assertEquals(publica, json.readValue(json.writeValueAsString(publica), QuestaoParaEstudo.class));
   }

   @Test
   void simuladoApresentaDadosPublicosECorrigeComGabaritoInterno() {
      var questoes = IntStream.rangeClosed(1, Simulado.TOTAL)
            .mapToObj(i -> new Questao(i, "FIFO?", List.of("Pilha", "Fila"), 2)).toList();
      var simulado = new Simulado(questoes);
      assertFalse(json.valueToTree(simulado.questaoAtual().getQuestao()).has("gabarito"));
      for (int numero = 1; numero <= Simulado.TOTAL; numero++) simulado.responder(numero, 2);
      assertEquals(Simulado.TOTAL, simulado.consultarResultado().getAcertos());
      assertTrue(simulado.consultarResultado().isFinalizado());
   }
}

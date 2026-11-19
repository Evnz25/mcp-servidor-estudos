package br.edu.ifpr.mcp_server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class QuestaoDecoratorTest {
   private final QuestaoParaEstudo base = new QuestaoParaEstudo(7, "FIFO?", List.of("Pilha", "Fila"));
   private final JsonMapper json = new JsonMapper();

   @Test
   void decoradoresCompartilhamContratoEDelegamAoComponente() {
      var sorteada = new QuestaoSorteada(base, "tentativa-1");
      var corrigivel = new QuestaoComGabarito(sorteada, 2);
      base.setEnunciado("Qual estrutura segue FIFO?");
      for (Questao questao : List.of(base, sorteada, corrigivel)) {
         assertEquals(7, questao.getId());
         assertEquals("Qual estrutura segue FIFO?", questao.getEnunciado());
         assertEquals(List.of("Pilha", "Fila"), questao.getAlternativas());
      }
      assertEquals("tentativa-1", sorteada.getTentativa());
      assertTrue(corrigivel.corrigir(2).isAcertou());
      assertFalse(corrigivel.corrigir(1).isAcertou());
      assertThrows(IllegalArgumentException.class, () -> corrigivel.corrigir(0));
      assertThrows(IllegalArgumentException.class, () -> corrigivel.corrigir(3));
   }

   @Test
   void sorteioMantemJsonPlanoSemExporGabaritoDoComponente() {
      var sorteada = new QuestaoSorteada(new QuestaoComGabarito(base, 2), "tentativa-1");
      var esperado = json.readTree("""
            {"id":7,"enunciado":"FIFO?","alternativas":["Pilha","Fila"],"tentativa":"tentativa-1"}
            """);
      assertEquals(esperado, json.readTree(json.writeValueAsString(sorteada)));
      var lida = json.readValue(json.writeValueAsString(sorteada), QuestaoSorteada.class);
      assertEquals(sorteada, lida);
      assertEquals(sorteada.hashCode(), lida.hashCode());
   }

   @Test
   void simuladoExibeSomenteDadosPublicosMesmoComDecoradoresEncadeados() {
      var questoes = IntStream.range(0, Simulado.TOTAL)
            .mapToObj(i -> new QuestaoComGabarito(new QuestaoSorteada(base, "tentativa-" + i), 2))
            .toList();
      var simulado = new Simulado(questoes);
      assertEquals(json.valueToTree(base), json.valueToTree(simulado.questaoAtual().getQuestao()));
      for (int numero = 1; numero <= Simulado.TOTAL; numero++) simulado.responder(numero, 2);
      assertEquals(Simulado.TOTAL, simulado.consultarResultado().getAcertos());
      assertTrue(simulado.consultarResultado().isFinalizado());
   }

   @Test
   void rejeitaDecoradorSemComponente() {
      assertThrows(NullPointerException.class, () -> new QuestaoSorteada(null, "tentativa"));
      assertThrows(NullPointerException.class, () -> new QuestaoComGabarito(null, 1));
   }
}

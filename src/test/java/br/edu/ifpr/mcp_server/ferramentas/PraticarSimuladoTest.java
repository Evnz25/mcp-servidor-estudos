package br.edu.ifpr.mcp_server.ferramentas;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.modelos.QuestaoComGabarito;
import br.edu.ifpr.mcp_server.modelos.QuestaoParaEstudo;
import br.edu.ifpr.mcp_server.modelos.Simulado.Pergunta;
import br.edu.ifpr.mcp_server.modelos.Simulado.Placar;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class PraticarSimuladoTest {
   private final PraticarSimulado ferramenta = new PraticarSimulado(quantidade -> IntStream.rangeClosed(1, quantidade)
         .mapToObj(n -> new QuestaoComGabarito(new QuestaoParaEstudo(n, "Pergunta " + n, List.of("A", "B")), 2))
         .toList());

   /** Monta uma chamada como a enviada pelo agente. */
   private CallToolRequest Pedido(Map<String, Object> argumentos) {
      return CallToolRequest.builder(PraticarSimulado.NOME).arguments(argumentos).build();
   }

   /** Inicia uma rodada e recupera seu identificador. */
   private String Iniciar() {
      var resultado = ferramenta.run(null, Pedido(Map.of("operacao", "iniciar")));
      return ((Pergunta) resultado.structuredContent()).rodada();
   }

   /** Envia uma alternativa com o número da questão à qual se refere. */
   private CallToolResult Responder(String rodada, int numero, Number alternativa) {
      return ferramenta.run(null, Pedido(Map.of("operacao", "responder", "rodada", rodada,
            "numero", numero, "alternativa", alternativa)));
   }

   /** Respostas inválidas e reenvios não alteram a pontuação de nenhuma rodada. */
   @Test
   void MantemOrdemEPlacarDeCadaRodada() {
      String primeira = Iniciar();
      String segunda = Iniciar();
      assertNotEquals(primeira, segunda);
      assertTrue(Responder("ausente", 1, 2).isError());
      assertTrue(Responder(primeira, 2, 2).isError());
      assertTrue(Responder(primeira, 1, 3).isError());
      assertTrue(Responder(primeira, 1, 1.5).isError());
      for (int n = 1; n <= 5; n++) {
         var acerto = Responder(primeira, n, 2);
         var erro = Responder(segunda, n, 1);
         assertFalse(acerto.isError());
         assertFalse(erro.isError());
         assertEquals(acerto.structuredContent(), Responder(primeira, n, 2).structuredContent());
         assertTrue(Responder(primeira, n, 1).isError());
         if (n < 5) {
            assertEquals(n + 1, ((Pergunta) acerto.structuredContent()).numero());
         } else {
            assertEquals(new Placar(5, 5, 0), acerto.structuredContent());
            assertEquals(new Placar(5, 0, 5), erro.structuredContent());
         }
      }
   }

   /** Rejeita operações incompletas e informa falhas que impedem iniciar um sorteio. */
   @Test
   void TrataErrosDeEntradaEDoSorteio() {
      assertTrue(ferramenta.run(null, Pedido(Map.of("operacao", "responder"))).isError());
      assertTrue(ferramenta.run(null, Pedido(Map.of("operacao", "iniciar", "numero", 1))).isError());
      var iniciar = Pedido(Map.of("operacao", "iniciar"));
      assertTrue(new PraticarSimulado(n -> List.of()).run(null, iniciar).isError());
      assertTrue(new PraticarSimulado(n -> { throw new SQLException(); }).run(null, iniciar).isError());
   }
}

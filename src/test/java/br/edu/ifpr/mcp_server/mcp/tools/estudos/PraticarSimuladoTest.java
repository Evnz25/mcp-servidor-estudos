package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.model.Questao;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;
import br.edu.ifpr.mcp_server.model.RodadaSimulado;
import br.edu.ifpr.mcp_server.model.ResultadoSimulado;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class PraticarSimuladoTest {
   private final PraticarSimuladoTool ferramenta = new PraticarSimuladoTool(quantidade -> IntStream.rangeClosed(1, quantidade)
         .mapToObj(n -> new Questao(new QuestaoParaEstudo(n, "Pergunta " + n, List.of("A", "B")), 2))
         .toList());

   /** Monta uma chamada como a enviada pelo agente. */
   private CallToolRequest pedido(Map<String, Object> argumentos) {
      return CallToolRequest.builder(PraticarSimuladoTool.NOME).arguments(argumentos).build();
   }

   /** Inicia uma rodada e recupera seu identificador. */
   private String iniciar() {
      var resultado = ferramenta.run(null, pedido(Map.of("operacao", "iniciar")));
      return ((RodadaSimulado) resultado.structuredContent()).getRodada();
   }

   /** Envia uma alternativa com o número da questão à qual se refere. */
   private CallToolResult responder(String rodada, int numero, Number alternativa) {
      return ferramenta.run(null, pedido(Map.of("operacao", "responder", "rodada", rodada,
            "numero", numero, "alternativa", alternativa)));
   }

   /** Percorre as questões e mantém a pontuação de cada rodada. */
   @Test
   void mantemOrdemEPlacarDeCadaRodada() {
      String primeira = iniciar();
      String segunda = iniciar();
      assertNotEquals(primeira, segunda);
      assertTrue(responder("ausente", 1, 2).isError());
      assertTrue(responder(primeira, 1, 3).isError());
      assertTrue(responder(primeira, 1, 1.5).isError());
      for (int n = 1; n <= 5; n++) {
         var acerto = responder(primeira, n, 2);
         var erro = responder(segunda, n, 1);
         assertFalse(acerto.isError());
         assertFalse(erro.isError());
         if (n < 5) {
            assertEquals(n + 1, ((RodadaSimulado) acerto.structuredContent()).getNumero());
         } else {
            assertEquals(new ResultadoSimulado(primeira, 5, 5, 0), acerto.structuredContent());
            assertEquals(new ResultadoSimulado(segunda, 5, 0, 5), erro.structuredContent());
         }
      }
   }

   /** Rejeita operações incompletas e informa falhas que impedem iniciar um sorteio. */
   @Test
   void trataErrosDeEntradaEDoSorteio() {
      assertTrue(ferramenta.run(null, pedido(Map.of("operacao", "responder"))).isError());
      assertTrue(ferramenta.run(null, pedido(Map.of("operacao", "iniciar", "numero", 1))).isError());
      var iniciar = pedido(Map.of("operacao", "iniciar"));
      assertTrue(new PraticarSimuladoTool(n -> List.of()).run(null, iniciar).isError());
      assertTrue(new PraticarSimuladoTool(n -> { throw new SQLException(); }).run(null, iniciar).isError());
   }
}

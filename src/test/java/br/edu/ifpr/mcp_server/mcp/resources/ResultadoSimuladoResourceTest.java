package br.edu.ifpr.mcp_server.mcp.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarSimuladoTool;
import br.edu.ifpr.mcp_server.model.QuestaoComGabarito;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;
import br.edu.ifpr.mcp_server.model.ResultadoSimulado;
import br.edu.ifpr.mcp_server.model.Simulado;
import br.edu.ifpr.mcp_server.services.estudos.SimuladoService;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import tools.jackson.databind.json.JsonMapper;

class ResultadoSimuladoResourceTest {
   private final JsonMapper json = new JsonMapper();
   private final SimuladoService service = new SimuladoService(n -> IntStream.rangeClosed(1, n)
         .mapToObj(i -> new QuestaoComGabarito(new QuestaoParaEstudo(i, "Pergunta " + i, List.of("A", "B")), 2))
         .toList());
   private final ResultadoSimuladoResource resource = new ResultadoSimuladoResource(service);
   private final PraticarSimuladoTool tool = new PraticarSimuladoTool(service);

   /** Lê o JSON do Resource, conferindo também URI, tipo e ausência de dados das questões. */
   private ResultadoSimulado ler(String rodada) {
      String uri = ResultadoSimuladoResource.URI_TEMPLATE.replace("{rodada}", rodada);
      var resposta = resource.read(null, ReadResourceRequest.builder(uri).build());
      assertEquals(1, resposta.contents().size());
      var conteudo = assertInstanceOf(TextResourceContents.class, resposta.contents().getFirst());
      assertEquals(uri, conteudo.uri());
      assertEquals("application/json", conteudo.mimeType());
      assertEquals(6, json.readTree(conteudo.text()).size());
      return json.readValue(conteudo.text(), ResultadoSimulado.class);
   }

   /** Inicia a rodada pela ferramenta para verificar o compartilhamento do serviço. */
   private String iniciar() {
      var resposta = tool.run(null, CallToolRequest.builder(PraticarSimuladoTool.NOME)
            .arguments(Map.of("operacao", "iniciar")).build());
      assertFalse(resposta.isError());
      return ((Simulado.Pergunta) resposta.structuredContent()).rodada();
   }

   /** Responde pela ferramenta, usando o mesmo caminho acionado pelo cliente MCP. */
   private void responder(String rodada, int numero, int alternativa) {
      var resposta = tool.run(null, CallToolRequest.builder(PraticarSimuladoTool.NOME)
            .arguments(Map.of("operacao", "responder", "rodada", rodada,
                              "numero", numero, "alternativa", alternativa)).build());
      assertFalse(resposta.isError());
   }

   /** Confere resultados inicial, parcial e final e garante que leituras/reenvios não somam pontos. */
   @Test
   void consultaOPlacarSemAlterarAsRespostas() {
      String rodada = iniciar();
      var inicial = new ResultadoSimulado(rodada, false, 5, 0, 0, 0);
      assertEquals(inicial, ler(rodada));
      assertEquals(inicial, ler(rodada));
      responder(rodada, 1, 2);
      var parcial = ler(rodada);
      assertEquals(new ResultadoSimulado(rodada, false, 5, 1, 1, 0), parcial);
      responder(rodada, 1, 2);
      assertEquals(parcial, ler(rodada));
      for (int numero = 2; numero <= 5; numero++) responder(rodada, numero, numero <= 3 ? 2 : 1);
      var esperado = new ResultadoSimulado(rodada, true, 5, 5, 3, 2);
      assertEquals(esperado, ler(rodada));
      assertEquals(esperado, ler(rodada));
      assertEquals(new ResultadoSimulado(rodada, false, 5, 1, 1, 0), parcial);
   }

   /** Mantém o placar separado para cada rodada e não conta perguntas pendentes como erros. */
   @Test
   void separaAsRodadas() {
      String primeira = iniciar();
      String segunda = iniciar();
      responder(primeira, 1, 1);
      assertEquals(new ResultadoSimulado(primeira, false, 5, 1, 0, 1), ler(primeira));
      assertEquals(new ResultadoSimulado(segunda, false, 5, 0, 0, 0), ler(segunda));
   }

   /** Uma rodada desconhecida ou descartada deve produzir erro de Resource, nunca um placar zerado. */
   @Test
   void informaRodadasAusentesEDescartadas() {
      assertEquals(ErrorCodes.RESOURCE_NOT_FOUND,
            assertThrows(McpError.class, () -> ler(UUID.randomUUID().toString())).getJsonRpcError().code());
      String antiga = iniciar();
      for (int i = 0; i < 256; i++) iniciar();
      assertEquals(ErrorCodes.RESOURCE_NOT_FOUND,
            assertThrows(McpError.class, () -> ler(antiga)).getJsonRpcError().code());
   }

   /** Rejeita outros caminhos e identificadores malformados sem consultar ou alterar a rodada. */
   @ParameterizedTest
   @ValueSource(strings = { "estudos://simulados/abc/resultado", "file:///resultado",
         "estudos://simulados/00000000-0000-0000-0000-000000000000/resultado?extra=1",
         "estudos://simulados/00000000-0000-0000-0000-000000000000/resultado/",
         "estudos://simulados/../resultado" })
   void rejeitaUrisInvalidas(String uri) {
      var erro = assertThrows(McpError.class,
            () -> resource.read(null, ReadResourceRequest.builder(uri).build()));
      assertEquals(ErrorCodes.RESOURCE_NOT_FOUND, erro.getJsonRpcError().code());
   }
}

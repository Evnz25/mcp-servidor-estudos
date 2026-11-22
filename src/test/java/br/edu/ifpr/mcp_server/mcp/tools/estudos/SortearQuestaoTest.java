package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.repositories.IQuestaoRepository;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class SortearQuestaoTest {
   private final CallToolRequest pedido = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "sortear")).build();

   private PraticarQuestaoTool sorteio(IQuestaoRepository questoes) {
      return new PraticarQuestaoTool(questoes, id -> fail("Um sorteio não consulta o gabarito."));
   }

   //Confere a lista exata de campos nas duas representações enviadas ao aluno
   @Test
   void retornaSomenteOsDadosPublicos() {
      var questao = new QuestaoParaEstudo(7L, "Qual estrutura segue FIFO?", List.of("Pilha", "Fila"));
      var ferramenta = sorteio(() -> Optional.of(questao));
      var resultado = ferramenta.run(null, pedido);
      var json = new JsonMapper();
      var esperado = json.valueToTree(Map.of("id", 7L, "enunciado", questao.getEnunciado(),
                                            "alternativas", questao.getAlternativas()));
      assertFalse(resultado.isError());
      assertEquals(esperado, json.valueToTree(resultado.structuredContent()));
      assertEquals(1, resultado.content().size());
      assertEquals(json.readTree(esperado.toString()),
                   json.readTree(((TextContent) resultado.content().getFirst()).text()));
   }

   //Uma tabela vazia é um resultado normal do sorteio
   @Test
   void informaQuandoNaoHaQuestoes() {
      var resultado = sorteio(Optional::empty).run(null, pedido);
      assertFalse(resultado.isError());
      assertNull(resultado.structuredContent());
      assertEquals("Nenhuma questão cadastrada.", ((TextContent) resultado.content().getFirst()).text());
   }
}

package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import br.edu.ifpr.mcp_server.model.NovaQuestao;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class CadastrarQuestaoToolTest {
   //Prepara uma questão com espaços externos para verificar a normalização antes de persistir
   private static Map<String, Object> argumentos(Object gabarito) {
      var dados = new HashMap<String, Object>();
      dados.put("enunciado", "  Qual padrão agrega respostas?  ");
      dados.put("alternativas", List.of(" Circuit Breaker ", " API Gateway "));
      dados.put("gabarito", gabarito);
      return dados;
   }

   //Oferece representações equivalentes da segunda alternativa
   static Stream<Object> gabaritosValidos() { return Stream.of("B", 2); }

   //O contrato aceita letra ou posição e devolve exatamente o conteúdo gravado e seu ID
   @ParameterizedTest
   @MethodSource("gabaritosValidos")
   void cadastraComLetraOuPosicao(Object gabarito) {
      var gravadas = new ArrayList<NovaQuestao>();
      var ferramenta = new CadastrarQuestaoTool(questao -> { gravadas.add(questao); return 3_000_000_001L; });
      var resposta = ferramenta.run(null, CallToolRequest.builder(CadastrarQuestaoTool.NOME)
            .arguments(argumentos(gabarito)).build());
      assertFalse(resposta.isError());
      assertEquals(List.of(new NovaQuestao("Qual padrão agrega respostas?",
            List.of("Circuit Breaker", "API Gateway"), 2)), gravadas);
      var json = new JsonMapper();
      var dados = json.valueToTree(resposta.structuredContent());
      assertEquals(3_000_000_001L, dados.get("id").asLong());
      assertEquals("Qual padrão agrega respostas?", dados.get("enunciado").asString());
      assertEquals(2, dados.get("gabarito").asInt());
      assertEquals("API Gateway", dados.get("alternativas").get(1).asString());
      assertEquals(dados, json.readTree(((TextContent) resposta.content().getFirst()).text()));
   }

   //Impede a gravação quando o gabarito aponta para uma alternativa inexistente
   @Test
   void rejeitaGabaritoForaDasAlternativas() {
      var dados = argumentos(3);
      var ferramenta = new CadastrarQuestaoTool(questao -> fail("Não deve acessar o banco."));
      CallToolResult resposta = ferramenta.run(null,
            CallToolRequest.builder(CadastrarQuestaoTool.NOME).arguments(dados).build());
      assertTrue(resposta.isError());
      assertNull(resposta.structuredContent());
      assertFalse(((TextContent) resposta.content().getFirst()).text().isBlank());
   }
}

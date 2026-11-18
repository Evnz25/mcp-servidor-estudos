package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
   /** Prepara uma questão com espaços externos para verificar a normalização antes de persistir. */
   private static Map<String, Object> argumentos(Object gabarito) {
      var dados = new HashMap<String, Object>();
      dados.put("enunciado", "  Qual padrão agrega respostas?  ");
      dados.put("alternativas", List.of(" Circuit Breaker ", " API Gateway "));
      dados.put("gabarito", gabarito);
      return dados;
   }

   /** Oferece representações equivalentes da segunda alternativa. */
   static Stream<Object> gabaritosValidos() { return Stream.of("B", "b", " B ", "2", 2, 2L); }

   /** O contrato aceita letra ou posição e devolve exatamente o conteúdo gravado e seu ID. */
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

   /** Reúne entradas que não podem produzir uma posição válida dentro da questão de duas alternativas. */
   static Stream<Object> gabaritosInvalidos() {
      return Stream.of(null, true, List.of(2), 0, -1, 3, 1.5, Long.MAX_VALUE,
            Double.NaN, "", " ", "C", "B)", "API Gateway", "1.5", "0", "999999999999999999999999");
   }

   /** Recusa gabaritos inválidos antes de chamar a operação de escrita. */
   @ParameterizedTest
   @MethodSource("gabaritosInvalidos")
   void rejeitaGabaritosInvalidos(Object gabarito) {
      conferirRejeicao(argumentos(gabarito));
   }

   /** Campos ausentes, extras ou de tipo errado nunca chegam ao repositório. */
   @Test
   void rejeitaDadosIncompletos() {
      conferirRejeicao(null);
      conferirRejeicao(Map.of());
      for (String campo : List.of("enunciado", "alternativas", "gabarito")) {
         var dados = argumentos("B");
         dados.remove(campo);
         conferirRejeicao(dados);
      }
      var extra = argumentos("B");
      extra.put("id", 7);
      conferirRejeicao(extra);
      for (Object enunciado : Arrays.asList(null, 42, "", " \n ", "Texto\0")) {
         var dados = argumentos("B");
         dados.put("enunciado", enunciado);
         conferirRejeicao(dados);
      }
      for (Object alternativas : Arrays.asList(null, "A,B", List.of(), List.of("A"),
            List.of("A", " "), List.of("A", 3), Arrays.asList("A", null),
            List.of("A", "B\0"), java.util.Collections.nCopies(27, "Alternativa"))) {
         var dados = argumentos("B");
         dados.put("alternativas", alternativas);
         conferirRejeicao(dados);
      }
   }

   /** Confere o erro MCP e impede silenciosamente qualquer tentativa de persistir dados inválidos. */
   private void conferirRejeicao(Map<String, Object> dados) {
      var ferramenta = new CadastrarQuestaoTool(questao -> fail("Não deve acessar o banco."));
      CallToolResult resposta = ferramenta.run(null,
            CallToolRequest.builder(CadastrarQuestaoTool.NOME).arguments(dados).build());
      assertTrue(resposta.isError());
      assertNull(resposta.structuredContent());
      assertFalse(((TextContent) resposta.content().getFirst()).text().isBlank());
   }

   /** Diferencia falta de permissão das demais falhas sem declarar sucesso ou repetir a gravação. */
   @Test
   void informaFalhasDoBanco() {
      for (String estado : List.of("42501", "08001")) {
         var ferramenta = new CadastrarQuestaoTool(questao -> { throw new SQLException("Detalhe interno", estado); });
         var resposta = ferramenta.run(null, CallToolRequest.builder(CadastrarQuestaoTool.NOME)
               .arguments(argumentos("B")).build());
         assertTrue(resposta.isError());
         assertNull(resposta.structuredContent());
         String texto = ((TextContent) resposta.content().getFirst()).text();
         assertTrue(texto.contains(estado.equals("42501") ? "permissão" : "Não foi possível cadastrar"));
         assertFalse(texto.contains("Detalhe interno"));
      }
   }
}

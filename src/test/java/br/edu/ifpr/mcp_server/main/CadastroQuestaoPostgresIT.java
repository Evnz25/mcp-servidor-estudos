package br.edu.ifpr.mcp_server.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = ".+")
class CadastroQuestaoPostgresIT {
   /** Cadastra o exemplo de API Gateway por MCP, confere o banco e corrige a questão após reiniciar o servidor. */
   @Test
   void cadastraEPersisteQuestaoPeloProtocolo() throws Exception {
      String url = System.getenv("BANCO_TESTE_URL");
      String schema = "teste_cadastro_" + UUID.randomUUID().toString().replace("-", "");
      var json = new JsonMapper();
      try (var conexao = DriverManager.getConnection(url, System.getenv("BANCO_USUARIO"), System.getenv("BANCO_SENHA"));
           var comando = conexao.createStatement()) {
         comando.execute("CREATE SCHEMA " + schema);
         try {
            conexao.setSchema(schema);
            try (var arquivo = getClass().getResourceAsStream("/banco/questao.sql")) {
               comando.execute(new String(arquivo.readAllBytes(), UTF_8));
            }
            Map<String, String> ambiente = Map.of("BANCO_URL", url + (url.contains("?") ? "&" : "?")
                  + "currentSchema=" + schema);
            long id;
            Map<String, Object> exemplo = lerExemplo();
            try (var cliente = McpServer01IT.abrirCliente(ambiente)) {
               var ferramenta = cliente.listTools().tools().stream()
                     .filter(tool -> tool.name().equals("cadastrar_questao")).findFirst().orElseThrow();
               assertFalse(ferramenta.annotations().readOnlyHint());
               assertFalse(ferramenta.annotations().idempotentHint());
               var resposta = cadastrar(cliente, exemplo);
               assertFalse(resposta.isError());
               var dados = json.valueToTree(resposta.structuredContent());
               id = dados.get("id").asLong();
               assertTrue(id > 0);
               assertEquals(2, dados.get("gabarito").asInt());
               assertEquals(exemplo.get("enunciado"), dados.get("enunciado").asString());
               assertEquals(json.valueToTree(exemplo.get("alternativas")), dados.get("alternativas"));
               assertEquals(dados, json.readTree(((TextContent) resposta.content().getFirst()).text()));
               try (var linha = comando.executeQuery("SELECT * FROM questao")) {
                  assertTrue(linha.next());
                  assertEquals(id, linha.getLong("id"));
                  assertEquals(exemplo.get("enunciado"), linha.getString("enunciado"));
                  assertEquals(2, linha.getInt("gabarito"));
                  assertEquals(exemplo.get("alternativas"), List.of((String[]) linha.getArray("alternativas").getArray()));
                  assertFalse(linha.next());
               }
               var invalida = Map.<String, Object>of("enunciado", " ", "alternativas", List.of("A", "B"), "gabarito", 1);
               assertTrue(cadastrar(cliente, invalida).isError());
               String textoLiteral = "Pergunta com 'aspas'; DROP TABLE questao; -- e acentuação: ação?";
               var literal = cadastrar(cliente, Map.of("enunciado", textoLiteral,
                     "alternativas", List.of("Alternativa 'A'", "Alternativa B"), "gabarito", 1));
               assertFalse(literal.isError());
               long outroId = json.valueToTree(literal.structuredContent()).get("id").asLong();
               assertNotEquals(id, outroId);
               try (var linhas = comando.executeQuery("SELECT count(*) FROM questao")) {
                  assertTrue(linhas.next());
                  assertEquals(2, linhas.getInt(1));
               }
               try (var stmt = conexao.prepareStatement("SELECT enunciado FROM questao WHERE id = ?")) {
                  stmt.setLong(1, outroId);
                  try (var linha = stmt.executeQuery()) {
                     assertTrue(linha.next());
                     assertEquals(textoLiteral, linha.getString(1));
                  }
               }
            }
            // Um novo processo confirma a persistência real, sem depender do estado em memória.
            try (var cliente = McpServer01IT.abrirCliente(ambiente)) {
               var correcao = cliente.callTool(CallToolRequest.builder("praticar_questao")
                     .arguments(Map.of("operacao", "corrigir", "id", id, "alternativa", 2)).build());
               assertFalse(correcao.isError());
               assertTrue(json.valueToTree(correcao.structuredContent()).get("acertou").asBoolean());
            }
         } finally {
            comando.execute("DROP SCHEMA " + schema + " CASCADE");
         }
      }
   }

   /** Envia uma chamada real da ferramenta usando os argumentos fornecidos. */
   private CallToolResult cadastrar(McpSyncClient cliente, Map<String, Object> argumentos) {
      return cliente.callTool(CallToolRequest.builder("cadastrar_questao").arguments(argumentos).build());
   }

   /** Reaproveita exatamente o exemplo disponibilizado ao usuário para o Inspector. */
   @SuppressWarnings("unchecked")
   private Map<String, Object> lerExemplo() throws Exception {
      try (var arquivo = getClass().getResourceAsStream("/exemplos/cadastrar-questao.json")) {
         assertNotNull(arquivo);
         return new JsonMapper().readValue(arquivo, Map.class);
      }
   }
}

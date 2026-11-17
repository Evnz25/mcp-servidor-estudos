package br.edu.ifpr.mcp_server.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = ".+")
class SorteioPostgresIT {
   /** Testa banco vazio e questão cadastrada com um servidor real e um esquema exclusivo do teste. */
   @Test
   void sorteiaSemDivulgarOGabarito() throws Exception {
      String url = System.getenv("BANCO_TESTE_URL");
      String usuario = System.getenv("BANCO_USUARIO");
      String senha = System.getenv("BANCO_SENHA");
      String esquema = "teste_sorteio_" + UUID.randomUUID().toString().replace("-", "");
      try (var conexao = DriverManager.getConnection(url, usuario, senha);
           var comando = conexao.createStatement()) {
         comando.execute("CREATE SCHEMA " + esquema);
         try {
            conexao.setSchema(esquema);
            try (var arquivo = getClass().getResourceAsStream("/banco/questao.sql")) {
               comando.execute(new String(arquivo.readAllBytes(), UTF_8));
            }
            String urlServidor = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + esquema;
            // O processo filho herda as credenciais do teste e usa o esquema isolado.
            var ambiente = Map.of("BANCO_URL", urlServidor);
            try (var cliente = McpServer01IT.abrirCliente(ambiente)) {
               var pedido = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "sortear")).build();
               var vazio = cliente.callTool(pedido);
               assertFalse(vazio.isError());
               assertEquals("Nenhuma questão cadastrada.", ((TextContent) vazio.content().getFirst()).text());

               comando.executeUpdate("""
                     INSERT INTO questao (enunciado, alternativas, gabarito)
                     VALUES ('Qual estrutura segue FIFO?', ARRAY['Pilha', 'Fila'], 2)
                     """);
               var resultado = cliente.callTool(pedido);
               var json = new JsonMapper();
               var esperado = json.readTree("""
                     {"id":1,"enunciado":"Qual estrutura segue FIFO?","alternativas":["Pilha","Fila"]}
                     """);
               assertFalse(resultado.isError());
               assertEquals(esperado, json.valueToTree(resultado.structuredContent()));
               assertEquals(1, resultado.content().size());
               assertEquals(esperado, json.readTree(((TextContent) resultado.content().getFirst()).text()));
               long id = json.valueToTree(resultado.structuredContent()).get("id").asLong();
               conferirCorrecao(cliente, id);
               var inicio = CallToolRequest.builder("praticar_simulado").arguments(Map.of("operacao", "iniciar")).build();
               assertTrue(cliente.callTool(inicio).isError());
               comando.executeUpdate("""
                     INSERT INTO questao (enunciado, alternativas, gabarito)
                     SELECT 'Pergunta ' || n, ARRAY['A', 'B'], 2 FROM generate_series(1, 6) AS n
                     """);
               conferirSimulado(cliente, inicio);
            }
         } finally {
            comando.execute("DROP SCHEMA " + esquema + " CASCADE");
         }
      }
   }

   /** Verifica cinco perguntas distintas e o placar real através de PostgreSQL e STDIO. */
   private void conferirSimulado(McpSyncClient cliente, CallToolRequest inicio) {
      var json = new JsonMapper();
      var ids = new HashSet<Long>();
      var resultado = cliente.callTool(inicio);
      for (int numero = 1; numero <= 5; numero++) {
         assertFalse(resultado.isError());
         var dados = json.valueToTree(resultado.structuredContent());
         assertEquals(numero, dados.get("numero").asInt());
         assertTrue(ids.add(dados.get("questao").get("id").asLong()));
         assertFalse(dados.get("questao").has("gabarito"));
         assertFalse(dados.has("acertos"));
         var argumentos = Map.<String, Object>of("operacao", "responder", "rodada", dados.get("rodada").asString(),
               "numero", numero, "alternativa", numero <= 3 ? 2 : 1);
         resultado = cliente.callTool(CallToolRequest.builder("praticar_simulado").arguments(argumentos).build());
      }
      assertFalse(resultado.isError());
      assertEquals(json.valueToTree(Map.of("total", 5, "acertos", 3, "erros", 2)), json.valueToTree(resultado.structuredContent()));
   }

   /** Executa a correção da questão sorteada e verifica as marcações recebidas via STDIO. */
   private void conferirCorrecao(McpSyncClient cliente, long id) {
      assertTrue(cliente.listTools().tools().stream().anyMatch(item -> "praticar_questao".equals(item.name())));
      var json = new JsonMapper();
      for (int escolha : List.of(1, 2)) {
         var pedido = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "corrigir", "id", id, "alternativa", escolha)).build();
         var resultado = cliente.callTool(pedido);
         assertFalse(resultado.isError());
         var dados = json.valueToTree(resultado.structuredContent());
         assertEquals(id, dados.get("id").asLong());
         assertEquals(escolha == 2, dados.get("acertou").asBoolean());
         assertEquals(2, dados.get("gabarito").asInt());
         assertFalse(dados.get("alternativas").get(0).get("correta").asBoolean());
         assertTrue(dados.get("alternativas").get(1).get("correta").asBoolean());
      }
      var ausente = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "corrigir", "id", id + 1, "alternativa", 1)).build();
      assertTrue(cliente.callTool(ausente).isError());
   }
}

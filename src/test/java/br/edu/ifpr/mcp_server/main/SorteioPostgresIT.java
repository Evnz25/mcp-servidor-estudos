package br.edu.ifpr.mcp_server.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
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
   void SorteiaSemDivulgarOGabarito() throws Exception {
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
            try (var cliente = McpServer01IT.AbrirCliente(ambiente)) {
               var pedido = CallToolRequest.builder("sortear_questao").arguments(Map.of()).build();
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
               ConferirCorrecao(cliente, id);
            }
         } finally {
            comando.execute("DROP SCHEMA " + esquema + " CASCADE");
         }
      }
   }

   /** Executa a correção da questão sorteada e verifica as marcações recebidas via STDIO. */
   private void ConferirCorrecao(McpSyncClient cliente, long id) {
      assertTrue(cliente.listTools().tools().stream().anyMatch(item -> "corrigir_resposta".equals(item.name())));
      var json = new JsonMapper();
      for (int escolha : List.of(1, 2)) {
         var pedido = CallToolRequest.builder("corrigir_resposta").arguments(Map.of("id", id, "alternativa", escolha)).build();
         var resultado = cliente.callTool(pedido);
         assertFalse(resultado.isError());
         var dados = json.valueToTree(resultado.structuredContent());
         assertEquals(id, dados.get("id").asLong());
         assertEquals(escolha == 2, dados.get("acertou").asBoolean());
         assertEquals(2, dados.get("gabarito").asInt());
         assertFalse(dados.get("alternativas").get(0).get("correta").asBoolean());
         assertTrue(dados.get("alternativas").get(1).get("correta").asBoolean());
      }
      var ausente = CallToolRequest.builder("corrigir_resposta").arguments(Map.of("id", id + 1, "alternativa", 1)).build();
      assertTrue(cliente.callTool(ausente).isError());
   }
}

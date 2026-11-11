package br.edu.ifpr.mcp_server.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
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
            }
         } finally {
            comando.execute("DROP SCHEMA " + esquema + " CASCADE");
         }
      }
   }
}

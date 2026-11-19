package br.edu.ifpr.mcp_server.main;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import br.edu.ifpr.mcp_server.model.Desempenho;
import br.edu.ifpr.mcp_server.mcp.resources.DesempenhoResource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import br.edu.ifpr.mcp_server.model.ResultadoSimulado;
import br.edu.ifpr.mcp_server.mcp.resources.ResultadoSimuladoResource;
import tools.jackson.databind.json.JsonMapper;

@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = ".+")
class SorteioPostgresIT {
   @TempDir Path pasta;
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
            var ambiente = Map.of("BANCO_URL", urlServidor, "DESEMPENHO_ARQUIVO", pasta.resolve("desempenho.json").toString());
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
               var esperado = json.valueToTree(Map.of("id", 1, "enunciado", "Qual estrutura segue FIFO?",
                     "alternativas", List.of("Pilha", "Fila")));
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
               assertEquals(new Desempenho(7, 4, 3, 57.14), lerDesempenho(cliente));
            }
            // O desempenho sobrevive à troca do processo servidor porque é lido do mesmo arquivo.
            try (var cliente = McpServer01IT.abrirCliente(ambiente)) {
               assertEquals(new Desempenho(7, 4, 3, 57.14), lerDesempenho(cliente));
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
      String rodada = json.valueToTree(resultado.structuredContent()).get("rodada").asString();
      assertEquals(new ResultadoSimulado(rodada, false, 5, 0, 0, 0), lerResultado(cliente, rodada));
      var outraRodada = cliente.callTool(inicio);
      String outra = json.valueToTree(outraRodada.structuredContent()).get("rodada").asString();
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
         int acertos = Math.min(numero, 3);
         var esperado = new ResultadoSimulado(rodada, numero == 5, 5, numero, acertos, numero - acertos);
         assertEquals(esperado, lerResultado(cliente, rodada));
         assertEquals(esperado, lerResultado(cliente, rodada));
      }
      assertFalse(resultado.isError());
      assertEquals(json.valueToTree(Map.of("total", 5, "acertos", 3, "erros", 2)), json.valueToTree(resultado.structuredContent()));
      assertEquals(new ResultadoSimulado(outra, false, 5, 0, 0, 0), lerResultado(cliente, outra));
   }

   /** Consulta o Resource pelo protocolo real e desserializa somente os dados públicos do placar. */
   private ResultadoSimulado lerResultado(McpSyncClient cliente, String rodada) {
      String uri = ResultadoSimuladoResource.URI_TEMPLATE.replace("{rodada}", rodada);
      var resposta = cliente.readResource(ReadResourceRequest.builder(uri).build());
      assertEquals(1, resposta.contents().size());
      var conteudo = assertInstanceOf(TextResourceContents.class, resposta.contents().getFirst());
      assertEquals(uri, conteudo.uri());
      assertEquals("application/json", conteudo.mimeType());
      var json = new JsonMapper();
      assertEquals(6, json.readTree(conteudo.text()).size());
      return json.readValue(conteudo.text(), ResultadoSimulado.class);
   }

   /** Executa a correção da questão sorteada e verifica o resultado recebido via STDIO. */
   private void conferirCorrecao(McpSyncClient cliente, long id) {
      assertTrue(cliente.listTools().tools().stream().anyMatch(item -> "praticar_questao".equals(item.name())));
      var json = new JsonMapper();
      for (int escolha : List.of(1, 2)) {
         var pedido = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "corrigir", "id", id,
               "alternativa", escolha)).build();
         var resultado = cliente.callTool(pedido);
         assertFalse(resultado.isError());
         assertEquals(resultado.structuredContent(), cliente.callTool(pedido).structuredContent());
         var dados = json.valueToTree(resultado.structuredContent());
         assertEquals(id, dados.get("id").asLong());
         assertEquals(escolha == 2, dados.get("acertou").asBoolean());
         assertEquals(2, dados.get("gabarito").asInt());
         assertEquals(escolha, dados.get("alternativaEscolhida").asInt());
         assertFalse(dados.has("alternativas"));
      }
      var ausente = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "corrigir", "id", id + 1, "alternativa", 1)).build();
      assertTrue(cliente.callTool(ausente).isError());
   }

   /** Lê o Resource fixo de desempenho pela mesma conexão que executa as ferramentas. */
   private Desempenho lerDesempenho(McpSyncClient cliente) {
      var resposta = cliente.readResource(ReadResourceRequest.builder(DesempenhoResource.URI).build());
      var texto = (TextResourceContents) resposta.contents().getFirst();
      return new JsonMapper().readValue(texto.text(), Desempenho.class);
   }
}

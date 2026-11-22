package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

class McpServer01IT {
   private static final Logger LOG =
                                   LoggerFactory.getLogger(McpServer01IT.class);
   // nome e caminho para o servidor MCP
   private static final File SERVER_JAR = new File(System.getProperty("dist-dir", "server"), "mcp_server01_server.jar");

   private static McpSyncClient client;

   @BeforeAll
   static void setUpBeforeClass() throws Exception {
      // antes que a classe de testes seja carregada, este método providencia
      // alguma inicialização ou alocação de recursos, neste caso, o servidor
      // MCP é executado e o cliente se conecta a ele
      // Força uma falha JDBC reproduzível sem tocar no banco de desenvolvimento.
      client = abrirCliente(Map.of("BANCO_URL", "jdbc:teste:indisponivel"));
   }

   /** Inicia o servidor real via STDIO com o ambiente específico do teste. */
   static McpSyncClient abrirCliente(Map<String, String> ambiente) {
      var ambienteTeste = new HashMap<>(ambiente);
      ambienteTeste.putIfAbsent("DESEMPENHO_ARQUIVO", Path.of("target", "test-desempenho",
            UUID.randomUUID() + ".json").toAbsolutePath().toString());
      File serverJar = SERVER_JAR;
      assertTrue(serverJar.isFile(),"JAR do servidor não encontrado: " +
                                    serverJar.getAbsolutePath());

      String           jarPath = serverJar.getAbsolutePath();
      ServerParameters params  = ServerParameters.builder(Path.of(System.getProperty("java.home"), "bin", "java").toString())
    			                                 .args("-jar",jarPath)
                                                 .env(ambienteTeste)
    			                                 .build();
      JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
    			                                              new JsonMapper());
      StdioClientTransport transport  = new StdioClientTransport(params,
    			                                                 jsonMapper);

      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] " + line));

      McpSyncClient cliente = McpClient.sync(transport).build();
      cliente.initialize();
      return cliente;
   }

   @AfterAll
   static void tearDownAfterClass() throws Exception {
      // ao terminar todos os testes, todos os métodos nesta classe foram
      // executados, o cliente se desconecta do servidor
      if (client != null)
         client.closeGracefully();
   }

   //Exercita o tratamento de erro do sorteio através do protocolo real
   @Test
   void informaBancoIndisponivelNoSorteio() {
      var resultado = client.callTool(CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "sortear")).build());
      assertTrue(resultado.isError());
      var texto = (TextContent) resultado.content().getFirst();
      assertEquals("Não foi possível consultar o banco de questões.", texto.text());
   }

   //Confere que um cliente pode descobrir e obter o prompt, inclusive sem conexão com o banco
   @Test
   void disponibilizaPromptDeExplicacao() {
      assertTrue(client.listPrompts().prompts().stream().anyMatch(item -> "explicar_correcao".equals(item.name())));
      var resultado = client.getPrompt(new GetPromptRequest("explicar_correcao", Map.of()));
      assertEquals(1, resultado.messages().size());
      var mensagem = resultado.messages().getFirst();
      assertEquals(Role.USER, mensagem.role());
      String texto = ((TextContent) mensagem.content()).text();
      assertTrue(texto.contains("praticar_questao"));
      assertTrue(texto.contains("cada uma está certa ou errada"));
   }

   //Confere o catálogo atual em uma única chamada de descoberta
   @Test
   void deveDisponibilizarFerramentas() {
      var ferramentas = client.listTools().tools();
      assertEquals(List.of("cadastrar_questao", "praticar_questao", "praticar_simulado"),
            ferramentas.stream().map(Tool::name).sorted().toList());
      for (var ferramenta : ferramentas) {
         assertFalse(ferramenta.description().isBlank());
         assertNotNull(ferramenta.inputSchema());
      }
   }
}

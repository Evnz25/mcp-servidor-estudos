package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.resources.ResultadoSimuladoResource;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
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
      File serverJar = SERVER_JAR;
      assertTrue(serverJar.isFile(),"JAR do servidor não encontrado: " +
                                    serverJar.getAbsolutePath());

      String           jarPath = serverJar.getAbsolutePath();
      ServerParameters params  = ServerParameters.builder("java")
    			                                 .args("-jar",jarPath)
                                                 .env(ambiente)
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

   /** Exercita o tratamento de erro do sorteio através do protocolo real. */
   @Test
   void informaBancoIndisponivelNoSorteio() {
      assertEquals(List.of("obter_documento", "praticar_questao", "praticar_simulado"),
            client.listTools().tools().stream().map(Tool::name).sorted().toList());
      var resultado = client.callTool(CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "sortear")).build());
      assertTrue(resultado.isError());
      var texto = (TextContent) resultado.content().getFirst();
      assertEquals("Não foi possível consultar o banco de questões.", texto.text());
   }

   // ==========================================================================
   /** Verifica a descoberta e a chamada real da correção quando o banco está indisponível. */
   @Test
   void chamaCorrecaoPeloProtocolo() {
      assertTrue(client.listTools().tools().stream().anyMatch(item -> "praticar_questao".equals(item.name())));
      var pedido = CallToolRequest.builder("praticar_questao").arguments(Map.of("operacao", "corrigir", "id", 1, "alternativa", 1)).build();
      var resultado = client.callTool(pedido);
      assertTrue(resultado.isError());
      assertEquals("Não foi possível consultar o banco de questões.", ((TextContent) resultado.content().getFirst()).text());
   }

   /** Confere que um cliente pode descobrir e obter o prompt, inclusive sem conexão com o banco. */
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

   /** Descobre o template real e confere o erro de leitura sem precisar conectar ao PostgreSQL. */
   @Test
   void disponibilizaResourceDeResultado() {
      var templates = client.listResourceTemplates().resourceTemplates();
      var resultado = templates.stream()
            .filter(item -> ResultadoSimuladoResource.URI_TEMPLATE.equals(item.uriTemplate()))
            .findFirst().orElseThrow();
      assertEquals(ResultadoSimuladoResource.NAME, resultado.name());
      assertEquals("application/json", resultado.mimeType());
      var pedido = ReadResourceRequest.builder(
            "estudos://simulados/00000000-0000-0000-0000-000000000000/resultado").build();
      var erro = assertThrows(McpError.class, () -> client.readResource(pedido));
      assertEquals(ErrorCodes.RESOURCE_NOT_FOUND, erro.getJsonRpcError().code());
   }

   // Os métodos de teste à seguir simulam a aplicação cliente:
   // - há ferramentas (tools) disponíveis???
   // - a ferramenta 'obter_documento' existe???
   // - a ferramenta 'obter_documento' executou com sucesso???
   // ==========================================================================

   @Test
   void deveDisponibilizarFerramentas() {
      ListToolsResult result = client.listTools();

      assertNotNull(result,"O servidor não retornou o resultado.");
      assertNotNull(result.tools(),"A lista de ferramentas não pode ser null.");
      assertFalse(result.tools().isEmpty(),"O servidor deveria disponibilizar" +
                                           " pelo menos uma ferramenta." );
   }

   @Test
   void deveDisponibilizarObterDocumento() {
      ListToolsResult result = client.listTools();
                                      // retorna a lista de ferramentas
      Tool            tool   = result.tools()
                                     // cria um fluxo para percorrer a lista
    			                     .stream()
    			                     // filtra a lista para um elemento
    			                     .filter(item ->
    			                          "obter_documento".equals(item.name()))
    			                     // verifica se há alguma elemento...
    			                     .findFirst()
    			                     // ...caso contrário, retorna "null"
    			                     .orElse(null);

      assertNotNull(tool,"A ferramenta obter_documento não foi encontrada.");
      assertNotNull(tool.description(),"A ferramenta deveria possuir " +
                                       "descrição.");
      assertNotNull(tool.inputSchema(),"A ferramenta deveria possuir " +
                                       "'inputSchema'.");
   }

   @Test
   void deveExecutarObterDocumento() {
      CallToolRequest request = CallToolRequest.builder("obter_documento")
    			                               .arguments(Map.of("id",123))
    			                               .build();
      CallToolResult  result  = client.callTool(request);

      assertNotNull(result,"O servidor não retornou resultado.");
      assertFalse(result.isError(),"A execução da ferramenta não deveria " +
                                   "resultar em erro.");
      assertNotNull(result.content(),"O resultado deveria possuir 'content'.");
      assertFalse(result.content().isEmpty(),"O resultado deveria possuir " +
                                             "pelo menos um 'content'." );
   }
}

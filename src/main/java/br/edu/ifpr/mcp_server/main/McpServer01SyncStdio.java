package br.edu.ifpr.mcp_server.main;

import java.util.List;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.services.estudos.QuestaoRepository;
import br.edu.ifpr.mcp_server.services.estudos.SimuladoService;
import br.edu.ifpr.mcp_server.mcp.resources.ResultadoSimuladoResource;
import br.edu.ifpr.mcp_server.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarQuestaoTool;
import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarSimuladoTool;
import br.edu.ifpr.mcp_server.mcp.tools.SimpleReportTool;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.mcp.utils.McpPromptDefinition;
import br.edu.ifpr.mcp_server.resources.producers.JdbcConnectionProducer;
import br.edu.ifpr.mcp_server.mcp.prompts.ExplicarCorrecaoPrompt;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

public class McpServer01SyncStdio {
   private static final Logger LOG =
                            LoggerFactory.getLogger(McpServer01SyncStdio.class);

   private final McpSyncServer server;
   private final JdbcConnectionProducer connectionProducer = JdbcConnectionProducer.getInstance();

   /** Monta as dependências explicitamente e registra as ferramentas do servidor. */
   private void registerTools(
                          SyncSpecification<SingleSessionSyncSpecification> mcp,
                          QuestaoRepository repositorio, SimuladoService simulados)
                throws Exception {
      // cria a lista de ferramentas disponíveis no servidor...
      List<McpToolDefinition> tools = List.of(new SimpleReportTool(), new PraticarQuestaoTool(repositorio, repositorio),
            new PraticarSimuladoTool(simulados));
      // ...para cada ferramenta, executa seu registro para posterior execução
      for (McpToolDefinition tool : tools)
         if (tool != null)
            tool.register(mcp);
   }

   /** Registra os Resources pelo contrato do professor, compartilhando o estado das ferramentas. */
   private void registerResources(SyncSpecification<SingleSessionSyncSpecification> mcp,
                                  SimuladoService simulados) throws Exception {
      List<McpResourceDefinition> resources = List.of(new ResultadoSimuladoResource(simulados));
      for (McpResourceDefinition resource : resources) resource.register(mcp);
   }

   /** Registra os prompts pela mesma interface utilizada no exemplo do professor. */
   private void registerPrompts(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
      List<McpPromptDefinition> prompts = List.of(new ExplicarCorrecaoPrompt());
      for (McpPromptDefinition prompt : prompts) prompt.register(mcp);
   }

   /** Configura o transporte, a identificação e as capacidades do protocolo MCP. */
   private SyncSpecification<SingleSessionSyncSpecification> configMcpServer() {
      // cria o objeto para comunicação entre servidor e cliente
      JacksonMcpJsonMapper         jsonMapper        =
                                     new JacksonMcpJsonMapper(new JsonMapper());
      // estabelece que a comunicação usará o transporte STDIO
      StdioServerTransportProvider transportProvider =
                                   new StdioServerTransportProvider(jsonMapper);
      // identifica quais "capacidades" o servidor disponibiliza, neste caso,
      // ferramentas, resources, prompts e 'logging'
      /* capacidades, o que o servidor disponibiliza, do servidor
      completions  presente se o servidor suporta sugestões para auto completar
                   argumentos
      experimental capacidades não padronizadas que o servidor suporta!!!
      logging      presente se o servidor suporta envio de mensagen ('logging')
                   ao cliente
      prompts      presente se o servidor oferece modelos ('templates') de
                   promptsPresent if the server offers any prompt templates
      resources    presente se o servidor oferece recursos que podem ser lidos
      tools        presente se o servidor oferece ferramentas
       */
      ServerCapabilities capabilities    = ServerCapabilities.builder()
                                                             .tools(true)
                                                             .prompts(false)
                                                             .resources(false, false)
                                                             .logging()
                                                             .build();
      // identificação do servidor
      Implementation      implementation = Implementation.builder("MCP Server",
    			                                                  "1.0.0")
                                                         .build(); 

      // configura o servidor com transporte STDIO
      return McpServer.sync(transportProvider)
                      .serverInfo(implementation)
                      .capabilities(capabilities);
   }

   /** Inicializa o servidor e registra o descarte da conexão no encerramento da JVM. */
   public McpServer01SyncStdio() throws Exception {
      // configura/inicializa o servidor
      SyncSpecification<SingleSessionSyncSpecification> srv = configMcpServer();
      
      // registra as ferramentas disponíveis neste servidor
      var repositorio = new QuestaoRepository(connectionProducer);
      var simulados = new SimuladoService(repositorio);
      registerTools(srv, repositorio, simulados);
      registerResources(srv, simulados);
      registerPrompts(srv);
      // cria o servidor
      this.server = srv.build();
      Runtime.getRuntime().addShutdownHook(new Thread(this::close, "mcp-shutdown"));

      LOG.info("MCP Server 01 iniciado via STDIO...");
   }

   /** Encerra o transporte e descarta a conexão, mesmo se o fechamento MCP falhar. */
   private void close() {
      try {
         server.closeGracefully();
      } finally {
         try {
            connectionProducer.close();
         } catch (SQLException ex) {
            LOG.error("Falha ao encerrar a conexão JDBC (SQLState {}).", ex.getSQLState());
         }
      }
   }

   /** Inicia a aplicação que será executada pelo cliente ou pelo assistente MCP. */
   public static void main(String[] args) throws Exception {
      new McpServer01SyncStdio();
   }
}

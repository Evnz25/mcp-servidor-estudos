package br.edu.ifpr.mcp_server.ferramentas;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.banco.IRepositorioDeQuestoes;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.modelos.QuestaoParaEstudo;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

public class SortearQuestao implements McpToolDefinition {
   private static final Logger LOG = LoggerFactory.getLogger(SortearQuestao.class);
   private static final JsonMapper JSON = new JsonMapper();
   private final IRepositorioDeQuestoes repositorio;

   /** Recebe o repositório que fornece as questões de estudo. */
   public SortearQuestao(IRepositorioDeQuestoes repositorio) {
      this.repositorio = repositorio;
   }

   /** Publica uma ferramenta sem argumentos, que o cliente pode descobrir pelo MCP. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> servidor) {
      Map<String, Object> entrada = Map.of("type", "object", "properties", Map.of(),
                                          "additionalProperties", false);
      Tool ferramenta = Tool.builder("sortear_questao", entrada)
            .title("Sortear questão")
            .description("Sorteia uma questão de computação com as alternativas na ordem cadastrada.")
            .build();
      servidor.tools(SyncToolSpecification.builder().tool(ferramenta).callHandler(this::run).build());
   }

   /** Devolve a mesma questão nos formatos textual e estruturado do protocolo. */
   @Override
   public CallToolResult run(McpSyncServerExchange troca, CallToolRequest pedido) {
      if (pedido.arguments() != null && !pedido.arguments().isEmpty()) {
         return CallToolResult.builder().isError(true)
               .addTextContent("O sorteio não recebe argumentos.").build();
      }
      try {
         Optional<QuestaoParaEstudo> sorteio = repositorio.Sortear();
         if (sorteio.isEmpty()) {
            return CallToolResult.builder().addTextContent("Nenhuma questão cadastrada.").build();
         }
         QuestaoParaEstudo questao = sorteio.orElseThrow();
         return CallToolResult.builder().structuredContent(questao)
               .addTextContent(JSON.writeValueAsString(questao)).build();
      } catch (SQLException erro) {
         LOG.warn("Falha ao sortear questão no PostgreSQL (SQLState {}).", erro.getSQLState());
         return CallToolResult.builder().isError(true)
               .addTextContent("Não foi possível consultar o banco de questões.").build();
      }
   }
}

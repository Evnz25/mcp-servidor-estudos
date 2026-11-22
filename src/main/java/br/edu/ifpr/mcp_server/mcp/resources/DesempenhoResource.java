package br.edu.ifpr.mcp_server.mcp.resources;

import java.io.IOException;
import java.util.List;

import br.edu.ifpr.mcp_server.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_server.repositories.IConsultaDeDesempenho;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import tools.jackson.databind.json.JsonMapper;

// resource de URI fixa, com leitura do histórico local
public final class DesempenhoResource implements McpResourceDefinition {
   public static final String URI = "estudos://desempenho";
   private static final JsonMapper JSON = new JsonMapper();
   private final IConsultaDeDesempenho desempenho;

   //recebe somente a consulta; a leitura do Resource não registra respostas.
   public DesempenhoResource(IConsultaDeDesempenho desempenho) {
      this.desempenho = desempenho;
   }

   //Registra o recurso fixo na lista normal de Resources, sem parâmetros de rodada
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) {
      Resource resource = Resource.builder(URI, "desempenho")
            .title("Desempenho geral nos estudos")
            .description("Total de respostas, acertos, erros e percentual de acertos do usuário local, salvo em arquivo.")
            .mimeType("application/json").build();
      SyncResourceSpecification spec = new SyncResourceSpecification(resource, this::read);
      mcp.resources(spec);
   }

   //Lê o arquivo pelo repositório e entrega o resumo em JSON, sem consultar PostgreSQL.
   @Override
   public ReadResourceResult read(McpSyncServerExchange exchange, ReadResourceRequest request) {
      if (!URI.equals(request.uri())) throw McpError.RESOURCE_NOT_FOUND.apply(request.uri());
      try {
         var text = TextResourceContents.builder(URI, JSON.writeValueAsString(desempenho.consultar()))
               .mimeType("application/json").build();
         return ReadResourceResult.builder(List.of(text)).build();
      } catch (IOException ex) {
         throw McpError.builder(ErrorCodes.INTERNAL_ERROR)
               .message("Não foi possível ler o arquivo de desempenho. Verifique o arquivo e suas permissões.").build();
      }
   }
}

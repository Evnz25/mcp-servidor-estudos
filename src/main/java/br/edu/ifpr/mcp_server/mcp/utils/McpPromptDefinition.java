package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;

/** Contrato de prompts com a mesma organização do projeto MCP do professor. */
public interface McpPromptDefinition {
   /** Registra a definição do prompt e o método que produzirá sua resposta. */
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception;

   /** Produz as instruções que serão apresentadas ao assistente de IA. */
   GetPromptResult run(McpSyncServerExchange exchange, GetPromptRequest request);
}

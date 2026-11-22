package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;

//Contrato de Resources
public interface McpResourceDefinition {
   //Registra o recurso e o método que fornecerá seu conteúdo ao cliente
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception;

   //Obtém o conteúdo solicitado sem executar uma nova ação de estudo
   ReadResourceResult read(McpSyncServerExchange exchange, ReadResourceRequest request);
}

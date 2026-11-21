package br.edu.ifpr.mcp_server.mcp.resources;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import br.edu.ifpr.mcp_server.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_server.services.estudos.SimuladoService;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import tools.jackson.databind.json.JsonMapper;

/** Disponibiliza o placar por URI, seguindo o contrato register/read do professor. */
public final class ResultadoSimuladoResource implements McpResourceDefinition {
   public static final String URI_TEMPLATE = "estudos://simulados/{rodada}/resultado";
   public static final String NAME = "resultado_simulado";
   public static final String MIME_TYPE = "application/json";
   private static final Pattern URI_PATTERN = Pattern.compile(
         "estudos://simulados/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/resultado");
   private static final JsonMapper JSON = new JsonMapper();
   private final SimuladoService service;

   /** Recebe a mesma instância de serviço utilizada pela ferramenta de simulado. */
   public ResultadoSimuladoResource(SimuladoService service) {
      this.service = Objects.requireNonNull(service);
   }

   /** Publica um template porque cada rodada possui uma URI própria. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) {
      ResourceTemplate resource = ResourceTemplate.builder(URI_TEMPLATE, NAME)
            .title("Resultado do simulado")
            .description("Consulte o placar usando a rodada retornada por praticar_simulado. "
                  + "A leitura não registra respostas. Disponível enquanto a rodada estiver na memória deste servidor.")
            .mimeType(MIME_TYPE)
            .build();
      SyncResourceTemplateSpecification spec = new SyncResourceTemplateSpecification(resource, this::read);
      mcp.resourceTemplates(spec);
   }

   /** Retorna uma fotografia do placar ou o erro MCP de recurso inexistente (-32002). */
   @Override
   public ReadResourceResult read(McpSyncServerExchange exchange, ReadResourceRequest request) {
      String uri = request.uri();
      String rodada = lerRodada(uri);
      var resultado = service.consultarResultado(rodada)
            .orElseThrow(() -> McpError.RESOURCE_NOT_FOUND.apply(uri));
      TextResourceContents text = TextResourceContents.builder(uri, JSON.writeValueAsString(resultado))
            .mimeType(MIME_TYPE)
            .build();
      return ReadResourceResult.builder(List.of(text)).build();
   }

   /** Aceita somente o endereço de resultado e o formato de identificador emitido pelo servidor. */
   private String lerRodada(String uri) {
      var matcher = URI_PATTERN.matcher(uri);
      if (!matcher.matches()) throw McpError.RESOURCE_NOT_FOUND.apply(uri);
      return matcher.group(1);
   }
}

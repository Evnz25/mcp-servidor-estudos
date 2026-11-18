package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static br.edu.ifpr.mcp_server.mcp.tools.estudos.RespostaDaFerramenta.*;

import java.sql.SQLException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.services.estudos.ISorteioDeSimulado;
import br.edu.ifpr.mcp_server.services.estudos.SimuladoService;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.model.Simulado;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public final class PraticarSimuladoTool implements McpToolDefinition {
   public static final String NOME = "praticar_simulado";
   private static final Logger LOG = LoggerFactory.getLogger(PraticarSimuladoTool.class);
   private final SimuladoService service;

   /** Recebe a consulta usada para preparar cada rodada. */
   public PraticarSimuladoTool(ISorteioDeSimulado repositorio) {
      this(new SimuladoService(repositorio));
   }

   /** Compartilha as rodadas com o Resource registrado pelo servidor. */
   public PraticarSimuladoTool(SimuladoService service) {
      this.service = Objects.requireNonNull(service);
   }

   /** Orienta o Claude a aguardar cada resposta e a apresentar o placar ao final. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> servidor) {
      var propriedades = Map.of(
            "operacao", Map.of("type", "string", "enum", List.of("iniciar", "responder")),
            "rodada", Map.of("type", "string", "description", "Identificador devolvido ao iniciar."),
            "numero", Map.of("type", "integer", "minimum", 1, "maximum", Simulado.TOTAL),
            "alternativa", Map.of("type", "integer", "minimum", 1));
      Map<String, Object> entrada = Map.of("type", "object", "properties", propriedades,
            "required", List.of("operacao"), "additionalProperties", false);
      var ferramenta = Tool.builder(NOME, entrada).title("Simulado de cinco questões")
            .description("Use iniciar para sortear cinco questões. Mostre ao aluno somente a questão atual, "
                  + "com o número e as alternativas numeradas a partir de 1, e aguarde a resposta. "
                  + "Se a escolha for ambígua, peça esclarecimento. Depois use responder com rodada e numero "
                  + "da questão apresentada e a posição da alternativa escolhida. Repita a cada questão. "
                  + "Ao receber o placar, informe quantas questões o aluno acertou e quantas errou. "
                  + "Para consultar novamente, leia o Resource estudos://simulados/{rodada}/resultado "
                  + "usando o identificador desta rodada.")
            .build();
      servidor.tools(SyncToolSpecification.builder().tool(ferramenta).callHandler(this::run).build());
   }

   /** Valida os dados de cada operação antes de alterar o andamento do simulado. */
   @Override
   public CallToolResult run(McpSyncServerExchange troca, CallToolRequest pedido) {
      var argumentos = pedido.arguments();
      if (argumentos == null) return erro("Informe a operação iniciar ou responder.");
      try {
         if ("iniciar".equals(argumentos.get("operacao")) && argumentos.size() == 1) {
            return resultado(service.iniciar());
         }
         if ("responder".equals(argumentos.get("operacao"))
               && argumentos.keySet().equals(Set.of("operacao", "rodada", "numero", "alternativa"))) {
            if (!(argumentos.get("rodada") instanceof String rodada)) {
               return erro("Rodada não encontrada. Inicie um novo simulado.");
            }
            int numero = (int) lerInteiro(argumentos, "numero", Simulado.TOTAL);
            int alternativa = (int) lerInteiro(argumentos, "alternativa", Integer.MAX_VALUE);
            return resultado(service.responder(rodada, numero, alternativa));
         }
         return erro("Use iniciar sem outros argumentos ou responder com rodada, numero e alternativa.");
      } catch (IllegalArgumentException erro) {
         return erro(erro.getMessage());
      } catch (SQLException erro) {
         LOG.warn("Falha ao sortear simulado no PostgreSQL (SQLState {}).", erro.getSQLState());
         return erro("Não foi possível consultar o banco de questões.");
      } catch (IOException erro) {
         return erro("Não foi possível salvar o desempenho no arquivo. Reenvie a mesma resposta da rodada.");
      }
   }
}

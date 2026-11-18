package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static br.edu.ifpr.mcp_server.mcp.tools.estudos.RespostaDaFerramenta.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.model.NovaQuestao;
import br.edu.ifpr.mcp_server.services.estudos.ICadastroDeQuestao;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;

/** Adapta o cadastro de questões ao contrato register/run usado pelo professor. */
public final class CadastrarQuestaoTool implements McpToolDefinition {
   public static final String NOME = "cadastrar_questao";
   private static final Logger LOG = LoggerFactory.getLogger(CadastrarQuestaoTool.class);
   private final ICadastroDeQuestao cadastro;

   /** Recebe apenas a operação de escrita necessária, permitindo testar sem banco real. */
   public CadastrarQuestaoTool(ICadastroDeQuestao cadastro) {
      this.cadastro = Objects.requireNonNull(cadastro);
   }

   /** Publica os campos exigidos e sinaliza que esta ferramenta grava uma nova linha a cada chamada. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) {
      var propriedades = Map.of(
            "enunciado", Map.of("type", "string", "minLength", 1,
                  "description", "Texto completo da pergunta fornecida pelo usuário."),
            "alternativas", Map.of("type", "array", "minItems", NovaQuestao.MIN_ALTERNATIVAS,
                  "maxItems", NovaQuestao.MAX_ALTERNATIVAS, "items", Map.of("type", "string", "minLength", 1),
                  "description", "Textos das alternativas na ordem A, B, C... sem os rótulos."),
            "gabarito", Map.of("anyOf", List.of(
                  Map.of("type", "integer", "minimum", 1, "maximum", NovaQuestao.MAX_ALTERNATIVAS),
                  Map.of("type", "string", "pattern", "^\\s*([A-Za-z]|[1-9][0-9]*)\\s*$")),
                  "description", "Letra ou posição da alternativa correta: B ou 2 representam a segunda alternativa."));
      Map<String, Object> entrada = Map.of("type", "object", "properties", propriedades,
            "required", List.of("enunciado", "alternativas", "gabarito"), "additionalProperties", false);
      var ferramenta = Tool.builder(NOME, entrada).title("Cadastrar questão")
            .description("Adiciona ao banco uma questão fornecida pelo usuário com enunciado, alternativas e gabarito. "
                  + "Preserve o conteúdo e a ordem das alternativas; solicite os campos que estiverem ausentes. "
                  + "Retorna o ID e os dados gravados. Cada chamada bem-sucedida cria uma nova questão.")
            .annotations(ToolAnnotations.builder().readOnlyHint(false).destructiveHint(false)
                  .idempotentHint(false).openWorldHint(false).build())
            .build();
      mcp.tools(SyncToolSpecification.builder().tool(ferramenta).callHandler(this::run).build());
   }

   /** Valida os dados antes da escrita e só informa sucesso depois que o JDBC retorna o ID. */
   @Override
   public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
      try {
         NovaQuestao questao = lerQuestao(request.arguments());
         long id = cadastro.cadastrar(questao);
         return resultado(Map.of("id", id, "enunciado", questao.getEnunciado(),
               "alternativas", questao.getAlternativas(), "gabarito", questao.getGabarito()));
      } catch (IllegalArgumentException ex) {
         return erro(ex.getMessage());
      } catch (SQLException ex) {
         LOG.warn("Falha ao cadastrar questão no PostgreSQL (SQLState {}).", ex.getSQLState());
         return erro("42501".equals(ex.getSQLState())
               ? "O usuário do banco não tem permissão para cadastrar questões. Solicite permissão de INSERT na tabela questao."
               : "Não foi possível cadastrar a questão no banco de dados.");
      }
   }

   /** Converte os tipos recebidos do protocolo sem aceitar campos extras ou alternativas não textuais. */
   private NovaQuestao lerQuestao(Map<String, Object> argumentos) {
      if (argumentos == null || !argumentos.keySet().equals(Set.of("enunciado", "alternativas", "gabarito"))) {
         throw new IllegalArgumentException("Informe somente enunciado, alternativas e gabarito.");
      }
      if (!(argumentos.get("enunciado") instanceof String enunciado)) {
         throw new IllegalArgumentException("O enunciado deve ser um texto.");
      }
      if (!(argumentos.get("alternativas") instanceof List<?> lista)) {
         throw new IllegalArgumentException("As alternativas devem ser uma lista de textos.");
      }
      var alternativas = new ArrayList<String>();
      for (Object item : lista) {
         if (!(item instanceof String texto)) throw new IllegalArgumentException("Cada alternativa deve ser um texto.");
         alternativas.add(texto);
      }
      int gabarito = argumentos.get("gabarito") instanceof String texto
            ? NovaQuestao.converterGabarito(texto)
            : (int) lerInteiro(argumentos, "gabarito", NovaQuestao.MAX_ALTERNATIVAS);
      return new NovaQuestao(enunciado, alternativas, gabarito);
   }
}

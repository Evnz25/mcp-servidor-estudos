package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static br.edu.ifpr.mcp_server.mcp.tools.estudos.RespostaDaFerramenta.*;

import java.sql.SQLException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import br.edu.ifpr.mcp_server.model.QuestaoSorteada;
import br.edu.ifpr.mcp_server.model.RegistroResposta;
import br.edu.ifpr.mcp_server.services.estudos.IRegistroDeResposta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.services.estudos.IConsultaDeGabarito;
import br.edu.ifpr.mcp_server.services.estudos.IQuestaoRepository;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class PraticarQuestaoTool implements McpToolDefinition {
   private static final Logger LOG = LoggerFactory.getLogger(PraticarQuestaoTool.class);
   public static final String NOME = "praticar_questao";
   private final IQuestaoRepository questoes;
   private final IConsultaDeGabarito gabaritos;
   private final IRegistroDeResposta historico;

   /** Recebe as consultas usadas no sorteio e na correção. */
   public PraticarQuestaoTool(IQuestaoRepository questoes, IConsultaDeGabarito gabaritos) {
      this(questoes, gabaritos, resposta -> {});
   }

   /** Recebe o registro em arquivo usado pelo servidor; testes podem fornecer um registrador em memória. */
   public PraticarQuestaoTool(IQuestaoRepository questoes, IConsultaDeGabarito gabaritos, IRegistroDeResposta historico) {
      this.questoes = questoes;
      this.gabaritos = gabaritos;
      this.historico = historico;
   }

   /** Publica os argumentos e orienta o agente sobre o uso pedagógico do resultado. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> servidor) {
      var propriedades = Map.of(
            "operacao", Map.of("type", "string", "enum", List.of("sortear", "corrigir")),
            "tentativa", Map.of("type", "string", "description", "Reutilize a tentativa retornada pelo sorteio para não contar reenvios duas vezes."),
            "id", Map.of("type", "integer", "format", "int64", "minimum", 1, "maximum", Long.MAX_VALUE,
                         "description", "Na correção, reutilize o ID retornado pelo sorteio nesta conversa."),
            "alternativa", Map.of("type", "integer", "minimum", 1, "maximum", Integer.MAX_VALUE,
                                  "description", "Posição escolhida pelo aluno, começando em 1."));
      Map<String, Object> entrada = Map.of("type", "object", "properties", propriedades,
            "required", List.of("operacao"), "additionalProperties", false);
      Tool ferramenta = Tool.builder(NOME, entrada).title("Praticar questão")
            .description("Para iniciar, use sortear. Apresente o enunciado e as alternativas numeradas e aguarde. "
                       + "Quando o aluno responder, use corrigir com o ID e a tentativa do sorteio desta conversa e a posição "
                       + "da alternativa escolhida, contando a partir de 1. Nunca peça o ID ao aluno. Se a escolha "
                       + "for ambígua, esclareça apenas a alternativa. Use a correção do banco para explicar "
                       + "por que cada alternativa está certa ou errada. Reutilize a mesma tentativa ao repetir a correção. "
                       + "O desempenho acumulado pode ser lido no Resource estudos://desempenho. Se a consulta falhar, informe o erro.")
            .build();
      servidor.tools(SyncToolSpecification.builder().tool(ferramenta).callHandler(this::run).build());
   }

   /** Valida a escolha antes de divulgar a correção e devolve o mesmo conteúdo em texto e JSON. */
   @Override
   public CallToolResult run(McpSyncServerExchange troca, CallToolRequest pedido) {
      var argumentos = pedido.arguments();
      if (argumentos == null || !(argumentos.get("operacao") instanceof String operacao)) {
         return erro("Informe a operação sortear ou corrigir.");
      }
      try {
         return switch (operacao) {
            case "sortear" -> argumentos.size() == 1 ? sortear() : erro("O sorteio recebe apenas a operação.");
            case "corrigir" -> corrigir(argumentos);
            default -> erro("Operação desconhecida. Use sortear ou corrigir.");
         };
      } catch (IllegalArgumentException erro) {
         return erro(erro.getMessage());
      } catch (SQLException erro) {
         LOG.warn("Falha ao praticar questão no PostgreSQL (SQLState {}).", erro.getSQLState());
         return erro("Não foi possível consultar o banco de questões.");
      } catch (IOException erro) {
         return erro("Não foi possível salvar o desempenho no arquivo. Tente novamente com a mesma tentativa.");
      }
   }

   /** Envia somente os campos que podem ser apresentados antes da resposta do aluno. */
   private CallToolResult sortear() throws SQLException {
      var questao = questoes.sortear();
      if (questao.isEmpty()) {
         return CallToolResult.builder().addTextContent("Nenhuma questão cadastrada.").build();
      }
      var sorteada = questao.orElseThrow();
      return resultado(new QuestaoSorteada(sorteada.id(), sorteada.enunciado(), sorteada.alternativas(),
                                          UUID.randomUUID().toString()));
   }

   /** Usa o ID associado pelo cliente à questão apresentada e confere o gabarito cadastrado. */
   private CallToolResult corrigir(Map<String, Object> argumentos) throws SQLException, IOException {
      if (!argumentos.keySet().equals(Set.of("operacao", "id", "alternativa"))
            && !argumentos.keySet().equals(Set.of("operacao", "id", "alternativa", "tentativa"))) {
         return erro("A correção exige o ID retornado pelo sorteio e a alternativa escolhida.");
      }
      long id = lerInteiro(argumentos, "id", Long.MAX_VALUE);
      int escolha = (int) lerInteiro(argumentos, "alternativa", Integer.MAX_VALUE);
      String tentativa = lerTentativa(argumentos);
      var questao = gabaritos.buscarPorId(id);
      if (questao.isEmpty()) {
         return erro("Questão não encontrada.");
      }
      var correcao = questao.orElseThrow().corrigir(escolha);
      historico.registrar(new RegistroResposta("questao:" + tentativa, id, escolha, correcao.acertou()));
      return resultado(correcao);
   }

   /** Aceita o UUID do sorteio; clientes antigos sem tentativa registram uma nova resposta a cada chamada. */
   private String lerTentativa(Map<String, Object> argumentos) {
      if (!argumentos.containsKey("tentativa")) return UUID.randomUUID().toString();
      if (argumentos.get("tentativa") instanceof String texto) {
         try {
            var uuid = UUID.fromString(texto);
            if (uuid.toString().equalsIgnoreCase(texto)) return uuid.toString();
         } catch (IllegalArgumentException ex) {
            // A mensagem abaixo orienta o cliente a usar a tentativa recebida no sorteio.
         }
      }
      throw new IllegalArgumentException("Use a tentativa retornada pelo sorteio.");
   }
}

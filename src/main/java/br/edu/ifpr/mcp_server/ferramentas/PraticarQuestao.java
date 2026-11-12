package br.edu.ifpr.mcp_server.ferramentas;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.banco.IConsultaDeGabarito;
import br.edu.ifpr.mcp_server.banco.IRepositorioDeQuestoes;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

public class PraticarQuestao implements McpToolDefinition {
   private static final Logger LOG = LoggerFactory.getLogger(PraticarQuestao.class);
   private static final JsonMapper JSON = new JsonMapper();
   public static final String NOME = "praticar_questao";
   private final IRepositorioDeQuestoes questoes;
   private final IConsultaDeGabarito gabaritos;

   /** Recebe as consultas usadas no sorteio e na correção. */
   public PraticarQuestao(IRepositorioDeQuestoes questoes, IConsultaDeGabarito gabaritos) {
      this.questoes = questoes;
      this.gabaritos = gabaritos;
   }

   /** Publica os argumentos e orienta o agente sobre o uso pedagógico do resultado. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> servidor) {
      var propriedades = Map.of(
            "operacao", Map.of("type", "string", "enum", List.of("sortear", "corrigir")),
            "id", Map.of("type", "integer", "format", "int64", "minimum", 1, "maximum", Long.MAX_VALUE,
                         "description", "Na correção, reutilize o ID retornado pelo sorteio nesta conversa."),
            "alternativa", Map.of("type", "integer", "minimum", 1, "maximum", Integer.MAX_VALUE,
                                  "description", "Posição escolhida pelo aluno, começando em 1."));
      Map<String, Object> entrada = Map.of("type", "object", "properties", propriedades,
            "required", List.of("operacao"), "additionalProperties", false);
      Tool ferramenta = Tool.builder(NOME, entrada).title("Praticar questão")
            .description("Para iniciar, use sortear. Apresente o enunciado e as alternativas numeradas e aguarde. "
                       + "Quando o aluno responder, use corrigir com o ID do sorteio desta conversa e a posição "
                       + "da alternativa escolhida, contando a partir de 1. Nunca peça o ID ao aluno. Se a escolha "
                       + "for ambígua, esclareça apenas a alternativa. Use a correção do banco para explicar "
                       + "por que cada alternativa está certa ou errada. Se a consulta falhar, informe o erro.")
            .build();
      servidor.tools(SyncToolSpecification.builder().tool(ferramenta).callHandler(this::run).build());
   }

   /** Valida a escolha antes de divulgar a correção e devolve o mesmo conteúdo em texto e JSON. */
   @Override
   public CallToolResult run(McpSyncServerExchange troca, CallToolRequest pedido) {
      var argumentos = pedido.arguments();
      if (argumentos == null || !(argumentos.get("operacao") instanceof String operacao)) {
         return Erro("Informe a operação sortear ou corrigir.");
      }
      try {
         return switch (operacao) {
            case "sortear" -> argumentos.size() == 1 ? Sortear() : Erro("O sorteio recebe apenas a operação.");
            case "corrigir" -> Corrigir(argumentos);
            default -> Erro("Operação desconhecida. Use sortear ou corrigir.");
         };
      } catch (IllegalArgumentException erro) {
         return Erro(erro.getMessage());
      } catch (SQLException erro) {
         LOG.warn("Falha ao praticar questão no PostgreSQL (SQLState {}).", erro.getSQLState());
         return Erro("Não foi possível consultar o banco de questões.");
      }
   }

   /** Envia somente os campos que podem ser apresentados antes da resposta do aluno. */
   private CallToolResult Sortear() throws SQLException {
      var questao = questoes.Sortear();
      if (questao.isEmpty()) {
         return CallToolResult.builder().addTextContent("Nenhuma questão cadastrada.").build();
      }
      return Resultado(questao.orElseThrow());
   }

   /** Usa o ID associado pelo cliente à questão apresentada e confere o gabarito cadastrado. */
   private CallToolResult Corrigir(Map<String, Object> argumentos) throws SQLException {
      if (!argumentos.keySet().equals(Set.of("operacao", "id", "alternativa"))) {
         return Erro("A correção exige o ID retornado pelo sorteio e a alternativa escolhida.");
      }
      long id = LerInteiro(argumentos, "id", Long.MAX_VALUE);
      int escolha = (int) LerInteiro(argumentos, "alternativa", Integer.MAX_VALUE);
      var questao = gabaritos.BuscarPorId(id);
      if (questao.isEmpty()) {
         return Erro("Questão não encontrada.");
      }
      return Resultado(questao.orElseThrow().Corrigir(escolha));
   }

   /** Entrega o mesmo resultado aos clientes de conteúdo textual e estruturado. */
   private static CallToolResult Resultado(Object dados) {
      return CallToolResult.builder().structuredContent(dados).addTextContent(JSON.writeValueAsString(dados)).build();
   }

   /** Converte inteiros JSON sem arredondar frações ou truncar valores grandes. */
   private static long LerInteiro(Map<String, Object> argumentos, String nome, long maximo) {
      if (argumentos.get(nome) instanceof Number numero) {
         try {
            long valor = new BigDecimal(numero.toString()).longValueExact();
            if (valor >= 1 && valor <= maximo) {
               return valor;
            }
         } catch (NumberFormatException | ArithmeticException ignorado) {
            // O erro abaixo indica ao cliente qual argumento precisa ser corrigido.
         }
      }
      throw new IllegalArgumentException("O argumento " + nome + " deve ser um inteiro entre 1 e " + maximo + ".");
   }

   /** Mantém os erros de entrada e de consulta fora do conteúdo estruturado da correção. */
   private static CallToolResult Erro(String mensagem) {
      return CallToolResult.builder().isError(true).addTextContent(mensagem).build();
   }
}

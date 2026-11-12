package br.edu.ifpr.mcp_server.ferramentas;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.banco.IConsultaDeGabarito;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

public class CorrigirResposta implements McpToolDefinition {
   private static final Logger LOG = LoggerFactory.getLogger(CorrigirResposta.class);
   private static final JsonMapper JSON = new JsonMapper();
   private final IConsultaDeGabarito repositorio;

   /** Recebe a consulta usada para conferir o gabarito oficial. */
   public CorrigirResposta(IConsultaDeGabarito repositorio) {
      this.repositorio = repositorio;
   }

   /** Publica os argumentos e orienta o agente sobre o uso pedagógico do resultado. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> servidor) {
      var propriedades = Map.of(
            "id", Map.of("type", "integer", "format", "int64", "minimum", 1, "maximum", Long.MAX_VALUE,
                         "description", "ID recebido no sorteio."),
            "alternativa", Map.of("type", "integer", "minimum", 1, "maximum", Integer.MAX_VALUE,
                                  "description", "Posição escolhida pelo aluno, começando em 1."));
      Map<String, Object> entrada = Map.of("type", "object", "properties", propriedades,
            "required", List.of("id", "alternativa"), "additionalProperties", false);
      Tool ferramenta = Tool.builder("corrigir_resposta", entrada).title("Corrigir resposta")
            .description("Use após o aluno escolher uma alternativa. Informe se ele acertou e explique, "
                       + "para cada alternativa, por que ela está certa ou errada à luz do enunciado "
                       + "e do gabarito retornados.")
            .build();
      servidor.tools(SyncToolSpecification.builder().tool(ferramenta).callHandler(this::run).build());
   }

   /** Valida a escolha antes de divulgar a correção e devolve o mesmo conteúdo em texto e JSON. */
   @Override
   public CallToolResult run(McpSyncServerExchange troca, CallToolRequest pedido) {
      var argumentos = pedido.arguments();
      if (argumentos == null || !argumentos.keySet().equals(Set.of("id", "alternativa"))) {
         return Erro("Informe os argumentos id e alternativa.");
      }
      try {
         long id = LerInteiro(argumentos, "id", Long.MAX_VALUE);
         int escolha = (int) LerInteiro(argumentos, "alternativa", Integer.MAX_VALUE);
         var questao = repositorio.BuscarPorId(id);
         if (questao.isEmpty()) {
            return Erro("Questão não encontrada.");
         }
         var correcao = questao.orElseThrow().Corrigir(escolha);
         return CallToolResult.builder().structuredContent(correcao)
               .addTextContent(JSON.writeValueAsString(correcao)).build();
      } catch (IllegalArgumentException erro) {
         return Erro(erro.getMessage());
      } catch (SQLException erro) {
         LOG.warn("Falha ao consultar gabarito no PostgreSQL (SQLState {}).", erro.getSQLState());
         return Erro("Não foi possível consultar o banco de questões.");
      }
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

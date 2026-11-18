package br.edu.ifpr.mcp_server.main;

import java.io.PrintStream;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarQuestaoTool;
import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarSimuladoTool;
import br.edu.ifpr.mcp_server.model.Simulado;
import br.edu.ifpr.mcp_server.model.QuestaoComGabarito.Correcao;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;
import br.edu.ifpr.mcp_server.model.QuestaoSorteada;
import br.edu.ifpr.mcp_server.model.Desempenho;
import br.edu.ifpr.mcp_server.mcp.resources.DesempenhoResource;
import br.edu.ifpr.mcp_server.model.ResultadoSimulado;
import br.edu.ifpr.mcp_server.mcp.resources.ResultadoSimuladoResource;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.ErrorCodes;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

final class PraticaNoConsole {
   private static final JsonMapper JSON = new JsonMapper();

   /** Mantém o ID associado ao sorteio desta rodada e solicita ao aluno somente a alternativa. */
   static void executar(Scanner entrada, PrintStream saida, Function<CallToolRequest, CallToolResult> chamar) {
      var sorteio = chamar.apply(CallToolRequest.builder(PraticarQuestaoTool.NOME)
            .arguments(Map.of("operacao", "sortear")).build());
      if (exibirAviso(sorteio, saida)) return;

      var sorteada = JSON.convertValue(sorteio.structuredContent(), QuestaoSorteada.class);
      var questao = new QuestaoParaEstudo(sorteada.id(), sorteada.enunciado(), sorteada.alternativas());
      int alternativa = apresentarQuestao(entrada, saida, questao);
      if (alternativa == 0) return;
      var resultado = chamar.apply(CallToolRequest.builder(PraticarQuestaoTool.NOME)
            .arguments(Map.of("operacao", "corrigir", "id", questao.id(), "alternativa", alternativa,
                              "tentativa", sorteada.tentativa())).build());
      if (exibirAviso(resultado, saida)) return;
      var correcao = JSON.convertValue(resultado.structuredContent(), Correcao.class);
      saida.println(correcao.acertou() ? "Você acertou!" : "Você errou.");
      var correta = correcao.alternativas().get(correcao.gabarito() - 1);
      saida.printf("Gabarito: %d - %s%n", correta.posicao(), correta.texto());
   }

   /** Conduz as cinco perguntas com o identificador e a numeração devolvidos pelo servidor. */
   static void executarSimulado(Scanner entrada, PrintStream saida, Function<CallToolRequest, CallToolResult> chamar) {
      var pedido = CallToolRequest.builder(PraticarSimuladoTool.NOME).arguments(Map.of("operacao", "iniciar")).build();
      while (true) {
         var resultado = chamar.apply(pedido);
         if (exibirAviso(resultado, saida)) return;
         var dados = JSON.valueToTree(resultado.structuredContent());
         if (dados.has("acertos")) {
            saida.printf("Acertos: %d. Erros: %d.%n", dados.get("acertos").asInt(), dados.get("erros").asInt());
            return;
         }
         var pergunta = JSON.convertValue(dados, Simulado.Pergunta.class);
         if (pergunta.numero() == 1) {
            saida.println("Para consultar seu desempenho geral no menu, use: desempenho");
         }
         saida.printf("Questão %d de %d%n", pergunta.numero(), pergunta.total());
         int alternativa = apresentarQuestao(entrada, saida, pergunta.questao());
         if (alternativa == 0) return;
         pedido = CallToolRequest.builder(PraticarSimuladoTool.NOME).arguments(Map.of("operacao", "responder",
               "rodada", pergunta.rodada(), "numero", pergunta.numero(), "alternativa", alternativa)).build();
      }
   }

   /** Consulta o Resource fixo, sem exigir o identificador de uma rodada. */
   static void consultarDesempenho(PrintStream saida, Function<ReadResourceRequest, ReadResourceResult> ler) {
      try {
         var resposta = ler.apply(ReadResourceRequest.builder(DesempenhoResource.URI).build());
         var texto = (TextResourceContents) resposta.contents().getFirst();
         var dados = JSON.readValue(texto.text(), Desempenho.class);
         saida.printf("Respondidas: %d. Acertos: %d. Erros: %d. Aproveitamento: %.2f%%.%n",
               dados.respondidas(), dados.acertos(), dados.erros(), dados.percentualAcertos());
      } catch (RuntimeException ex) {
         saida.println("Não foi possível ler o desempenho. Verifique o arquivo e suas permissões.");
      }
   }

   /** Lê o Resource da rodada e apresenta seu placar sem enviar outra resposta ao simulado. */
   static void consultarResultado(String rodada, PrintStream saida,
                                  Function<ReadResourceRequest, ReadResourceResult> ler) {
      String uri = ResultadoSimuladoResource.URI_TEMPLATE.replace("{rodada}", rodada);
      try {
         var resposta = ler.apply(ReadResourceRequest.builder(uri).build());
         var conteudo = (TextResourceContents) resposta.contents().getFirst();
         var resultado = JSON.readValue(conteudo.text(), ResultadoSimulado.class);
         saida.println(resultado.finalizado() ? "Simulado finalizado." : "Simulado em andamento.");
         saida.printf("Respondidas: %d de %d. Acertos: %d. Erros: %d.%n",
               resultado.respondidas(), resultado.total(), resultado.acertos(), resultado.erros());
      } catch (McpError erro) {
         saida.println(erro.getJsonRpcError().code() == ErrorCodes.RESOURCE_NOT_FOUND
               ? "Resultado não encontrado. Use o código de uma rodada disponível nesta execução do servidor."
               : "Não foi possível consultar o resultado do simulado.");
      } catch (RuntimeException erro) {
         saida.println("Não foi possível consultar o resultado do simulado.");
      }
   }

   /** Apresenta as alternativas na ordem cadastrada antes de ler a escolha. */
   private static int apresentarQuestao(Scanner entrada, PrintStream saida, QuestaoParaEstudo questao) {
      saida.println(questao.enunciado());
      int quantidade = questao.alternativas().size();
      for (int i = 0; i < quantidade; i++) {
         saida.printf("%d - %s%n", i + 1, questao.alternativas().get(i));
      }
      return lerAlternativa(entrada, saida, quantidade);
   }

   /** Lê uma posição válida ou devolve zero quando o aluno sai da rodada. */
   private static int lerAlternativa(Scanner entrada, PrintStream saida, int quantidade) {
      while (true) {
         saida.printf("Sua alternativa (1 a %d, ou Enter para voltar): ", quantidade);
         if (!entrada.hasNextLine()) return 0;
         String resposta = entrada.nextLine().trim();
         if (resposta.isEmpty()) return 0;
         try {
            int alternativa = Integer.parseInt(resposta);
            if (alternativa >= 1 && alternativa <= quantidade) return alternativa;
         } catch (NumberFormatException ignorado) {
            // Permite corrigir a entrada mantendo a questão apresentada.
         }
         saida.printf("Escolha uma alternativa entre 1 e %d.%n", quantidade);
      }
   }

   /** Exibe a mensagem do servidor quando não há questão ou correção para apresentar. */
   private static boolean exibirAviso(CallToolResult resultado, PrintStream saida) {
      if (!Boolean.TRUE.equals(resultado.isError()) && resultado.structuredContent() != null) return false;
      for (var conteudo : resultado.content()) {
         if (conteudo instanceof TextContent texto) saida.println(texto.text());
      }
      return true;
   }
}

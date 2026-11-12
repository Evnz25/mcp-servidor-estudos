package br.edu.ifpr.mcp_server.main;

import java.io.PrintStream;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import br.edu.ifpr.mcp_server.ferramentas.PraticarQuestao;
import br.edu.ifpr.mcp_server.modelos.QuestaoComGabarito.Correcao;
import br.edu.ifpr.mcp_server.modelos.QuestaoParaEstudo;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

final class PraticaNoConsole {
   private static final JsonMapper JSON = new JsonMapper();

   /** Mantém o ID associado ao sorteio desta rodada e solicita ao aluno somente a alternativa. */
   static void Executar(Scanner entrada, PrintStream saida, Function<CallToolRequest, CallToolResult> chamar) {
      var sorteio = chamar.apply(CallToolRequest.builder(PraticarQuestao.NOME)
            .arguments(Map.of("operacao", "sortear")).build());
      if (ExibirAviso(sorteio, saida)) return;

      var questao = JSON.convertValue(sorteio.structuredContent(), QuestaoParaEstudo.class);
      saida.println(questao.enunciado());
      int quantidade = questao.alternativas().size();
      for (int i = 0; i < quantidade; i++) {
         saida.printf("%d - %s%n", i + 1, questao.alternativas().get(i));
      }
      int alternativa = LerAlternativa(entrada, saida, quantidade);
      if (alternativa == 0) return;
      var resultado = chamar.apply(CallToolRequest.builder(PraticarQuestao.NOME)
            .arguments(Map.of("operacao", "corrigir", "id", questao.id(), "alternativa", alternativa)).build());
      if (ExibirAviso(resultado, saida)) return;
      var correcao = JSON.convertValue(resultado.structuredContent(), Correcao.class);
      saida.println(correcao.acertou() ? "Você acertou!" : "Você errou.");
      var correta = correcao.alternativas().get(correcao.gabarito() - 1);
      saida.printf("Gabarito: %d - %s%n", correta.posicao(), correta.texto());
   }

   /** Lê uma posição válida ou devolve zero quando o aluno sai da rodada. */
   private static int LerAlternativa(Scanner entrada, PrintStream saida, int quantidade) {
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
   private static boolean ExibirAviso(CallToolResult resultado, PrintStream saida) {
      if (!Boolean.TRUE.equals(resultado.isError()) && resultado.structuredContent() != null) return false;
      for (var conteudo : resultado.content()) {
         if (conteudo instanceof TextContent texto) saida.println(texto.text());
      }
      return true;
   }
}

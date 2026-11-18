package br.edu.ifpr.mcp_server.main;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.CadastrarQuestaoTool;
import br.edu.ifpr.mcp_server.model.NovaQuestao;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

/** Entrada guiada de questões, sem exigir que o aluno escreva um array JSON. */
final class CadastroNoConsole {
   private static final JsonMapper JSON = new JsonMapper();

   /** Coleta uma questão completa e envia exatamente uma chamada de cadastro. */
   static void executar(Scanner entrada, PrintStream saida, Function<CallToolRequest, CallToolResult> chamar) {
      saida.println("Digite /cancelar em qualquer etapa para voltar sem cadastrar.");
      String enunciado = lerEnunciado(entrada, saida);
      if (enunciado == null) return;
      List<String> alternativas = lerAlternativas(entrada, saida);
      if (alternativas == null) return;
      NovaQuestao questao = lerGabarito(entrada, saida, enunciado, alternativas);
      if (questao == null) return;
      var pedido = CallToolRequest.builder(CadastrarQuestaoTool.NOME)
            .arguments(Map.of("enunciado", questao.enunciado(), "alternativas", questao.alternativas(),
                              "gabarito", questao.gabarito())).build();
      try {
         var resultado = chamar.apply(pedido);
         if (Boolean.TRUE.equals(resultado.isError())) {
            for (var conteudo : resultado.content()) {
               if (conteudo instanceof TextContent texto) saida.println(texto.text());
            }
            return;
         }
         long id = JSON.valueToTree(resultado.structuredContent()).get("id").asLong();
         saida.printf("Questão cadastrada. ID: %d. Gabarito: %c (%d).%n",
               id, 'A' + questao.gabarito() - 1, questao.gabarito());
      } catch (RuntimeException ex) {
         // Não repete a chamada: a gravação pode ter ocorrido antes de uma falha de comunicação.
         saida.println("Não foi possível confirmar o cadastro. Confira o banco antes de tentar novamente.");
      }
   }

   /** Preserva parágrafos do enunciado até uma linha contendo somente ponto. */
   private static String lerEnunciado(Scanner entrada, PrintStream saida) {
      saida.println("Enunciado (digite . em uma linha separada para terminar):");
      var texto = new StringBuilder();
      while (true) {
         String linha = lerLinha(entrada);
         if (linha == null) return null;
         if (linha.indexOf('\0') >= 0) {
            saida.println("O enunciado não pode conter caracteres nulos. Digite a linha novamente.");
            continue;
         }
         if (linha.strip().equals(".")) {
            if (!texto.toString().isBlank()) return texto.toString().strip();
            saida.println("Informe um enunciado antes de terminar.");
         } else {
            if (!texto.isEmpty()) texto.append('\n');
            texto.append(linha);
         }
      }
   }

   /** Lê os textos na ordem A a Z; Enter encerra a lista após pelo menos duas alternativas. */
   private static List<String> lerAlternativas(Scanner entrada, PrintStream saida) {
      saida.println("Alternativas (2 a 26; Enter em branco termina a lista):");
      var alternativas = new ArrayList<String>();
      while (alternativas.size() < NovaQuestao.MAX_ALTERNATIVAS) {
         saida.printf("%c) ", 'A' + alternativas.size());
         String linha = lerLinha(entrada);
         if (linha == null) return null;
         if (linha.indexOf('\0') >= 0) {
            saida.println("A alternativa não pode conter caracteres nulos. Digite novamente.");
            continue;
         }
         if (linha.isBlank()) {
            if (alternativas.size() >= NovaQuestao.MIN_ALTERNATIVAS) break;
            saida.println("Informe pelo menos duas alternativas.");
         } else {
            alternativas.add(linha.strip());
         }
      }
      return alternativas;
   }

   /** Converte a letra ou posição e permite corrigir um gabarito fora das alternativas informadas. */
   private static NovaQuestao lerGabarito(Scanner entrada, PrintStream saida, String enunciado,
                                         List<String> alternativas) {
      while (true) {
         saida.print("Gabarito (letra ou posição, por exemplo B ou 2): ");
         String linha = lerLinha(entrada);
         if (linha == null) return null;
         try {
            return new NovaQuestao(enunciado, alternativas, NovaQuestao.converterGabarito(linha));
         } catch (IllegalArgumentException ex) {
            saida.println(ex.getMessage());
         }
      }
   }

   /** Trata o cancelamento e o fim da entrada antes que qualquer dado seja enviado ao servidor. */
   private static String lerLinha(Scanner entrada) {
      if (!entrada.hasNextLine()) return null;
      String linha = entrada.nextLine();
      return linha.strip().equalsIgnoreCase("/cancelar") ? null : linha;
   }
}

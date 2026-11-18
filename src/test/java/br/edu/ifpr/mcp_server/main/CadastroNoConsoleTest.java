package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.CadastrarQuestaoTool;
import br.edu.ifpr.mcp_server.model.NovaQuestao;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class CadastroNoConsoleTest {
   /** Preserva parágrafos, coleta as alternativas em ordem e permite corrigir um gabarito inválido. */
   @Test
   void cadastraUmaUnicaVezComEntradaGuiada() {
      var gravadas = new ArrayList<NovaQuestao>();
      var tool = new CadastrarQuestaoTool(questao -> { gravadas.add(questao); return 10; });
      var tela = new ByteArrayOutputStream();
      try (var entrada = new Scanner("Enunciado longo.\nSegundo parágrafo.\n.\nCircuit Breaker\nAPI Gateway\n\nZ\nB\n");
           var saida = new PrintStream(tela)) {
         CadastroNoConsole.executar(entrada, saida, pedido -> tool.run(null, pedido));
      }
      assertEquals(List.of(new NovaQuestao("Enunciado longo.\nSegundo parágrafo.",
            List.of("Circuit Breaker", "API Gateway"), 2)), gravadas);
      assertTrue(tela.toString().contains("Questão cadastrada. ID: 10. Gabarito: B (2)."));
   }

   /** Cancelar ou encerrar a entrada em qualquer etapa não grava uma questão incompleta. */
   @ParameterizedTest
   @ValueSource(strings = { "", "/cancelar\n", "Pergunta\n", "Pergunta\n.\n/cancelar\n",
         "Pergunta\n.\nA\nB\n\n/cancelar\n", "Pergunta\n.\nA\nB\n\n" })
   void cancelaSemChamarOFerramenta(String texto) {
      try (var entrada = new Scanner(texto); var saida = new PrintStream(new ByteArrayOutputStream())) {
         CadastroNoConsole.executar(entrada, saida, pedido -> fail("Não deve enviar um cadastro incompleto."));
      }
   }

   /** Mensagens de falha do servidor são mostradas sem mensagem de sucesso. */
   @Test
   void exibeErroDoServidor() {
      var tela = new ByteArrayOutputStream();
      try (var entrada = new Scanner("Pergunta\n.\nA\nB\n\n1\n"); var saida = new PrintStream(tela)) {
         CadastroNoConsole.executar(entrada, saida, pedido ->
               CallToolResult.builder().isError(true).addTextContent("Banco indisponível.").build());
      }
      assertTrue(tela.toString().contains("Banco indisponível."));
      assertFalse(tela.toString().contains("Questão cadastrada."));
   }
}

package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.CadastrarQuestaoTool;
import br.edu.ifpr.mcp_server.model.NovaQuestao;

class CadastroNoConsoleTest {
   // Trata /cancelar como texto e permite corrigir um gabarito inválido
   @Test
   void cadastraUmaUnicaVezComEntradaGuiada() {
      var gravadas = new ArrayList<NovaQuestao>();
      var tool = new CadastrarQuestaoTool(questao -> { gravadas.add(questao); return 10; });
      var tela = new ByteArrayOutputStream();
      try (var entrada = new Scanner("Enunciado longo.\n/cancelar\n.\nCircuit Breaker\n/cancelar\n\n/cancelar\nB\n");
           var saida = new PrintStream(tela)) {
         CadastroNoConsole.executar(entrada, saida, pedido -> tool.run(null, pedido));
      }
      assertEquals(List.of(new NovaQuestao("Enunciado longo.\n/cancelar",
            List.of("Circuit Breaker", "/cancelar"), 2)), gravadas);
      assertTrue(tela.toString().contains("Questão cadastrada. ID: 10. Gabarito: B (2)."));
      assertFalse(tela.toString().contains("Digite /cancelar"));
   }
}

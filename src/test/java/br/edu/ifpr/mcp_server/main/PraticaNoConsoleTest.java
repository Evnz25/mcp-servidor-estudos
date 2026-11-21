package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.mcp.resources.ResultadoSimuladoResource;
import br.edu.ifpr.mcp_server.services.estudos.SimuladoService;
import io.modelcontextprotocol.spec.McpError;
import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarQuestaoTool;
import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarSimuladoTool;
import br.edu.ifpr.mcp_server.model.Questao;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;

class PraticaNoConsoleTest {
   /** O aluno pode corrigir uma entrada inválida e responder sem saber o ID do sorteio. */
   @Test
   void corrigeComOIdDoSorteio() {
      var questao = new QuestaoParaEstudo(3_000_000_001L, "Qual estrutura segue FIFO?", List.of("Pilha", "Fila"));
      var consultados = new ArrayList<Long>();
      var ferramenta = new PraticarQuestaoTool(() -> Optional.of(questao), id -> {
         consultados.add(id);
         return Optional.of(new Questao(questao, 2));
      });
      var tela = new ByteArrayOutputStream();
      try (var entrada = new Scanner("3\n2\n"); var saida = new PrintStream(tela)) {
         PraticaNoConsole.executar(entrada, saida, pedido -> ferramenta.run(null, pedido));
      }
      assertEquals(List.of(questao.getId()), consultados);
      assertTrue(tela.toString().contains("Você acertou!"));
      assertTrue(tela.toString().contains("Gabarito: 2 - Fila"));
      assertFalse(tela.toString().contains(String.valueOf(questao.getId())));
   }

   /** Percorre o simulado pelo menu de prática e confere a apresentação do resultado final. */
   @Test
   void apresentaCincoPerguntasEOPlacar() {
      var simulado = new PraticarSimuladoTool(n -> IntStream.rangeClosed(1, n)
            .mapToObj(i -> new Questao(new QuestaoParaEstudo(i, "Pergunta " + i, List.of("A", "B")), 1))
            .toList());
      var tela = new ByteArrayOutputStream();
      try (var entrada = new Scanner("1\n2\n2\n2\n1\n"); var saida = new PrintStream(tela)) {
         PraticaNoConsole.executarSimulado(entrada, saida, pedido -> simulado.run(null, pedido));
      }
      String texto = tela.toString();
      for (int numero = 1; numero <= 5; numero++) {
         assertTrue(texto.contains("Questão " + numero + " de 5"));
      }
      assertTrue(texto.stripTrailing().endsWith("Acertos: 2. Erros: 3."));
      assertTrue(texto.contains("Para consultar seu desempenho geral no menu, use: desempenho"));
   }

   /** O comando de consulta apresenta o placar do simulado. */
   @Test
   void consultaResultadoNoConsole() throws Exception {
      var service = new SimuladoService(n -> IntStream.rangeClosed(1, n)
            .mapToObj(i -> new Questao(new QuestaoParaEstudo(i, "Pergunta", List.of("A", "B")), 1))
            .toList());
      String rodada = service.iniciar().getRodada();
      for (int i = 1; i <= 5; i++) service.responder(rodada, i, 1);
      var resource = new ResultadoSimuladoResource(service);
      var tela = new ByteArrayOutputStream();
      try (var saida = new PrintStream(tela)) {
         PraticaNoConsole.consultarResultado(rodada, saida, pedido -> resource.read(null, pedido));
      }
      String texto = tela.toString();
      assertTrue(texto.contains("Total: 5. Acertos: 5. Erros: 0."));
   }

   /** Informa que a rodada não existe sem encerrar a interação no console. */
   @Test
   void informaResultadoAusente() {
      var tela = new ByteArrayOutputStream();
      try (var saida = new PrintStream(tela)) {
         PraticaNoConsole.consultarResultado("ausente", saida, pedido -> {
            throw McpError.RESOURCE_NOT_FOUND.apply(pedido.uri());
         });
      }
      assertTrue(tela.toString().contains("Resultado não encontrado."));
   }

   /** Uma falha do transporte é apresentada como erro de consulta, sem fabricar um placar. */
   @Test
   void informaFalhaNaLeituraDoResultado() {
      var tela = new ByteArrayOutputStream();
      try (var saida = new PrintStream(tela)) {
         PraticaNoConsole.consultarResultado("rodada", saida, pedido -> {
            throw new IllegalStateException("Transporte encerrado");
         });
      }
      assertTrue(tela.toString().contains("Não foi possível consultar o resultado do simulado."));
      assertFalse(tela.toString().contains("Acertos:"));
   }
}

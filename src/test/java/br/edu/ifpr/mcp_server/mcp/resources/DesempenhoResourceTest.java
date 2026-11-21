package br.edu.ifpr.mcp_server.mcp.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarQuestaoTool;
import br.edu.ifpr.mcp_server.model.Desempenho;
import br.edu.ifpr.mcp_server.model.Questao;
import br.edu.ifpr.mcp_server.model.Correcao;
import br.edu.ifpr.mcp_server.services.estudos.DesempenhoArquivoRepository;
import br.edu.ifpr.mcp_server.services.estudos.SimuladoService;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import tools.jackson.databind.json.JsonMapper;

class DesempenhoResourceTest {
   @TempDir Path pasta;
   private final JsonMapper json = new JsonMapper();

   @Test
   void contaAcertosEErrosRepetidosEPreservaTotaisAoReabrirHistorico() throws Exception {
      Path arquivo = pasta.resolve("desempenho.json");
      var historico = new DesempenhoArquivoRepository(arquivo);
      var questao = new Questao(7, "FIFO?", List.of("Pilha", "Fila"), 2);
      var ferramenta = new PraticarQuestaoTool(() -> Optional.of(questao.paraEstudo()),
            id -> Optional.of(questao), historico);
      assertEquals(new Desempenho(0, 0, 0, 0), ler(historico));

      for (int escolha : List.of(2, 2, 1, 1)) {
         var resultado = ferramenta.run(null, CallToolRequest.builder(PraticarQuestaoTool.NOME)
               .arguments(Map.of("operacao", "corrigir", "id", 7, "alternativa", escolha)).build());
         assertFalse(resultado.isError());
      }
      var invalida = ferramenta.run(null, CallToolRequest.builder(PraticarQuestaoTool.NOME)
            .arguments(Map.of("operacao", "corrigir", "id", 7, "alternativa", 3)).build());
      assertTrue(invalida.isError());

      var esperado = new Desempenho(4, 2, 2, 50);
      assertEquals(esperado, ler(historico));
      assertEquals(esperado, ler(historico));
      assertEquals(esperado, ler(new DesempenhoArquivoRepository(arquivo)));
      var registros = json.readTree(Files.readString(arquivo));
      assertEquals(4, registros.size());
      for (var registro : registros) {
         assertEquals(5, registro.size());
         assertEquals(7, registro.get("id").asLong());
         assertEquals("FIFO?", registro.get("enunciado").asString());
         assertTrue(registro.has("alternativaEscolhida"));
         assertEquals(2, registro.get("gabarito").asInt());
         assertTrue(registro.has("acertou"));
      }
   }

   @Test
   void rejeitaHistoricoAntigoSemSobrescreverArquivo() throws Exception {
      Path arquivo = pasta.resolve("antigo.json");
      for (String original : List.of(
            "[{\"questao\":7,\"alternativa\":2,\"acertou\":true}]",
            "[{\"tentativa\":\"questao:abc\",\"questao\":7,\"alternativa\":2,\"acertou\":true}]")) {
         Files.writeString(arquivo, original);
         var historico = new DesempenhoArquivoRepository(arquivo);
         assertThrows(IOException.class, historico::consultar);
         assertThrows(IOException.class, () -> historico.registrar(new Correcao(7, "P", 1, 2, false)));
         assertEquals(original, Files.readString(arquivo));
      }
   }

   @Test
   void rejeitaHistoricoIncompletoSemSobrescreverArquivo() throws Exception {
      Path arquivo = pasta.resolve("invalido.json");
      String original = "[{\"id\":7,\"enunciado\":\"P\",\"alternativaEscolhida\":2,\"gabarito\":2}]";
      Files.writeString(arquivo, original);
      var historico = new DesempenhoArquivoRepository(arquivo);
      assertThrows(IOException.class, () -> historico.registrar(new Correcao(7, "P", 2, 2, true)));
      assertEquals(original, Files.readString(arquivo));
   }

   @Test
   void registraRespostasDoSimulado() throws Exception {
      var historico = new DesempenhoArquivoRepository(pasta.resolve("simulado.json"));
      var service = new SimuladoService(n -> IntStream.rangeClosed(1, n)
            .mapToObj(i -> new Questao(i, "P", List.of("A", "B"), 2)).toList(), historico);
      String rodada = service.iniciar().getRodada();
      for (int numero = 1; numero <= 5; numero++) service.responder(rodada, numero, 2);
      assertEquals(new Desempenho(5, 5, 0, 100), ler(historico));
      assertEquals(5, service.consultarResultado(rodada).orElseThrow().getAcertos());
   }

   private Desempenho ler(DesempenhoArquivoRepository historico) {
      var resource = new DesempenhoResource(historico);
      var resposta = resource.read(null, ReadResourceRequest.builder(DesempenhoResource.URI).build());
      var conteudo = assertInstanceOf(TextResourceContents.class, resposta.contents().getFirst());
      return json.readValue(conteudo.text(), Desempenho.class);
   }
}

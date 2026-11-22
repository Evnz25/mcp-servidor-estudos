package br.edu.ifpr.mcp_server.mcp.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_server.mcp.tools.estudos.PraticarQuestaoTool;
import br.edu.ifpr.mcp_server.model.Desempenho;
import br.edu.ifpr.mcp_server.model.Questao;
import br.edu.ifpr.mcp_server.model.Correcao;
import br.edu.ifpr.mcp_server.repositories.DesempenhoArquivoRepository;
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

   //Um registro incompleto não deve causar perda do histórico já salvo
   @Test
   void rejeitaHistoricoInvalidoSemSobrescreverArquivo() throws Exception {
      String original = "[{\"id\":7,\"enunciado\":\"P\",\"alternativaEscolhida\":2,\"gabarito\":2}]";
      Path arquivo = pasta.resolve("invalido.json");
      Files.writeString(arquivo, original);
      var historico = new DesempenhoArquivoRepository(arquivo);
      assertThrows(IOException.class, historico::consultar);
      assertThrows(IOException.class, () -> historico.registrar(new Correcao(7, "P", 2, 2, true)));
      assertEquals(original, Files.readString(arquivo));
   }

   private Desempenho ler(DesempenhoArquivoRepository historico) {
      var resource = new DesempenhoResource(historico);
      var resposta = resource.read(null, ReadResourceRequest.builder(DesempenhoResource.URI).build());
      var conteudo = assertInstanceOf(TextResourceContents.class, resposta.contents().getFirst());
      return json.readValue(conteudo.text(), Desempenho.class);
   }
}

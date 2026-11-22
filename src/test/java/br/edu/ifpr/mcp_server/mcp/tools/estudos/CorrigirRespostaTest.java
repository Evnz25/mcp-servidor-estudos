package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_server.repositories.IConsultaDeGabarito;
import br.edu.ifpr.mcp_server.model.Questao;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class CorrigirRespostaTest {
   private static final long ID = 3_000_000_000L;
   private final QuestaoParaEstudo questao = new QuestaoParaEstudo(ID, "Qual estrutura segue FIFO?",
                                                                  List.of("Pilha", "Fila", "Árvore"));
   private final PraticarQuestaoTool ferramenta = correcao(id -> {
      assertEquals(ID, id);
      return Optional.of(new Questao(questao, 2));
   });

   private PraticarQuestaoTool correcao(IConsultaDeGabarito gabaritos) {
      return new PraticarQuestaoTool(() -> fail("Uma correção não sorteia outra questão."), gabaritos);
   }

   //Confere todos os campos retornados tanto para acerto quanto para erro do aluno
   @ParameterizedTest
   @ValueSource(ints = {1, 2})
   void corrigeCadaEscolha(int escolha) {
      var resultado = chamar(ferramenta, Map.of("id", ID, "alternativa", escolha));
      assertFalse(resultado.isError());
      var json = new JsonMapper();
      var esperado = json.readTree("""
            {"id":3000000000,"enunciado":"Qual estrutura segue FIFO?","alternativaEscolhida":%d,
             "gabarito":2,"acertou":%s}
            """.formatted(escolha, escolha == 2));
      assertEquals(esperado, json.valueToTree(resultado.structuredContent()));
      assertEquals(esperado, json.readTree(((TextContent) resultado.content().getFirst()).text()));
   }

   //Uma posição inexistente informa os limites sem revelar o gabarito
   @Test
   void rejeitaEscolhaForaDaQuestao() {
      conferirErro(chamar(ferramenta, Map.of("id", ID, "alternativa", 4)), "Escolha uma alternativa entre 1 e 3.");
   }

   //Monta uma chamada com os mesmos argumentos usados pelo cliente MCP
   private CallToolResult chamar(PraticarQuestaoTool alvo, Map<String, Object> argumentos) {
      var entrada = new LinkedHashMap<>(argumentos);
      entrada.put("operacao", "corrigir");
      return alvo.run(null, CallToolRequest.builder("praticar_questao").arguments(entrada).build());
   }

   //Confere todo o conteúdo de uma resposta de erro
   private void conferirErro(CallToolResult resultado, String mensagem) {
      assertTrue(resultado.isError());
      assertNull(resultado.structuredContent());
      assertEquals(1, resultado.content().size());
      assertEquals(mensagem, ((TextContent) resultado.content().getFirst()).text());
   }
}

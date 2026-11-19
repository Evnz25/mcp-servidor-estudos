package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_server.services.estudos.IConsultaDeGabarito;
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

   /** Confere todos os campos retornados tanto para acerto quanto para erro do aluno. */
   @ParameterizedTest
   @ValueSource(ints = {1, 2, 3})
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

   /** Entradas incompletas, fracionárias ou fora do tipo numérico aceito nunca chegam ao banco. */
   @Test
   void rejeitaEntradasInvalidas() {
      var semConsulta = correcao(id -> fail("Não deveria consultar o banco."));
      List<Map<String, Object>> invalidos = List.of(Map.of(), Map.of("id", 1),
            Map.of("id", 1, "alternativa", 1, "extra", true), Map.of("id", 0, "alternativa", 1),
            Map.of("id", "1", "alternativa", 1), Map.of("id", 1, "alternativa", 1.5),
            Map.of("id", 1, "alternativa", 0), Map.of("id", 1, "alternativa", 3_000_000_000L),
            Map.of("id", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), "alternativa", 1));
      for (var argumentos : invalidos) {
         var resultado = chamar(semConsulta, argumentos);
         assertTrue(resultado.isError());
         assertNull(resultado.structuredContent());
      }
   }

   /** Uma posição inexistente informa os limites sem revelar o gabarito. */
   @Test
   void rejeitaEscolhaForaDaQuestao() {
      conferirErro(chamar(ferramenta, Map.of("id", ID, "alternativa", 4)), "Escolha uma alternativa entre 1 e 3.");
   }

   /** Distingue ausência da questão de indisponibilidade da consulta. */
   @Test
   void informaFalhasDaConsulta() {
      var pedido = Map.<String, Object>of("id", ID, "alternativa", 1);
      conferirErro(chamar(correcao(id -> Optional.empty()), pedido), "Questão não encontrada.");
      var indisponivel = correcao(id -> { throw new SQLException("Detalhes internos"); });
      conferirErro(chamar(indisponivel, pedido), "Não foi possível consultar o banco de questões.");
   }

   /** Monta uma chamada com os mesmos argumentos usados pelo cliente MCP. */
   private CallToolResult chamar(PraticarQuestaoTool alvo, Map<String, Object> argumentos) {
      var entrada = new LinkedHashMap<>(argumentos);
      entrada.put("operacao", "corrigir");
      return alvo.run(null, CallToolRequest.builder("praticar_questao").arguments(entrada).build());
   }

   /** Confere todo o conteúdo de uma resposta de erro. */
   private void conferirErro(CallToolResult resultado, String mensagem) {
      assertTrue(resultado.isError());
      assertNull(resultado.structuredContent());
      assertEquals(1, resultado.content().size());
      assertEquals(mensagem, ((TextContent) resultado.content().getFirst()).text());
   }
}

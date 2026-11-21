package br.edu.ifpr.mcp_server.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class ModelJsonTest {
   private final JsonMapper json = new JsonMapper();

   @Test
   void preservaContratoJsonDaCorrecaoSemAlternativas() {
      String documento = """
            {"id":7,"enunciado":"P","alternativaEscolhida":2,"gabarito":2,"acertou":true}
            """;
      var esperada = new Questao(new QuestaoParaEstudo(7, "P", List.of("A", "B")), 2).corrigir(2);
      var lida = json.readValue(documento, Correcao.class);

      assertEquals(esperada, lida);
      assertEquals(esperada.hashCode(), lida.hashCode());
      assertEquals(json.readTree(documento), json.readTree(json.writeValueAsString(lida)));
   }

   @Test
   void preservaContratoJsonDoHistorico() {
      String documento = """
            {"id":7,"enunciado":"P","alternativaEscolhida":2,"gabarito":2,"acertou":true}
            """;
      var esperado = new Correcao(7, "P", 2, 2, true);
      var lido = json.readValue(documento, Correcao.class);

      assertEquals(esperado, lido);
      assertEquals(esperado.hashCode(), lido.hashCode());
      assertEquals(json.readTree(documento), json.readTree(json.writeValueAsString(lido)));
   }

   @ParameterizedTest
   @MethodSource("documentosInvalidos")
   void mantemValidacaoAoLerJson(String documento, Class<?> modelo) {
      assertThrows(JacksonException.class, () -> json.readValue(documento, modelo));
   }

   static Stream<Arguments> documentosInvalidos() {
      return Stream.of(
            Arguments.of("{}", NovaQuestao.class),
            Arguments.of("{\"enunciado\":\"P\",\"alternativas\":[\"A\",\"B\"],\"gabarito\":3}", NovaQuestao.class),
            Arguments.of("{\"id\":7,\"enunciado\":\"P\"}", QuestaoParaEstudo.class));
   }
}

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
   void preservaContratoJsonDaCorrecaoComClassesSeparadas() {
      String documento = """
            {"id":7,"enunciado":"P","alternativaEscolhida":2,"gabarito":2,"acertou":true,
             "alternativas":[{"posicao":1,"texto":"A","correta":false},
                             {"posicao":2,"texto":"B","correta":true}]}
            """;
      var esperada = new QuestaoComGabarito(new QuestaoParaEstudo(7, "P", List.of("A", "B")), 2).corrigir(2);
      var lida = json.readValue(documento, Correcao.class);

      assertEquals(esperada, lida);
      assertEquals(esperada.hashCode(), lida.hashCode());
      assertEquals(json.readTree(documento), json.readTree(json.writeValueAsString(lida)));
   }

   @Test
   void preservaContratoJsonDoHistorico() {
      String documento = """
            {"tentativa":"questao:abc","questao":7,"alternativa":2,"acertou":true}
            """;
      var esperado = new RegistroResposta("questao:abc", 7, 2, true);
      var lido = json.readValue(documento, RegistroResposta.class);

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
            Arguments.of("{}", RegistroResposta.class),
            Arguments.of("{\"tentativa\":\"abc\",\"questao\":0,\"alternativa\":1,\"acertou\":false}", RegistroResposta.class),
            Arguments.of("{\"tentativa\":\"abc\",\"questao\":1,\"alternativa\":0,\"acertou\":false}", RegistroResposta.class),
            Arguments.of("{\"tentativa\":\" \",\"questao\":1,\"alternativa\":1,\"acertou\":false}", RegistroResposta.class),
            Arguments.of("{}", NovaQuestao.class),
            Arguments.of("{\"enunciado\":\"P\",\"alternativas\":[\"A\",\"B\"],\"gabarito\":3}", NovaQuestao.class),
            Arguments.of("{\"id\":7,\"enunciado\":\"P\"}", QuestaoParaEstudo.class));
   }
}

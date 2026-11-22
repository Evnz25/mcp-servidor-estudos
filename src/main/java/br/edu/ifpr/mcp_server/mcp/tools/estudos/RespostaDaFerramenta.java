package br.edu.ifpr.mcp_server.mcp.tools.estudos;

import java.math.BigDecimal;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import tools.jackson.databind.json.JsonMapper;

final class RespostaDaFerramenta {
   private static final JsonMapper JSON = new JsonMapper();

   // Entrega o mesmo resultado aos clientes de conteúdo textual e estruturado
   static CallToolResult resultado(Object dados) {
      return CallToolResult.builder().structuredContent(dados).addTextContent(JSON.writeValueAsString(dados)).build();
   }

   //Converte inteiros JSON sem arredondar frações ou truncar valores grandes
   static long lerInteiro(Map<String, Object> argumentos, String nome, long maximo) {
      if (argumentos.get(nome) instanceof Number numero) {
         try {
            long valor = new BigDecimal(numero.toString()).longValueExact();
            if (valor >= 1 && valor <= maximo) {
               return valor;
            }
         } catch (NumberFormatException | ArithmeticException ignorado) {
            // O erro abaixo indica ao cliente qual argumento precisa ser corrigido.
         }
      }
      throw new IllegalArgumentException("O argumento " + nome + " deve ser um inteiro entre 1 e " + maximo + ".");
   }

   //Mantém os erros de entrada e de consulta fora do conteúdo estruturado do resultado
   static CallToolResult erro(String mensagem) {
      return CallToolResult.builder().isError(true).addTextContent(mensagem).build();
   }
}

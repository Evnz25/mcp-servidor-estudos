package br.edu.ifpr.mcp_server.prompts;

import java.util.List;

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class ExplicarCorrecao {
   /** Cria o prompt que o aluno pode selecionar na conversa sobre a questão atual. */
   public static SyncPromptSpecification Criar() {
      var prompt = new Prompt("explicar_correcao", "Explique a correção da questão que respondi.", List.of());
      var instrucoes = """
            Use a questão e a alternativa que escolhi nesta conversa. Se faltar o ID da questão
            ou a minha escolha estiver ausente ou ambígua, peça o dado que falta e aguarde.
            Use o resultado de corrigir_resposta para esse ID e essa escolha. Se ainda não houver
            esse resultado, chame a ferramenta com o ID e a posição da alternativa, começando em 1.
            Se a ferramenta retornar um erro, informe o problema e aguarde antes de avaliar a resposta.
            Com a correção em mãos, diga se acertei. Percorra as alternativas na ordem apresentada:
            explique com suas palavras por que cada uma está certa ou errada, relacionando seu texto
            ao conceito que o enunciado cobra. Use o gabarito retornado como referência.
            """;
      var mensagem = new PromptMessage(Role.USER, TextContent.builder(instrucoes).build());
      return new SyncPromptSpecification(prompt, (troca, pedido) ->
            new GetPromptResult(prompt.description(), List.of(mensagem)));
   }
}

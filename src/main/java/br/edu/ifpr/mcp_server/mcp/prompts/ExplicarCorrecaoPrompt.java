package br.edu.ifpr.mcp_server.mcp.prompts;

import java.util.List;

import br.edu.ifpr.mcp_server.mcp.utils.McpPromptDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class ExplicarCorrecaoPrompt implements McpPromptDefinition {
   private static final String DESCRIPTION = "Explique a correção da questão que respondi.";

   /** Registra o prompt seguindo o mesmo contrato register/run das ferramentas. */
   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) {
      var prompt = Prompt.builder("explicar_correcao").description(DESCRIPTION).arguments(List.of()).build();
      mcp.prompts(new SyncPromptSpecification(prompt, this::run));
   }

   /** Cria as instruções para explicar a questão atual da conversa. */
   @Override
   public GetPromptResult run(McpSyncServerExchange exchange, GetPromptRequest request) {
      var instrucoes = """
            Use a questão sorteada nesta conversa. Reaproveite seu ID sem pedir que eu o informe.
            Se não houver sorteio identificável, ofereça iniciar uma nova questão e aguarde.
            Se a minha escolha estiver ausente ou ambígua, pergunte apenas qual alternativa escolhi.
            Use a correção correspondente ou chame praticar_questao com operacao="corrigir",
            o ID do sorteio, a mesma tentativa retornada no sorteio e a posição da alternativa escolhida, começando em 1.
            Reutilize a tentativa para não registrar a mesma resposta duas vezes no desempenho.
            Se a ferramenta retornar um erro, informe o problema e aguarde antes de avaliar a resposta.
            Com a correção em mãos, diga se acertei. Percorra as alternativas na ordem apresentada:
            explique com suas palavras por que cada uma está certa ou errada, relacionando seu texto
            ao conceito que o enunciado cobra. Use o gabarito retornado como referência.
            """;
      var mensagem = PromptMessage.builder(Role.USER, TextContent.builder(instrucoes).build()).build();
      return GetPromptResult.builder(List.of(mensagem)).description(DESCRIPTION).build();
   }
}

package br.edu.ifpr.mcp_server.model;

/** Identifica uma tentativa para que a mesma resposta não seja somada novamente em um reenvio. */
public record RegistroResposta(String tentativa, long questao, int alternativa, boolean acertou) {
   /** Recusa registros incompletos, inclusive ao ler um arquivo editado manualmente. */
   public RegistroResposta {
      if (tentativa == null || tentativa.isBlank() || questao < 1 || alternativa < 1) {
         throw new IllegalArgumentException("Registro de resposta inválido.");
      }
   }
}

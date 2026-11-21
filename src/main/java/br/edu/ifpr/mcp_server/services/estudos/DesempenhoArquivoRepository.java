package br.edu.ifpr.mcp_server.services.estudos;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_server.model.Desempenho;
import br.edu.ifpr.mcp_server.model.Correcao;
import tools.jackson.databind.json.JsonMapper;

/** Salva um array JSON simples de respostas e calcula os totais ao ler esse arquivo. */
public final class DesempenhoArquivoRepository implements IRegistroDeResposta, IConsultaDeDesempenho {
   private static final JsonMapper JSON = new JsonMapper();
   private static final Object MONITOR = new Object();
   private final Path arquivo;

   /** Recebe o caminho do arquivo para permitir a configuração e testes em diretórios temporários. */
   public DesempenhoArquivoRepository(Path arquivo) {
      this.arquivo = arquivo.toAbsolutePath().normalize();
   }

   /** Acrescenta cada resposta ao histórico, inclusive respostas iguais à mesma questão. */
   @Override
   public void registrar(Correcao resposta) throws IOException {
      acessar(resposta);
   }

   /** Retorna zeros quando ainda não existe histórico e nunca inventa dados para um arquivo danificado. */
   @Override
   public Desempenho consultar() throws IOException {
      return acessar(null);
   }

   /** Serializa acessos de threads e processos (Claude e terminal) que usam o mesmo arquivo. */
   private Desempenho acessar(Correcao nova) throws IOException {
      synchronized (MONITOR) {
         Files.createDirectories(arquivo.getParent());
         Path trava = arquivo.resolveSibling(arquivo.getFileName() + ".lock");
         try (var canal = FileChannel.open(trava, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
              var bloqueio = canal.lock()) {
            var respostas = ler();
            if (nova != null) {
               respostas.add(nova);
               salvar(respostas);
            }
            long acertos = respostas.stream().filter(Correcao::isAcertou).count();
            long total = respostas.size();
            double percentual = total == 0 ? 0 : Math.round(10000.0 * acertos / total) / 100.0;
            return new Desempenho(total, acertos, total - acertos, percentual);
         }
      }
   }

   /** Lê correções com id, enunciado, alternativaEscolhida, gabarito e acertou. */
   private List<Correcao> ler() throws IOException {
      var respostas = new ArrayList<Correcao>();
      if (Files.notExists(arquivo)) return respostas;
      try {
         var dados = JSON.readTree(Files.readString(arquivo));
         if (dados == null || !dados.isArray()) throw new IOException("O histórico deve ser um array JSON.");
         for (var item : dados) {
            if (item.size() != 5 || !item.path("id").isIntegralNumber()
                  || !item.path("enunciado").isString() || !item.path("alternativaEscolhida").isIntegralNumber()
                  || !item.path("gabarito").isIntegralNumber() || !item.path("acertou").isBoolean()) {
               throw new IOException("Registro incompleto no arquivo de desempenho.");
            }
            var resposta = JSON.treeToValue(item, Correcao.class);
            if (resposta.getId() < 1 || resposta.getAlternativaEscolhida() < 1) {
               throw new IOException("Registro de resposta inválido.");
            }
            respostas.add(resposta);
         }
      } catch (RuntimeException ex) {
         throw new IOException("Não foi possível ler o arquivo de desempenho.", ex);
      }
      return respostas;
   }

   /** Substitui o JSON somente depois de escrever completamente o novo conteúdo em um temporário. */
   private void salvar(List<Correcao> respostas) throws IOException {
      Path temporario = Files.createTempFile(arquivo.getParent(), "desempenho-", ".tmp");
      try {
         Files.writeString(temporario, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(respostas));
         try {
            Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(temporario);
      }
   }
}

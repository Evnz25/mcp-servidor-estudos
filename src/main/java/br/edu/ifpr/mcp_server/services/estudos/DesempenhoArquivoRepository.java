package br.edu.ifpr.mcp_server.services.estudos;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

import br.edu.ifpr.mcp_server.model.Desempenho;
import br.edu.ifpr.mcp_server.model.RegistroResposta;
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

   /** Registra a resposta apenas se sua tentativa ainda não estiver no histórico. */
   @Override
   public void registrar(RegistroResposta resposta) throws IOException {
      acessar(resposta);
   }

   /** Retorna zeros quando ainda não existe histórico e nunca inventa dados para um arquivo danificado. */
   @Override
   public Desempenho consultar() throws IOException {
      return acessar(null);
   }

   /** Serializa acessos de threads e processos (Claude e terminal) que usam o mesmo arquivo. */
   private Desempenho acessar(RegistroResposta nova) throws IOException {
      synchronized (MONITOR) {
         Files.createDirectories(arquivo.getParent());
         Path trava = arquivo.resolveSibling(arquivo.getFileName() + ".lock");
         try (var canal = FileChannel.open(trava, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
              var bloqueio = canal.lock()) {
            var respostas = ler();
            if (nova != null) {
               var anterior = respostas.putIfAbsent(nova.getTentativa(), nova);
               if (anterior != null && !anterior.equals(nova)) {
                  throw new IllegalArgumentException("Esta tentativa já possui outra resposta registrada.");
               }
               if (anterior == null) salvar(respostas);
            }
            long acertos = respostas.values().stream().filter(RegistroResposta::isAcertou).count();
            long total = respostas.size();
            double percentual = total == 0 ? 0 : Math.round(10000.0 * acertos / total) / 100.0;
            return new Desempenho(total, acertos, total - acertos, percentual);
         }
      }
   }

   /** Lê o array de registros em UTF-8 e rejeita estruturas incompletas ou tentativas duplicadas. */
   private Map<String, RegistroResposta> ler() throws IOException {
      var respostas = new LinkedHashMap<String, RegistroResposta>();
      if (Files.notExists(arquivo)) return respostas;
      try {
         var dados = JSON.readTree(Files.readString(arquivo));
         if (dados == null || !dados.isArray()) throw new IOException("O histórico deve ser um array JSON.");
         for (var item : dados) {
            if (item.size() != 4 || !item.path("tentativa").isString() || !item.path("questao").isIntegralNumber()
                  || !item.path("alternativa").isIntegralNumber() || !item.path("acertou").isBoolean()) {
               throw new IOException("Registro incompleto no arquivo de desempenho.");
            }
            var resposta = JSON.treeToValue(item, RegistroResposta.class);
            if (respostas.putIfAbsent(resposta.getTentativa(), resposta) != null) {
               throw new IOException("Tentativa duplicada no arquivo de desempenho.");
            }
         }
      } catch (RuntimeException ex) {
         throw new IOException("Não foi possível ler o arquivo de desempenho.", ex);
      }
      return respostas;
   }

   /** Substitui o JSON somente depois de escrever completamente o novo conteúdo em um temporário. */
   private void salvar(Map<String, RegistroResposta> respostas) throws IOException {
      Path temporario = Files.createTempFile(arquivo.getParent(), "desempenho-", ".tmp");
      try {
         Files.writeString(temporario, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(respostas.values()));
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

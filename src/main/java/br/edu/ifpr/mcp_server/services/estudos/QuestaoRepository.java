package br.edu.ifpr.mcp_server.services.estudos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import br.edu.ifpr.mcp_server.resources.producers.JdbcConnectionProducer;
import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;
import br.edu.ifpr.mcp_server.model.QuestaoComGabarito;
import br.edu.ifpr.mcp_server.model.NovaQuestao;

public class QuestaoRepository implements IQuestaoRepository, IConsultaDeGabarito, ISorteioDeSimulado,
                                           ICadastroDeQuestao {
   private static final String CONSULTA = """
         SELECT id, enunciado, alternativas
         FROM questao
         ORDER BY random()
         LIMIT 1
         """;

   private final JdbcConnectionProducer connectionProducer;

   /** Recebe o produtor explicitamente, sem anotações de injeção de dependências. */
   public QuestaoRepository(JdbcConnectionProducer connectionProducer) {
      this.connectionProducer = Objects.requireNonNull(connectionProducer);
   }

   /** Fecha a consulta e o resultado, mantendo a conexão sob responsabilidade do produtor. */
   @Override
   public Optional<QuestaoParaEstudo> sortear() throws SQLException {
      synchronized (connectionProducer) {
         Connection conexao = connectionProducer.getJdbcConnection();
         try (PreparedStatement consulta = conexao.prepareStatement(CONSULTA);
              ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) {
               return Optional.empty();
            }
            return Optional.of(lerQuestao(resultado));
         }
      }
   }

   /** Lê a questão e seu gabarito na mesma consulta parametrizada. */
   @Override
   public Optional<QuestaoComGabarito> buscarPorId(long id) throws SQLException {
      String sql = "SELECT id, enunciado, alternativas, gabarito FROM questao WHERE id = ?";
      synchronized (connectionProducer) {
         Connection conexao = connectionProducer.getJdbcConnection();
         try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setLong(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
               if (!resultado.next()) {
                  return Optional.empty();
               }
               return Optional.of(new QuestaoComGabarito(lerQuestao(resultado), resultado.getInt("gabarito")));
            }
         }
      }
   }

   /** Sorteia linhas distintas em uma consulta, preservando o gabarito da rodada. */
   @Override
   public List<QuestaoComGabarito> sortearSequencia(int quantidade) throws SQLException {
      String sql = "SELECT id, enunciado, alternativas, gabarito FROM questao ORDER BY random() LIMIT ?";
      synchronized (connectionProducer) {
         Connection conexao = connectionProducer.getJdbcConnection();
         try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, quantidade);
            try (ResultSet resultado = consulta.executeQuery()) {
               var questoes = new ArrayList<QuestaoComGabarito>();
               while (resultado.next()) {
                  questoes.add(new QuestaoComGabarito(lerQuestao(resultado), resultado.getInt("gabarito")));
               }
               return questoes;
            }
         }
      }
   }

   /** Insere os valores parametrizados e obtém o ID na mesma instrução, sem fechar a conexão compartilhada. */
   @Override
   public long cadastrar(NovaQuestao questao) throws SQLException {
      String sql = "INSERT INTO questao (enunciado, alternativas, gabarito) VALUES (?, ?, ?) RETURNING id";
      synchronized (connectionProducer) {
         Connection conexao = connectionProducer.getJdbcConnection();
         var alternativas = conexao.createArrayOf("text", questao.alternativas().toArray(String[]::new));
         try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setString(1, questao.enunciado());
            consulta.setArray(2, alternativas);
            consulta.setInt(3, questao.gabarito());
            try (ResultSet resultado = consulta.executeQuery()) {
               if (!resultado.next()) throw new SQLException("O banco não retornou o ID da questão cadastrada.");
               return resultado.getLong("id");
            }
         } finally {
            alternativas.free();
         }
      }
   }

   /** Reconstrói os dados públicos sem alterar a ordem armazenada pelo PostgreSQL. */
   private QuestaoParaEstudo lerQuestao(ResultSet resultado) throws SQLException {
      var array = resultado.getArray("alternativas");
      try {
         String[] alternativas = (String[]) array.getArray();
         return new QuestaoParaEstudo(resultado.getLong("id"), resultado.getString("enunciado"),
                                      Arrays.asList(alternativas));
      } finally {
         array.free();
      }
   }
}

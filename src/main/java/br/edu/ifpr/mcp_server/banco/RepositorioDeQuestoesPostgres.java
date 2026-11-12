package br.edu.ifpr.mcp_server.banco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

import br.edu.ifpr.mcp_server.modelos.QuestaoParaEstudo;
import br.edu.ifpr.mcp_server.modelos.QuestaoComGabarito;

public class RepositorioDeQuestoesPostgres implements IRepositorioDeQuestoes, IConsultaDeGabarito {
   private static final String CONSULTA = """
         SELECT id, enunciado, alternativas
         FROM questao
         ORDER BY random()
         LIMIT 1
         """;

   private final String url;
   private final String usuario;
   private final String senha;

   /** Guarda a configuração para abrir uma conexão a cada consulta. */
   public RepositorioDeQuestoesPostgres(String url, String usuario, String senha) {
      this.url = url;
      this.usuario = usuario;
      this.senha = senha;
   }

   /** Busca apenas os campos públicos e fecha os recursos JDBC ao concluir a leitura. */
   @Override
   public Optional<QuestaoParaEstudo> Sortear() throws SQLException {
      try (Connection conexao = DriverManager.getConnection(url, usuario, senha);
           PreparedStatement consulta = conexao.prepareStatement(CONSULTA);
           ResultSet resultado = consulta.executeQuery()) {
         if (!resultado.next()) {
            return Optional.empty();
         }
         return Optional.of(LerQuestao(resultado));
      }
   }

   /** Lê a questão e seu gabarito na mesma consulta parametrizada. */
   @Override
   public Optional<QuestaoComGabarito> BuscarPorId(long id) throws SQLException {
      String sql = "SELECT id, enunciado, alternativas, gabarito FROM questao WHERE id = ?";
      try (Connection conexao = DriverManager.getConnection(url, usuario, senha);
           PreparedStatement consulta = conexao.prepareStatement(sql)) {
         consulta.setLong(1, id);
         try (ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) {
               return Optional.empty();
            }
            return Optional.of(new QuestaoComGabarito(LerQuestao(resultado), resultado.getInt("gabarito")));
         }
      }
   }

   /** Reconstrói os dados públicos sem alterar a ordem armazenada pelo PostgreSQL. */
   private QuestaoParaEstudo LerQuestao(ResultSet resultado) throws SQLException {
      String[] alternativas = (String[]) resultado.getArray("alternativas").getArray();
      return new QuestaoParaEstudo(resultado.getLong("id"), resultado.getString("enunciado"),
                                   Arrays.asList(alternativas));
   }
}

package br.edu.ifpr.mcp_server.banco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

import br.edu.ifpr.mcp_server.modelos.QuestaoParaEstudo;

public class RepositorioDeQuestoesPostgres implements IRepositorioDeQuestoes {
   private static final String CONSULTA = """
         SELECT id, enunciado, alternativas
         FROM questao
         ORDER BY random()
         LIMIT 1
         """;

   private final String url;
   private final String usuario;
   private final String senha;

   /** Guarda a configuração para abrir a conexão somente quando houver um sorteio. */
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
         String[] alternativas = (String[]) resultado.getArray("alternativas").getArray();
         return Optional.of(new QuestaoParaEstudo(resultado.getLong("id"),
               resultado.getString("enunciado"), Arrays.asList(alternativas)));
      }
   }
}

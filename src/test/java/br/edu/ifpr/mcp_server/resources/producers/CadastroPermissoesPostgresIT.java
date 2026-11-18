package br.edu.ifpr.mcp_server.resources.producers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import br.edu.ifpr.mcp_server.model.NovaQuestao;
import br.edu.ifpr.mcp_server.services.estudos.QuestaoRepository;

@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = ".+")
class CadastroPermissoesPostgresIT {
   /** Aplica o script real a um papel temporário de leitura e verifica a liberação apenas do cadastro. */
   @Test
   void habilitaCadastroEmUmUsuarioDeLeitura() throws Exception {
      String url = System.getenv("BANCO_TESTE_URL");
      String usuario = System.getenv("BANCO_USUARIO");
      String senha = System.getenv("BANCO_SENHA");
      String sufixo = UUID.randomUUID().toString().replace("-", "");
      String papel = "teste_papel_" + sufixo;
      String esquema = "teste_permissao_" + sufixo;
      try (var admin = DriverManager.getConnection(url, usuario, senha); var comando = admin.createStatement()) {
         try (var acesso = comando.executeQuery("SELECT rolsuper FROM pg_roles WHERE rolname = current_user")) {
            assertTrue(acesso.next());
            assumeTrue(acesso.getBoolean(1), "Este teste de permissões cria um papel temporário e exige superusuário.");
         }
         comando.execute("CREATE ROLE " + papel + " NOLOGIN");
         try {
            comando.execute("CREATE SCHEMA " + esquema);
            admin.setSchema(esquema);
            try (var arquivo = getClass().getResourceAsStream("/banco/questao.sql")) {
               comando.execute(new String(arquivo.readAllBytes(), UTF_8));
            }
            comando.execute("GRANT USAGE ON SCHEMA " + esquema + " TO " + papel);
            comando.execute("GRANT SELECT ON TABLE questao TO " + papel);
            String testUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + esquema;
            try (var producer = new JdbcConnectionProducer(testUrl, usuario, senha)) {
               var connection = producer.getJdbcConnection();
               try (var sessao = connection.createStatement()) {
                  sessao.execute("SET ROLE " + papel);
               }
               var repository = new QuestaoRepository(producer);
               var questao = new NovaQuestao("Qual padrão?", List.of("Circuit Breaker", "API Gateway"), 2);
               assertEquals("42501", assertThrows(SQLException.class, () -> repository.cadastrar(questao)).getSQLState());
               String sql = Files.readString(Path.of("docker", "habilitar-cadastro.sql"), UTF_8)
                     .replace("leitor_estudo", papel).replace("public.", esquema + ".");
               comando.execute(sql);
               comando.execute(sql); // Pode ser reaplicado em volumes existentes.
               long id = repository.cadastrar(questao);
               assertTrue(id > 0);
               assertEquals(2, repository.buscarPorId(id).orElseThrow().gabarito());
               assertSame(connection, producer.getJdbcConnection());
               try (var sessao = connection.createStatement()) {
                  assertEquals("42501", assertThrows(SQLException.class,
                        () -> sessao.executeUpdate("DELETE FROM questao")).getSQLState());
               }
            }
         } finally {
            comando.execute("DROP SCHEMA IF EXISTS " + esquema + " CASCADE");
            comando.execute("DROP ROLE " + papel);
         }
      }
   }
}

package br.edu.ifpr.mcp_server.resources.producers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import br.edu.ifpr.mcp_server.services.estudos.QuestaoRepository;

@EnabledIfEnvironmentVariable(named = "BANCO_TESTE_URL", matches = ".+")
class JdbcConnectionProducerIT {
   /** Verifica consultas, reconexão e descarte no PostgreSQL, usando somente um esquema temporário. */
   @Test
   void compartilhaConexaoEntreConsultasEReconecta() throws Exception {
      String url = System.getenv("BANCO_TESTE_URL");
      String user = System.getenv("BANCO_USUARIO");
      String password = System.getenv("BANCO_SENHA");
      String schema = "teste_jdbc_" + UUID.randomUUID().toString().replace("-", "");
      try (var admin = DriverManager.getConnection(url, user, password);
           var stmt = admin.createStatement()) {
         stmt.execute("CREATE SCHEMA " + schema);
         try {
            admin.setSchema(schema);
            try (var sql = getClass().getResourceAsStream("/banco/questao.sql")) {
               stmt.execute(new String(sql.readAllBytes(), UTF_8));
            }
            String testUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            try (var producer = new JdbcConnectionProducer(testUrl, user, password)) {
               var repository = new QuestaoRepository(producer);
               var connection = producer.getJdbcConnection();
               assertTrue(repository.sortear().isEmpty());
               assertTrue(repository.buscarPorId(1).isEmpty());
               assertTrue(repository.sortearSequencia(5).isEmpty());
               stmt.executeUpdate("""
                     INSERT INTO questao (enunciado, alternativas, gabarito)
                     VALUES ('Qual estrutura segue FIFO?', ARRAY['Pilha', 'Fila'], 2)
                     """);
               var question = repository.sortear().orElseThrow();
               assertEquals(List.of("Pilha", "Fila"), question.alternativas());
               assertEquals(2, repository.buscarPorId(question.id()).orElseThrow().gabarito());
               assertEquals(1, repository.sortearSequencia(5).size());
               assertSame(connection, producer.getJdbcConnection());
               assertFalse(connection.isClosed());

               connection.close();
               assertEquals(question, repository.sortear().orElseThrow());
               var replacement = producer.getJdbcConnection();
               assertNotSame(connection, replacement);
               stmt.execute("DROP TABLE questao");
               assertThrows(SQLException.class, repository::sortear);
               assertFalse(replacement.isClosed());
               producer.close();
               assertTrue(replacement.isClosed());
            }
         } finally {
            stmt.execute("DROP SCHEMA " + schema + " CASCADE");
         }
      }
   }
}

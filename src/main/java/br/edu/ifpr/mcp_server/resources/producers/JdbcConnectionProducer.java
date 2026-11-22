package br.edu.ifpr.mcp_server.resources.producers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JdbcConnectionProducer implements AutoCloseable {
   private static final Logger LOG = LoggerFactory.getLogger(JdbcConnectionProducer.class);
   private static final String DB_NAME = "estudo";
   private static final String DRIVER_JDBC = "org.postgresql.Driver";
   private static final String POSTGRESQL_URL_CONN = "jdbc:postgresql://";
   private static final String POSTGRESQL_SERVER_ADDR = "localhost:5432/";
   private static final String JDBC_URL_CONN = POSTGRESQL_URL_CONN + POSTGRESQL_SERVER_ADDR + DB_NAME;

   private final String url;
   private final String user;
   private final String password;
   private Connection conn;
   private boolean closed;

   //Lê a configuração uma vez; a conexão só será aberta na primeira consulta.
   private JdbcConnectionProducer() {
      this(System.getenv().getOrDefault("BANCO_URL", JDBC_URL_CONN),
            System.getenv("BANCO_USUARIO"), System.getenv("BANCO_SENHA"));
   }

   //Permite testes isolados no mesmo pacote, sem alterar o Singleton da aplicação.
   JdbcConnectionProducer(String url, String user, String password) {
      this.url = url;
      this.user = user;
      this.password = password;
   }

   // A JVM inicializa este holder apenas uma vez, mesmo com chamadas simultâneas
   private static final class InstanceHolder {
      private static final JdbcConnectionProducer INSTANCE = new JdbcConnectionProducer();
   }

   public static JdbcConnectionProducer getInstance() {
      return InstanceHolder.INSTANCE;
   }

   /**
    * Reutiliza a conexão válida e reabre uma conexão interrompida pelo banco.
    * O consumidor sincroniza no produtor durante toda a consulta, pois Connection
    * é compartilhada. O fechamento da aplicação usa o mesmo monitor.
    */
   public synchronized Connection getJdbcConnection() throws SQLException {
      if (closed) throw new SQLException("O produtor JDBC já foi encerrado.", "08003");
      if (conn != null && !conn.isClosed() && conn.isValid(2)) return conn;
      closeConnection();
      loadDriver();
      conn = DriverManager.getConnection(url, user, password);
      LOG.info("Conexão JDBC PostgreSQL estabelecida.");
      return conn;
   }

   // Carrega o driver explicitamente
   private void loadDriver() throws SQLException {
      try {
         Class.forName(DRIVER_JDBC);
      } catch (ClassNotFoundException ex) {
         throw new SQLException("Driver JDBC PostgreSQL não encontrado.", "08001", ex);
      }
   }

   // Fecha a conexão anterior antes de substituí-la ou encerrar o produtor.
   private void closeConnection() throws SQLException {
      if (conn != null) {
         conn.close();
         conn = null;
      }
   }

   @Override
   public synchronized void close() throws SQLException {
      closed = true;
      closeConnection();
   }
}

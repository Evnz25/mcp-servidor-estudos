package br.edu.ifpr.mcp_server.resources.producers;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** Usa um driver controlado para testar o ciclo de vida sem acessar um banco real. */
@EnabledIf("driverAvailable")
class JdbcConnectionProducerTest {
   private final TestDriver driver = new TestDriver();
   private JdbcConnectionProducer producer;

   /** O perfil client não distribui o driver PostgreSQL; o perfil server executa estes testes. */
   static boolean driverAvailable() {
      try {
         Class.forName("org.postgresql.Driver");
         return true;
      } catch (ClassNotFoundException ex) {
         return false;
      }
   }

   /** Registra um driver que aceita somente a URL reservada deste teste. */
   @BeforeEach
   void setUp() throws SQLException {
      DriverManager.registerDriver(driver);
      producer = new JdbcConnectionProducer("jdbc:producer-test:estudo", "aluno", "teste");
   }

   /** Libera a conexão e remove o driver para não interferir em outros testes. */
   @AfterEach
   void tearDown() throws SQLException {
      try {
         producer.close();
      } finally {
         DriverManager.deregisterDriver(driver);
      }
   }

   /** Confere que threads diferentes recebem o mesmo produtor da aplicação. */
   @Test
   void mantemUmaInstanciaDaAplicacao() throws Exception {
      try (var executor = Executors.newFixedThreadPool(4)) {
         var expected = JdbcConnectionProducer.getInstance();
         for (int i = 0; i < 20; i++) {
            assertSame(expected, executor.submit(JdbcConnectionProducer::getInstance).get());
         }
      }
   }

   /** Abertura é tardia, credenciais são repassadas e a conexão válida é reutilizada. */
   @Test
   void reutilizaConexaoEFechaUmaVez() throws SQLException {
      assertEquals(0, driver.opens);
      var connection = producer.getJdbcConnection();
      assertSame(connection, producer.getJdbcConnection());
      assertEquals(1, driver.opens);
      assertEquals("aluno", driver.properties.getProperty("user"));
      assertEquals("teste", driver.properties.getProperty("password"));
      producer.close();
      producer.close();
      assertEquals(1, driver.closes);
      assertEquals("08003", assertThrows(SQLException.class, producer::getJdbcConnection).getSQLState());
   }

   /** Substitui uma conexão que o banco passou a considerar inválida. */
   @Test
   void reconectaQuandoOBancoInterrompeAConexao() throws SQLException {
      var first = producer.getJdbcConnection();
      driver.valid = false;
      assertNotSame(first, producer.getJdbcConnection());
      assertEquals(2, driver.opens);
      assertEquals(1, driver.closes);
   }

   /** Também substitui uma conexão fechada externamente. */
   @Test
   void reconectaQuandoAConexaoFoiFechada() throws SQLException {
      var first = producer.getJdbcConnection();
      first.close();
      assertNotSame(first, producer.getJdbcConnection());
      assertEquals(2, driver.opens);
   }

   /** Falha de abertura é propagada e não impede uma tentativa posterior. */
   @Test
   void permiteTentarNovamenteAposFalha() throws SQLException {
      driver.failure = new SQLException("Banco indisponível", "08001");
      assertSame(driver.failure, assertThrows(SQLException.class, producer::getJdbcConnection));
      driver.failure = null;
      assertNotNull(producer.getJdbcConnection());
   }

   /** Fechar sem consultas não deve iniciar uma conexão apenas para descartá-la. */
   @Test
   void fechaSemAbrirConexao() throws SQLException {
      producer.close();
      assertEquals(0, driver.opens);
      assertThrows(SQLException.class, producer::getJdbcConnection);
   }

   /** Driver mínimo: os métodos fora do ciclo de vida não fazem parte destes testes. */
   private static final class TestDriver implements Driver {
      private int opens;
      private int closes;
      private boolean valid = true;
      private SQLException failure;
      private Properties properties;

      /** Cria uma conexão controlada ou reproduz a falha de abertura configurada. */
      @Override
      public Connection connect(String url, Properties info) throws SQLException {
         if (!acceptsURL(url)) return null;
         if (failure != null) throw failure;
         properties = info;
         opens++;
         valid = true;
         boolean[] closed = { false };
         return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
               new Class<?>[] { Connection.class }, (proxy, method, args) -> {
                  return switch (method.getName()) {
                     case "isClosed" -> closed[0];
                     case "isValid" -> valid && !closed[0];
                     case "close" -> {
                        if (!closed[0]) closes++;
                        closed[0] = true;
                        yield null;
                     }
                     default -> throw new UnsupportedOperationException(method.getName());
                  };
               });
      }

      /** Impede que o driver intercepte qualquer URL de desenvolvimento. */
      @Override
      public boolean acceptsURL(String url) { return url.startsWith("jdbc:producer-test:"); }

      /** Não oferece propriedades adicionais para a ferramenta de testes. */
      @Override
      public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }

      /** Identifica a versão maior do driver de teste. */
      @Override
      public int getMajorVersion() { return 1; }

      /** Identifica a versão menor do driver de teste. */
      @Override
      public int getMinorVersion() { return 0; }

      /** O driver simula apenas o contrato necessário aos testes. */
      @Override
      public boolean jdbcCompliant() { return false; }

      /** Fornece o logger exigido pelo contrato JDBC. */
      @Override
      public Logger getParentLogger() { return Logger.getGlobal(); }
   }
}

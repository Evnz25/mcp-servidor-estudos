package br.edu.ifpr.mcp_server.services.estudos;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Optional;

import br.edu.ifpr.mcp_server.model.Simulado;
import br.edu.ifpr.mcp_server.model.ResultadoSimulado;

/** Coordena as rodadas sem depender das classes do protocolo MCP. */
public final class SimuladoService {
   private static final int LIMITE_DE_RODADAS = 256;
   private final LinkedHashMap<String, Simulado> rodadas = new LinkedHashMap<>(16, 0.75f, true);
   private final ISorteioDeSimulado repositorio;

   /** Recebe somente a operação de persistência necessária para preparar o simulado. */
   public SimuladoService(ISorteioDeSimulado repositorio) {
      this.repositorio = repositorio;
   }

   /** Sorteia cinco questões e descarta a rodada menos recente quando atinge o limite. */
   public Simulado.Pergunta iniciar() throws SQLException {
      var simulado = new Simulado(repositorio.sortearSequencia(Simulado.TOTAL));
      var primeira = simulado.questaoAtual();
      synchronized (rodadas) {
         rodadas.put(primeira.rodada(), simulado);
         if (rodadas.size() > LIMITE_DE_RODADAS) rodadas.pollFirstEntry();
      }
      return primeira;
   }

   /** Localiza a rodada e delega ao modelo a correção e o controle de respostas repetidas. */
   public Object responder(String rodada, int numero, int alternativa) {
      Simulado simulado = buscarRodada(rodada);
      if (simulado == null) {
         throw new IllegalArgumentException("Rodada não encontrada. Inicie um novo simulado.");
      }
      return simulado.responder(numero, alternativa);
   }

   /** Consulta somente o placar da rodada em memória, inclusive enquanto está em andamento. */
   public Optional<ResultadoSimulado> consultarResultado(String rodada) {
      return Optional.ofNullable(buscarRodada(rodada)).map(Simulado::consultarResultado);
   }

   /** Protege o mapa compartilhado e marca a rodada como recentemente consultada. */
   private Simulado buscarRodada(String rodada) {
      synchronized (rodadas) {
         return rodadas.get(rodada);
      }
   }
}

package br.edu.ifpr.mcp_server.services;

import br.edu.ifpr.mcp_server.repositories.ISorteioDeSimulado;
import br.edu.ifpr.mcp_server.repositories.IRegistroDeResposta;

import java.sql.SQLException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

import br.edu.ifpr.mcp_server.model.Simulado;
import br.edu.ifpr.mcp_server.model.RodadaSimulado;
import br.edu.ifpr.mcp_server.model.ResultadoSimulado;

// Coordena as rodadas sem depender das classes do protocolo MCP
public final class SimuladoService {
   private static final int LIMITE_DE_RODADAS = 256;
   private final LinkedHashMap<String, Simulado> rodadas = new LinkedHashMap<>(16, 0.75f, true);
   private final ISorteioDeSimulado repositorio;
   private final IRegistroDeResposta historico;

   //Recebe somente a operação de persistência necessária para preparar o simulado
   public SimuladoService(ISorteioDeSimulado repositorio) {
      this(repositorio, resposta -> {});
   }

   //Compartilha o registrador de desempenho com a ferramenta de questões avulsas
   public SimuladoService(ISorteioDeSimulado repositorio, IRegistroDeResposta historico) {
      this.repositorio = repositorio;
      this.historico = historico;
   }

   // Sorteia cinco questões e descarta a rodada menos recente quando atinge o limite
   public RodadaSimulado iniciar() throws SQLException {
      var questoes = repositorio.sortearSequencia(Simulado.TOTAL);
      // Uma consulta pode retornar menos questões quando o banco ainda está sendo preenchido.
      if (questoes.size() < Simulado.TOTAL) {
         throw new IllegalArgumentException("Cadastre pelo menos " + Simulado.TOTAL + " questões para iniciar um simulado.");
      }
      var simulado = new Simulado(questoes);
      var primeira = simulado.questaoAtual();
      synchronized (rodadas) {
         rodadas.put(primeira.getRodada(), simulado);
         if (rodadas.size() > LIMITE_DE_RODADAS) rodadas.pollFirstEntry();
      }
      return primeira;
   }

   //Localiza a rodada, salva a resposta e delega ao modelo a correção e o avanço
   public Object responder(String rodada, int numero, int alternativa) throws IOException {
      Simulado simulado = buscarRodada(rodada);
      if (simulado == null) {
         throw new IllegalArgumentException("Rodada não encontrada. Inicie um novo simulado.");
      }
      synchronized (simulado) {
         historico.registrar(simulado.corrigirResposta(numero, alternativa));
         return simulado.responder(numero, alternativa);
      }
   }

   //Consulta o placar da rodada
   public Optional<ResultadoSimulado> consultarResultado(String rodada) {
      return Optional.ofNullable(buscarRodada(rodada)).map(Simulado::consultarResultado);
   }

   //Localiza um simulado pelo identificador da rodada
   private Simulado buscarRodada(String rodada) {
      synchronized (rodadas) {
         return rodadas.get(rodada);
      }
   }
}

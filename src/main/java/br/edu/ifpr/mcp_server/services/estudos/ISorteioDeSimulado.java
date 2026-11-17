package br.edu.ifpr.mcp_server.services.estudos;

import java.sql.SQLException;
import java.util.List;

import br.edu.ifpr.mcp_server.model.QuestaoComGabarito;

public interface ISorteioDeSimulado {
   /** Devolve até a quantidade solicitada de questões distintas com seus gabaritos. */
   List<QuestaoComGabarito> sortearSequencia(int quantidade) throws SQLException;
}

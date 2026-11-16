package br.edu.ifpr.mcp_server.banco;

import java.sql.SQLException;
import java.util.List;

import br.edu.ifpr.mcp_server.modelos.QuestaoComGabarito;

public interface ISorteioDeSimulado {
   /** Devolve até a quantidade solicitada de questões distintas com seus gabaritos. */
   List<QuestaoComGabarito> SortearSequencia(int quantidade) throws SQLException;
}

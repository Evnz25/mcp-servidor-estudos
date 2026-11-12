package br.edu.ifpr.mcp_server.banco;

import java.sql.SQLException;
import java.util.Optional;

import br.edu.ifpr.mcp_server.modelos.QuestaoComGabarito;

public interface IConsultaDeGabarito {
   /** Busca exatamente a questão respondida ou devolve vazio quando o ID não existe. */
   Optional<QuestaoComGabarito> BuscarPorId(long id) throws SQLException;
}

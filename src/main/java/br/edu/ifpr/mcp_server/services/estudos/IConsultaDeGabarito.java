package br.edu.ifpr.mcp_server.services.estudos;

import java.sql.SQLException;
import java.util.Optional;

import br.edu.ifpr.mcp_server.model.QuestaoComGabarito;

public interface IConsultaDeGabarito {
   /** Busca exatamente a questão respondida ou devolve vazio quando o ID não existe. */
   Optional<QuestaoComGabarito> buscarPorId(long id) throws SQLException;
}

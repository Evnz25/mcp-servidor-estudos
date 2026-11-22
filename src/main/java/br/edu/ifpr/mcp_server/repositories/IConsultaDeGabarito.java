package br.edu.ifpr.mcp_server.repositories;

import java.sql.SQLException;
import java.util.Optional;

import br.edu.ifpr.mcp_server.model.Questao;

public interface IConsultaDeGabarito {
   //Busca exatamente a questão respondida ou devolve vazio quando o ID não existe
   Optional<Questao> buscarPorId(long id) throws SQLException;
}

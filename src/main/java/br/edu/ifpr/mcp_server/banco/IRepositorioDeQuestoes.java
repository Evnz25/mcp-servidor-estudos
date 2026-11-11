package br.edu.ifpr.mcp_server.banco;

import java.sql.SQLException;
import java.util.Optional;

import br.edu.ifpr.mcp_server.modelos.QuestaoParaEstudo;

public interface IRepositorioDeQuestoes {
   /** Sorteia uma questão ou devolve um resultado vazio quando não há questões. */
   Optional<QuestaoParaEstudo> Sortear() throws SQLException;
}

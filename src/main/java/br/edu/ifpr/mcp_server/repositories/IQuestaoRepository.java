package br.edu.ifpr.mcp_server.repositories;

import java.sql.SQLException;
import java.util.Optional;

import br.edu.ifpr.mcp_server.model.QuestaoParaEstudo;

public interface IQuestaoRepository {
   //Sorteia uma questão ou devolve um resultado vazio quando não há questões
   Optional<QuestaoParaEstudo> sortear() throws SQLException;
}

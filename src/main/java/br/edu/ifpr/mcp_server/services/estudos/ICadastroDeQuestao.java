package br.edu.ifpr.mcp_server.services.estudos;

import java.sql.SQLException;

import br.edu.ifpr.mcp_server.model.NovaQuestao;

/** Contrato de escrita separado das interfaces usadas apenas para consultas. */
public interface ICadastroDeQuestao {
   /** Persiste uma questão validada e devolve o identificador atribuído pelo banco. */
   long cadastrar(NovaQuestao questao) throws SQLException;
}

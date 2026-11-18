package br.edu.ifpr.mcp_server.services.estudos;

import java.io.IOException;
import br.edu.ifpr.mcp_server.model.Desempenho;

public interface IConsultaDeDesempenho {
   /** Lê o desempenho acumulado sem registrar outra resposta. */
   Desempenho consultar() throws IOException;
}

package br.edu.ifpr.mcp_server.services.estudos;

import java.io.IOException;
import br.edu.ifpr.mcp_server.model.RegistroResposta;

public interface IRegistroDeResposta {
   /** Guarda cada resposta recebida, inclusive repetições da mesma questão. */
   void registrar(RegistroResposta resposta) throws IOException;
}

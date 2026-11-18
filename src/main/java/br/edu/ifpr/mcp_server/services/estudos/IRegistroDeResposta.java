package br.edu.ifpr.mcp_server.services.estudos;

import java.io.IOException;
import br.edu.ifpr.mcp_server.model.RegistroResposta;

public interface IRegistroDeResposta {
   /** Guarda a resposta; o mesmo identificador de tentativa não pode produzir duas contagens. */
   void registrar(RegistroResposta resposta) throws IOException;
}

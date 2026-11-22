package br.edu.ifpr.mcp_server.repositories;

import java.io.IOException;
import br.edu.ifpr.mcp_server.model.Correcao;

public interface IRegistroDeResposta {
   //Guarda cada resposta recebida, inclusive repetições da mesma questão
   void registrar(Correcao resposta) throws IOException;
}

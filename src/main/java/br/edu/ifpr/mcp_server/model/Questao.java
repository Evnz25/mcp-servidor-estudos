package br.edu.ifpr.mcp_server.model;

import java.util.List;

//Interface da questao que sera utilizado para o Decorator
public interface Questao {
   long getId();
   String getEnunciado();
   List<String> getAlternativas();
}

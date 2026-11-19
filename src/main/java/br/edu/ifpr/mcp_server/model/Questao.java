package br.edu.ifpr.mcp_server.model;

import java.util.List;

/** Component do Decorator: operações comuns a qualquer questão. */
public interface Questao {
   long getId();
   String getEnunciado();
   List<String> getAlternativas();
}

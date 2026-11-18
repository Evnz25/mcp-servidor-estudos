package br.edu.ifpr.mcp_server.model;

import java.util.List;

/** Dados públicos do sorteio e identificador que o cliente reutiliza ao enviar a resposta. */
public record QuestaoSorteada(long id, String enunciado, List<String> alternativas, String tentativa) {}

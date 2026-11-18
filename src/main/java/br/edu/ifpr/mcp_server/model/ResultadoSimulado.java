package br.edu.ifpr.mcp_server.model;

/** Fotografia imutável do placar; questões ainda não respondidas não contam como erros. */
public record ResultadoSimulado(String rodada, boolean finalizado, int total,
                                int respondidas, int acertos, int erros) {}

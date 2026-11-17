package br.edu.ifpr.mcp_server.model;

import java.util.List;

/** Dados que o aluno pode receber antes de responder à questão. */
public record QuestaoParaEstudo(long id, String enunciado, List<String> alternativas) {
   /** Preserva a ordem cadastrada, inclusive para a correção posterior. */
   public QuestaoParaEstudo {
      alternativas = List.copyOf(alternativas);
   }
}

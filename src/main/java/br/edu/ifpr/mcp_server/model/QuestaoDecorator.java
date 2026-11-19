package br.edu.ifpr.mcp_server.model;

import java.util.List;
import java.util.Objects;

/** Decorator base: envolve um componente ou outro decorador e delega seus dados. */
public abstract class QuestaoDecorator implements Questao {
   private final Questao questao;

   protected QuestaoDecorator(Questao questao) {
      this.questao = questao;
   }

   @Override
   public long getId() { return questao.getId(); }

   @Override
   public String getEnunciado() { return questao.getEnunciado(); }

   @Override
   public List<String> getAlternativas() { return questao.getAlternativas(); }

   /** Projeta somente os dados públicos, inclusive quando há decoradores encadeados. */
   public QuestaoParaEstudo paraEstudo() {
      return new QuestaoParaEstudo(getId(), getEnunciado(), List.copyOf(getAlternativas()));
   }
}

# Contexto do projeto: mcp-para-estudo
## 1. Visão Geral: O projeto é um trabalho do meu curso em que é necessário desenvolver um servidor mcp, esse servidor tem como principal função auxiliar pessoas para o estudo de exercicios, em qual o usuário pode solicitar para realizar exercicios e o agente que estiver conectado com servidor podera auxiliar na correção, explicando por exemplo porque as outras alternativas estão erradas.

## 2. Stack Tecnológica e Restrições Absolutas
- **Linguagem:** Java puro (Vanilla Java).
- **Banco de Dados:** PostgreSQL.
- **Gerenciador de Dependências e Build:** Apache Maven.
- **IDE Alvo da correção:** Eclipse IDE (o código deve ser compatível para importação direta).
- **PROIBIDO (NÃO UTILIZE SOB NENHUMA HIPÓTESE):**
    - Frameworks que geram código mágico ou injeção pesada: **Spring** (ou qualquer módulo do ecossistema Spring).
    - Bibliotecas de geração de boilerplate: **Lombok**. Você (IA) DEVE escrever os getters, setters, construtores e builders manualmente.

## 3. Arquitetura e Padrões de Projeto (Core)
O código será avaliado rigorosamente por sua qualidade arquitetural. Ao gerar qualquer código, aplique:
- **Princípios S.O.L.I.D.** rigorosamente.
- **Padrões de Projeto (Design Patterns)** clássicos do GoF onde fizer sentido (ex: Factory, Strategy, Command).
- **Alta Coesão e Baixo Acoplamento.**
- **DRY** (Don't Repeat Yourself), **KISS** (Keep It Simple, Stupid) e **YAGNI** (You Aren't Gonna Need It).

## 4. Convenções de Código e Modularização
- **Responsabilidade Única (SRP):** Classes devem ter apenas um motivo para mudar. Métodos devem executar apenas uma tarefa muito simples. Se um método estiver grande, divida-o em métodos privados menores.
- **Pacotes:** Separe as responsabilidades em pacotes claros (ex: `servidor`, `cliente`, `ferramentas`, `modelos`, `nucleo`).
- **Idioma:** Use **português** nos nomes definidos pelo projeto (pacotes, classes, interfaces, métodos, variáveis, tabelas e colunas), nos comentários e nas mensagens ao usuário. Use os nomes oficiais exigidos pelas bibliotecas e pelo protocolo MCP.
- **Nomenclatura:**
  - Interfaces começam com `I` (ex: `IRepositorioDeQuestoes`).
  - Classes e Métodos usam `PascalCase`.
  - Variáveis locais usam `camelCase`.
- **Comentários:** Faça comentários em todos os métodos do sistemas, pois é muito importante para o aprendizado, quanto mais comentário e mais descritivo for para o entendimento do projeto, melhor.

## 5. Especificações de Negócio (O Protocolo MCP)
O projeto contém duas frentes:
1. **MCP Server:**
   - Deve disponibilizar um conjunto variado de *Tools* (Ferramentas).
   - Deve implementar *Resources* e *Prompts* (Requisito de alto valor agregado).
   - Deve ser totalmente compatível para testes no: **Claude Desktop** e **MCP Inspector**.
2. **MCP Client:**
   - Deve ser capaz de se conectar ao servidor desenvolvido e conseguir executar *toda e qualquer ferramenta* disponibilizada por ele.

## 6. Testes Unitários e de Integração
- **Testes Unitários:** Devem cobrir *todas* as possibilidades de retornos das funções/métodos implementados.
- **Testes de Integração:** Devem testar a comunicação real com o Servidor MCP, executando suas ferramentas e validando as respostas.

## 7. Build e Execução (Maven)
O `pom.xml` deve ser configurado corretamente para:
- Gerar os diferentes artefatos de software separadamente (o executável do Cliente e o executável do Servidor).
- Rodar os diferentes tipos de testes através das fases do Maven.

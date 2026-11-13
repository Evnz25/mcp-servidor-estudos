<!--
Markdown Cheatsheet
https://github.com/adam-p/markdown-here/wiki/markdown-cheatsheet
-->

## 🚀 MCP Server: Exemplo 🚀

Este projeto demonstra o uso do Protocolo de Contexto do Modelo ([MCP](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro) - Model Context Protocol) usando o [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk).

Com este SDK deve-ser programar todos os detalhes, tanto do servidor quanto do cliente.

A camada de transporte administra a comunicação entre cliente e servidor, entre outras tarefas, podendo ser STDIO ou *Streamable* HTTP.

Neste caso, é demonstrado o uso de STDIO como modelo de comunicação entre o cliente e o servidor.

O MCP define três primitivas básicas: *tools* (ferramentas), *resources* (recursos) e *prompts*.

O projeto disponibiliza ferramentas e um prompt de revisão.

### Testar com Docker (para o professor)

Com o Docker Desktop ou o Docker Engine com Compose em execução, abra um terminal na pasta que contém `compose.yaml`. A primeira execução precisa de internet para baixar as imagens e dependências.

```sh
docker compose up --build -d
docker compose logs mcp-inspector
```

1. Abra no navegador a URL local com o token que o Inspector imprime no log (porta `6274`). Se a URL ainda não apareceu, repita o segundo comando.
2. Selecione a conexão Java já configurada e conecte. Em **Tools**, execute `praticar_questao` com `{"operacao":"sortear"}`. No primeiro uso, você verá a questão sobre FIFO.
3. Na mesma ferramenta, execute `{"operacao":"corrigir","id":1,"alternativa":2}`, usando o ID devolvido no sorteio. Para a questão sobre FIFO, a resposta “Fila” devolve `"acertou": true`.

O Docker compila o servidor Java e inicia o PostgreSQL. No primeiro uso do volume, o banco `estudo` recebe a tabela `questao`, a questão de exemplo e o usuário `leitor_estudo`, com acesso de leitura. O servidor recebe a senha automaticamente e acessa o banco pela rede interna do Compose. O link com token autentica o navegador no Inspector.

Para encerrar, execute `docker compose down`. As questões ficam em um volume local e estarão disponíveis no próximo `docker compose up -d`. Cada computador começa com sua própria cópia da questão de exemplo.

As senhas padrão do Compose são para a demonstração local. Para escolher outras, defina `BANCO_SENHA` (leitor) e `POSTGRES_PASSWORD` (administrador) em um arquivo `.env` ao lado de `compose.yaml` antes do primeiro uso. Em um banco já inicializado, trocar uma senha exige alterá-la também no PostgreSQL.

### Conectar o Claude Desktop ao projeto com Docker

1. No computador que vai executar o Claude Desktop, abra o terminal na pasta do projeto e execute `docker compose up --build -d`. Mantenha os serviços em execução enquanto usar o Claude. O navegador pode ficar fechado.
2. No Claude Desktop, abra **Configurações → Desenvolvedor → Editar configuração**. Em `claude_desktop_config.json`, adicione a entrada `estudos` a `mcpServers`. Se o arquivo estiver vazio, use o exemplo completo abaixo.
3. Na pasta do projeto, obtenha o **caminho absoluto de `compose.yaml` neste computador**. No Linux, execute `realpath compose.yaml`. No macOS, execute `echo "$PWD/compose.yaml"`. No PowerShell, execute `(Resolve-Path compose.yaml).Path.Replace('\','/')`. Copie o resultado para o argumento logo após `-f` no JSON abaixo.

```json
{
  "mcpServers": {
    "estudos": {
      "command": "docker",
      "args": [
        "compose", "-f", "COLE_AQUI_O_CAMINHO_COMPLETO_DE_COMPOSE_YAML",
        "exec", "-T", "mcp-inspector",
        "java", "-jar", "/opt/mcp/server/mcp_server01_server.jar"
      ]
    }
  }
}
```

4. Salve o arquivo, encerre totalmente o Claude Desktop e abra-o novamente. Em uma nova conversa, peça **“Use a ferramenta praticar_questao para sortear uma questão e aguarde minha resposta.”** Para a questão sobre FIFO, responda **“Fila”** e peça a correção.

O Claude inicia seu próprio processo Java dentro do contêiner e se comunica com ele por STDIO. Esse processo recebe a credencial de leitura do Compose e consulta o mesmo PostgreSQL do Inspector. Se o Claude informar que não encontrou o executável `docker`, use em `command` o caminho completo retornado por `where.exe docker` (Windows) ou `which docker` (macOS).

### Banco de questões em uma instalação manual

Com o PostgreSQL em execução, use o terminal na raiz do projeto. No exemplo, o usuário local é `postgres`. Execute o primeiro comando uma vez para criar o banco e o segundo para criar a tabela.

```powershell
createdb -h localhost -U postgres estudo
psql -h localhost -U postgres -d estudo -v ON_ERROR_STOP=1 -f src/main/resources/banco/questao.sql
```

No Windows, esses executáveis ficam na pasta `bin` da instalação do PostgreSQL. Pelo pgAdmin, também é possível criar o banco `estudo` e executar o arquivo `questao.sql` no Query Tool desse banco.

Para cadastrar uma questão, execute o SQL abaixo no banco `estudo`. A posição da primeira alternativa é 1. Neste exemplo, o gabarito é 2, que corresponde a “Fila”.

```sql
INSERT INTO questao (enunciado, alternativas, gabarito)
VALUES ('Qual estrutura de dados segue a ordem FIFO?',
        ARRAY['Pilha', 'Fila', 'Árvore binária'], 2);
```

### Sortear uma questão

O perfil Maven `server` inclui o driver JDBC. Com o banco preparado, gere o servidor com `mvn -Pserver package`. Configure as variáveis de ambiente para o processo que vai executar o JAR.

| Variável | Valor |
| --- | --- |
| `BANCO_URL` | A URL padrão é `jdbc:postgresql://localhost:5432/estudo`. Ajuste o host, a porta ou o nome do banco conforme a instalação. |
| `BANCO_USUARIO` | Usuário do PostgreSQL com permissão de leitura na tabela `questao`. |
| `BANCO_SENHA` | Senha desse usuário. |

No Eclipse, ative o perfil Maven `server` e defina as variáveis na aba **Environment** da configuração de execução. No Claude Desktop, use o campo `env` da entrada do servidor. No MCP Inspector, informe as variáveis do processo STDIO na configuração da conexão. Se o Inspector estiver no Docker Desktop e o PostgreSQL no Windows, use `host.docker.internal` no lugar de `localhost` na URL.

Escolha `praticar_questao` no cliente Java e responda com o número da alternativa. Em um assistente, peça uma questão e responda naturalmente. Para reproduzir a primeira chamada no Inspector, use `{"operacao":"sortear"}`. Com a questão do exemplo cadastrada, uma resposta possível é:

```json
{"id":1,"enunciado":"Qual estrutura de dados segue a ordem FIFO?","alternativas":["Pilha","Fila","Árvore binária"]}
```

O servidor abre a conexão durante cada sorteio. A consulta lê somente os três campos da resposta e mantém a ordem das alternativas. Uma tabela vazia produz a mensagem “Nenhuma questão cadastrada.”.

### Corrigir a resposta do aluno

Depois da resposta, o cliente Java ou o agente reutiliza o ID do sorteio e chama `praticar_questao` com a operação `corrigir`. O aluno informa somente a alternativa. A contagem começa em 1. Por exemplo, se o aluno escolheu “Pilha” na questão anterior, os argumentos são:

```json
{"operacao": "corrigir", "id": 1, "alternativa": 1}
```

O servidor consulta o gabarito cadastrado para esse ID e devolve:

```json
{
  "id": 1,
  "enunciado": "Qual estrutura de dados segue a ordem FIFO?",
  "alternativaEscolhida": 1,
  "gabarito": 2,
  "acertou": false,
  "alternativas": [
    {"posicao": 1, "texto": "Pilha", "correta": false},
    {"posicao": 2, "texto": "Fila", "correta": true},
    {"posicao": 3, "texto": "Árvore binária", "correta": false}
  ]
}
```

### Pedir à IA que explique a correção

Após responder na conversa, selecione o prompt MCP `explicar_correcao` em um assistente que ofereça prompts. Ele usa o ID e a alternativa escolhida na conversa. Se esses dados já estiverem disponíveis, o assistente usa a correção correspondente ou usa a operação `corrigir` de `praticar_questao` para obtê-la. A IA escreve a justificativa de cada alternativa a partir do enunciado, dos textos e do gabarito. Basta cadastrar os dados da questão.

No MCP Inspector, use a aba **Prompts** para listar e obter o texto de `explicar_correcao`. A geração da explicação acontece no assistente com IA.

Para executar os testes, use `mvn -Pserver verify`. Para incluir o teste com PostgreSQL real, defina também `BANCO_TESTE_URL` com a URL JDBC de um banco de testes e configure `BANCO_USUARIO` e `BANCO_SENHA` para ele. O teste cria e remove um esquema próprio nesse banco, por isso o usuário de teste precisa de permissão para criar esquemas.

### 📌 Executar a aplicação 📌
Este é um projeto único para duas aplicações: servidor e cliente. O servidor é executado a partir da classe `br.edu.ifpr.mcp_server.main.McpServer01SyncStdio`. Já o cliente é executado a partir da classe `br.edu.ifpr.mcp_server.main.McpClient01SyncStdio`.

Inicialmente, é preciso gerar a aplicação servidor e, em seguida, executar o cliente, tanto no Eclispe IDE quanto em linha de comando (CLI).

Além disto, é possível executar o servidor MCP Java em assistentes de IA locais, tais como Claude Desktop, Gemini CLI e Cursor CLI, entre outros.

Também é possível verificar e testar o servidor MCP Java com o MCP Inspector. A Anthropic criou esta ferramenta para testar e depurar servidores MCP locais e remotos. Ela possui interface web, CLI e TUI. A seção **Testar com Docker (para o professor)** descreve a execução com o banco incluído.

#### ⚙️ Eclipse IDE ⚙️
1. Clicar com o botão direito do mouse no nome do projeto;
2. Selecionar **Run As** > **Maven build...**
3. Na janela **Edit configuration and launch**:
    * Na aba **Main** digitar **clean package -Pserver -DskipTests** no campo **Goals**
    * Na aba **JRE** digitar **--enable-native-access=ALL-UNNAMED** no campo **VM arguments**
4. Clicar no botão **Run**

Os passos anteriores devem ser executados somente na primeira vez. A partir da segunda vez clicar na "pontinha de flecha" ao lado do botão *play*, na barra de botões do Eclipse IDE.

Se o processo de construção da aplicação servidor ocorreu sem erros, ele produzirá o diretório (pasta) **server** na raiz do projeto. Este diretório contém a aplicação executável do servidor e o sub-diretório **libs** contendo as bibliotecas de código necessárias ao servidor.

**A aplicação servidor é executada pela aplicação cliente e não precisa de intervenção do programador.**

Para gerar a aplicação cliente, deve-se executar os quatro passos mostrados anteriormente, com a seguinte diferença, na aba **Main** digitar **clean package -Pclient -DskipTests**, e este procedimento, assim como o anterior, deve ser realizado apenas na primeira vez.

Se o processo de construção da aplicação cliente ocorreu sem erros, ele produzirá o diretório **dist-client** como sub-diretório de **target**. O diretório **dist_client** contém a aplicação cliente e o sub-diretório **libs** com as bibliotecas necessária à sua execução.

#### ⚙️ Linha de Comand (CLI) ⚙️
Abrir uma janela do terminal, Prompt de Comando no Windows, por exemplo, mudar para o diretório raiz do projeto e gerar as aplicações:

```
# gerar a aplicação servidor
mvn clean package -Pserver -DskipTests
```
```
# gerar a aplicação cliente
mvn clean package -Pclient -DskipTests

# executar o cliente MCP Java, que executará o servidor MCP Java
cd target/dist-client
java -jar mcp_server01_client.jar
```

Após gerar as duas aplicações, execute o cliente a partir de `target/dist-client` para que ele encontre o JAR do servidor. O menu permite escolher `praticar_questao` e responder à questão sorteada.

---

### 📌 Instalar o servidor MCP Java sem Docker no [Claude Desktop](https://claude.com/) 📌 ###
- Localizar o diretório `C:\Users\<USUÁRIO_LOGADO>\AppData\Local\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Roaming\Claude`
- Editar o arquivo `claude_desktop_config.json`
- Se não existir, criar a entrada `mcpServers` com o conteúdo

```
  "mcpServers": {
    "NOME_DO_SERVIDOR": {
      "command": "java",
      "args": ["-jar", "/caminho/absoluto/para/servidor.jar"],
      "env": {}
    }
  }
```
- Salvar o arquivo e iniciar ou reiniciar o Claude Desktop (se o Claude Desktop estiver em execução deve ser completamente encerrado e não apenas sua janela fechada).


O diretório `Users` também pode ser escrito como `Usuários`.
 
O diretório `Claude_pzs8sxrjxfjjc` pode possuir um nome diferente do apresentado, mas o prefixo `Claude_` será o mesmo.

O Java, JDK ou JRE, **deve** estar instalado e configurado.

---

🗣️ **OBSERVAÇÃO IMPORTANTE** 🗣️
> Sempre que o artefato SERVIDOR é gerado, o artefato CLIENTE é apagado do diretório "target", devido ao objetivo (*goal*) `clean`, e deve ser gerado novamente (mesmo que não tenha havido mudança de código nele).

---

### 📌 Executar Testes de Integração (IT - *Integration Test*) (OPCIONAL) 📌
O diretório `src/test/java` é o local onde se localizam as classes para testes, tanto de integração, classes com o sufixo **IT**, quanto unitários, classes com o sufixo **Test**.

Seguir os passos acima para criar um *build*, se for a primeira execução. No campo `Goals` digitar `clean verify -Pserver`.

Para executar os testes de integração em linha de comando, digitar `mvn clean verify -Pserver`.

---

#### 📘 REFERÊNCIAS 📘 ####
[What is the Model Context Protocol (MCP)? - Documentação Oficial](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro)

[MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)

[Building a JDBC-based MCP Server: A Developer Guide](https://www.cdata.com/kb/articles/jdbc-mcp-server-dev-guide.rst)

[Learn MCP Java Tutorial - Microsoft](https://github.com/microsoft/lets-learn-mcp-java/tree/main)

[Tutorial 1 no GitHub](https://github.com/eugenp/tutorials/tree/master/java-mcp)

[Tutorial 2 no GitHub](https://github.com/CDataSoftware/cdata-jdbc-mcp-server)

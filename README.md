## MCP Server para estudos

Este projeto tem como objetivo utilizar o formato de comunicação MCP para que alunos possam estudar por meio de questões relacionadas a área de desenvolvimento de software.

### Tools

| Nome                | Descrição                                                                                                                                             |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `praticar_questao`  | Sorteia uma questão do banco ou corrige uma resposta, informando o gabarito e registrando o desempenho. Possui as operações `sortear` e `corrigir`.   |
| `praticar_simulado` | Inicia um simulado de cinco questões e recebe as respostas, permitindo acompanhar o resultado da rodada. Possui as operações `iniciar` e `responder`. |
| `cadastrar_questao` | Salva uma nova questão no PostgreSQL, recebendo enunciado, alternativas e gabarito. Retorna o ID gerado e os dados cadastrados.                       |

### Prompts

| Nome                | Descrição                                                                                                                                                         |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `explicar_correcao` | Orienta a IA a usar a questão e a resposta da conversa para explicar a correção, detalhando por que cada alternativa está certa ou errada. Não recebe argumentos. |

### Resources

| Nome e URI                                                      | Descrição                                                                                                                                                                                                              |
| --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `desempenho` — `estudos://desempenho`                           | Retorna em JSON o total de respostas, acertos, erros e percentual de acertos do usuário local, persistidos em arquivo.                                                                                                 |

## Executar em um computador novo (Windows)

Este roteiro considera um computador Windows sem Java, Maven ou Docker instalados. O Docker Desktop fornece o ambiente necessário: Java, Maven, Node.js, MCP Inspector e PostgreSQL são preparados dentro dos contêineres. Não é necessário instalar essas ferramentas separadamente no Windows.

A primeira execução exige internet para baixar as imagens e as dependências do projeto.

### 1. Preparar o WSL 2

Abra o PowerShell **como administrador** e execute:

```powershell
wsl --install
```

Reinicie o computador se solicitado e conclua a configuração apresentada. Depois, atualize o WSL:

```powershell
wsl --update
```

### 2. Instalar o Docker Desktop

Baixe e instale o [Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/).

Utilize **WSL 2** e os contêineres Linux. Abra o Docker Desktop e aguarde o mecanismo do Docker ficar em execução.

### 3. Copiar o projeto

Transfira a pasta completa do projeto para o computador ou baixe o repositório como ZIP e extraia.

```powershell
git clone https://github.com/Evnz25/mcp-servidor-estudos.git
```

### 4. Compilar e iniciar o projeto

Abra o PowerShell na pasta que contém `compose.yaml`. Por exemplo, se o projeto estiver em `C:\Projetos`:

```powershell
cd C:\Projetos\mcp-servidor-estudos
docker compose up --build -d
```

Substitua o caminho pelo local em que você salvou o projeto. A primeira execução pode levar alguns minutos.

Esse comando compila o servidor com Java 25 e Maven, inicia o PostgreSQL e disponibiliza o MCP Inspector. Em um volume novo, o banco `estudo` é inicializado com oito questões.

Confira o estado dos serviços:

```powershell
docker compose ps
```

O PostgreSQL deve aparecer saudável (`healthy`) e o serviço `mcp-inspector` deve estar em execução (`Up`).

### 5. Testar pelo navegador

Abra [http://localhost:6274]

Selecione a conexão Java já configurada e conecte. Em **Tools**, escolha `praticar_questao` e execute:

```json
{
  "operacao": "sortear"
}
```

Para corrigir uma resposta, execute a mesma ferramenta com:

```json
{
  "operacao": "corrigir",
  "id": 1,
  "alternativa": 2
}
```

Substitua `id` pelo ID recebido no sorteio e `alternativa` pela posição da resposta escolhida: A = 1, B = 2 e assim por diante.

Esse teste não depende do Claude Desktop. O endereço `localhost:6274` corresponde à interface do Inspector, não a um endpoint HTTP do servidor MCP.

## Conectar ao Claude Desktop

### 1. Instalar o Claude Desktop e manter o Docker em execução

Instale o Claude Desktop no mesmo computador e entre na sua conta. Com o Docker Desktop aberto, execute na pasta do projeto:

```powershell
docker compose up --build -d
```

Se o projeto já estiver em execução, pode continuar para o próximo passo.

### 2. Obter o caminho do Compose

No PowerShell, dentro da pasta do projeto, execute:

```powershell
(Resolve-Path compose.yaml).Path.Replace('\','/')
```

Copie o resultado. Por exemplo:

```text
C:/Projetos/mcp-servidor-estudos/compose.yaml
```

Use o caminho retornado no computador em que o Claude Desktop será executado.

### 3. Configurar o servidor MCP no Claude

No Claude Desktop, abra **Configurações → Desenvolvedor → Editar configuração**. No arquivo `claude_desktop_config.json`, utilize:

```json
{
  "mcpServers": {
    "estudos": {
      "command": "docker",
      "args": [
        "compose",
        "-f",
        "C:/Projetos/mcp-servidor-estudos/compose.yaml",
        "exec",
        "-T",
        "mcp-inspector",
        "java",
        "-jar",
        "/opt/mcp/server/mcp_server01_server.jar"
      ]
    }
  }
}
```

Substitua apenas o caminho de `compose.yaml` pelo resultado do passo anterior. O caminho `/opt/mcp/server/mcp_server01_server.jar` é interno ao contêiner e deve permanecer como no exemplo.

Se já houver outros servidores configurados, acrescente a entrada `estudos` dentro de `mcpServers`, preservando as demais entradas e a sintaxe JSON.

### 4. Reiniciar e testar o Claude

Salve o arquivo, encerre completamente o Claude Desktop — inclusive pela bandeja do Windows, se continuar aberto — e abra novamente.

Em uma nova conversa, envie:

> Use a ferramenta praticar_questao para sortear uma questão e aguarde minha resposta.

Responda à questão e peça:

> Corrija minha resposta usando a ferramenta praticar_questao.

O servidor também disponibiliza ferramentas de cadastro de questões e de simulado. Para testar um simulado, você pode pedir:

> Use a ferramenta praticar_simulado para iniciar um simulado e me apresentar as questões.

## Utilizar novamente e encerrar

Nas próximas vezes, abra o Docker Desktop e execute na pasta do projeto:

```powershell
docker compose up -d
```

Depois, abra o Claude Desktop. A configuração MCP só precisa ser refeita se o caminho do projeto ou a forma de execução mudar. Quando alterar o código do projeto, use `docker compose up --build -d` para reconstruir a imagem.

Para encerrar os serviços:

```powershell
docker compose down
```

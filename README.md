# ServiceHub API — Checkpoint de Desenvolvimento Backend

## Sobre o projeto

O ServiceHub API é um projeto acadêmico de desenvolvimento de uma API REST para gerenciar um catálogo de serviços. A aplicação centraliza informações como nome, descrição, preço, duração estimada e categoria, permitindo cadastrar, consultar, atualizar e excluir serviços.

Esta versão corresponde ao checkpoint: uma entrega parcial destinada a demonstrar a evolução do backend, receber feedback e identificar ajustes antes da entrega final. O recurso desenvolvido nesta etapa é **Serviço**.

## Escopo do checkpoint

| Requisito da atividade | Implementação no projeto |
| --- | --- |
| Spring Boot com dependências essenciais | Spring Web MVC, Spring Data JPA, PostgreSQL e SpringDoc OpenAPI configurados no Maven |
| Recurso com CRUD completo | Cadastro, listagem, consulta por ID, atualização e exclusão de serviços |
| Documentação Swagger | Interface em `/swagger-ui.html`, com summaries, descriptions, schemas e exemplos |
| Métodos HTTP e status adequados | POST, GET, PUT, PATCH e DELETE, com respostas 201, 200 e 204 |
| Tratamento de erros | Validação dos campos e respostas para dados inválidos, recursos inexistentes e nomes duplicados |
| Separação de responsabilidades | Camadas Controller, Service e Repository, DTOs e tratamento centralizado de exceções |

Além do CRUD, a API permite filtrar serviços por categoria e alterar seu status entre ativo e inativo. O escopo atual é o gerenciamento do catálogo de serviços.

## Tecnologias

| Tecnologia | Finalidade |
| --- | --- |
| Java 21 | Linguagem e plataforma de execução |
| Spring Boot 4.1.1 | Configuração e inicialização da aplicação |
| Spring Web MVC | Implementação dos endpoints REST |
| Spring Data JPA / Hibernate | Persistência e mapeamento das entidades |
| PostgreSQL 17 | Banco de dados utilizado na execução local com Docker |
| Jakarta Bean Validation | Validação dos dados recebidos |
| SpringDoc OpenAPI 3.1.1 | Documentação da API e Swagger UI |
| Maven Wrapper | Execução do build sem instalação separada do Maven |
| JUnit, MockMvc e H2 | Testes automatizados com banco em memória |

## Arquitetura e organização

```text
src/main/java/br/com/servicehub/api
├── config        # Configuração da documentação OpenAPI
├── controller    # Requisições e respostas HTTP
├── dto           # Contratos de entrada e saída, validações e schemas
├── entity        # Entidade Serviço e enumeração de categorias
├── exception     # Exceções e tratamento padronizado de erros
├── repository    # Consultas e persistência com Spring Data JPA
└── service       # Regras de negócio e transações
```

O `ServicoController` delega as operações ao `ServicoService`, que aplica as regras de negócio e acessa o `ServicoRepository`. Os DTOs separam os dados enviados pela API da entidade persistida. O `GlobalExceptionHandler` centraliza a conversão de exceções em respostas HTTP.

## Recurso Serviço

| Campo de entrada | Descrição e validação |
| --- | --- |
| `nome` | Obrigatório, de 3 a 100 caracteres; verificação de duplicidade sem diferenciar maiúsculas e minúsculas |
| `descricao` | Opcional, com até 500 caracteres |
| `preco` | Obrigatório, a partir de R$ 0,01, com até 8 dígitos inteiros e 2 decimais |
| `duracaoMinutos` | Obrigatório, entre 1 e 1440 minutos |
| `categoria` | Obrigatória, conforme as categorias disponíveis |

Categorias: `LIMPEZA`, `MANUTENCAO`, `TECNOLOGIA`, `CONSULTORIA`, `BELEZA` e `OUTROS`.

As respostas também incluem `id`, `ativo`, `criadoEm` e `atualizadoEm`. O identificador é gerado pelo banco e as datas são preenchidas pela aplicação. Novos serviços são criados com `ativo: true`. A desativação preserva o cadastro e o serviço continua aparecendo nas consultas; a exclusão remove o registro definitivamente.

## Endpoints

URL base local: `http://localhost:8080`.

| Método | Rota | Operação | Resposta de sucesso |
| --- | --- | --- | --- |
| POST | `/api/servicos` | Cadastrar serviço | `201 Created`, com corpo e cabeçalho `Location` |
| GET | `/api/servicos` | Listar serviços; aceita `?categoria=MANUTENCAO` | `200 OK` |
| GET | `/api/servicos/{id}` | Consultar serviço pelo identificador | `200 OK` |
| PUT | `/api/servicos/{id}` | Atualizar dados do serviço | `200 OK` |
| PATCH | `/api/servicos/{id}/status` | Ativar ou desativar serviço | `200 OK` |
| DELETE | `/api/servicos/{id}` | Excluir serviço | `204 No Content` |

### Exemplos de payload

O POST e o PUT recebem um corpo JSON no formato abaixo. No PUT, todos os campos obrigatórios devem ser enviados; o status é alterado pelo PATCH.

```json
{
  "nome": "Instalação de ar-condicionado",
  "descricao": "Instalação completa de aparelho split até 12.000 BTUs.",
  "preco": 350.00,
  "duracaoMinutos": 120,
  "categoria": "MANUTENCAO"
}
```

Para desativar um serviço, envie para `PATCH /api/servicos/{id}/status`:

```json
{
  "ativo": false
}
```

### Tratamento de erros

| Status | Situação |
| --- | --- |
| `400 Bad Request` | Campos inválidos, corpo ausente ou malformado, ID ou categoria em formato inválido |
| `404 Not Found` | Serviço não encontrado |
| `409 Conflict` | Nome de serviço já cadastrado |
| `405 Method Not Allowed` | Método HTTP não suportado para a rota |
| `500 Internal Server Error` | Erro inesperado, com mensagem genérica na resposta |

As respostas de erro possuem `timestamp`, `status`, `erro`, `mensagem` e `path`. Erros de validação também incluem a lista `campos`, com o nome de cada campo inválido e sua mensagem.

## Execução local

### Pré-requisitos

- JDK 21 configurado no terminal e na IDE.
- Docker com Docker Compose para iniciar o PostgreSQL, ou uma instalação local do banco.
- Acesso à internet na primeira execução para baixar o Maven e as dependências.

### Iniciar a aplicação

1. Clone o repositório e entre na pasta:

   ```bash
   git clone https://github.com/bibipsi/servicehub.git
   cd servicehub
   ```

2. Com o Docker em execução, inicie o banco e aguarde sua disponibilidade:

   ```bash
   docker compose up -d --wait
   ```

3. Execute a API:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows, utilize `mvnw.cmd spring-boot:run`.

4. Abra o Swagger UI em **http://localhost:8080/swagger-ui.html**. A especificação OpenAPI em JSON fica em **http://localhost:8080/v3/api-docs**.

O banco configurado no Compose é `servicehub`, com usuário e senha `postgres`, disponível em `localhost:5432`. Essas credenciais são destinadas ao desenvolvimento local. O Hibernate está configurado com `ddl-auto=update` para atualizar a estrutura das tabelas na inicialização.

### Configurações de ambiente

| Variável | Valor padrão | Finalidade |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/servicehub` | Endereço do banco |
| `DB_USERNAME` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |

Para utilizar um PostgreSQL instalado diretamente no computador, crie previamente o banco e configure essas variáveis no terminal ou na IDE. Arquivos `.env` não são carregados automaticamente pela aplicação.

No macOS, se outra versão do Java estiver selecionada, execute antes de iniciar:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
```

Se a porta HTTP estiver ocupada, utilize, por exemplo, `SERVER_PORT=8081 ./mvnw spring-boot:run`. Se a porta 5432 estiver ocupada, ajuste a porta publicada no Compose e o endereço em `DB_URL`.

Para encerrar a API, pressione `Ctrl+C`. Para parar o banco, execute `docker compose down`; os dados permanecem no volume Docker.

## Documentação Swagger

Os endpoints são agrupados pela tag **Serviços**. As anotações `@Operation` descrevem o propósito de cada operação; `@Schema` documenta os campos dos DTOs; e `@ApiResponses` apresenta os códigos de resposta. Há exemplos de payloads de cadastro, atualização, alteração de status e erros.

A opção **Try it out** permite executar as requisições na interface do Swagger com a aplicação em execução.

## Testes e verificação do build

```bash
./mvnw clean verify
```

Esse comando compila o projeto, executa os testes e gera o pacote. No Windows, utilize `mvnw.cmd clean verify`. Os testes usam H2 em memória, sem necessidade de iniciar o PostgreSQL.

A suíte existente verifica a inicialização da aplicação, o fluxo de CRUD, o filtro por categoria, a alteração de status, dados inválidos, JSON malformado, ID inválido, recursos inexistentes, nomes duplicados e a disponibilização da documentação OpenAPI/Swagger.

O resultado com H2 deve ser complementado pela demonstração local com PostgreSQL antes da entrega.

## Roteiro de demonstração do checkpoint

1. Iniciar o PostgreSQL e a API e abrir o Swagger UI.
2. Cadastrar um serviço e observar o status `201` e o cabeçalho `Location`.
3. Listar os serviços, filtrar por categoria e consultar o ID criado.
4. Atualizar o preço e a duração pelo PUT.
5. Desativar o serviço pelo PATCH.
6. Demonstrar um cadastro inválido (`400`) e um nome duplicado (`409`).
7. Excluir o serviço (`204`) e consultar o mesmo ID para demonstrar o `404`.
8. Apresentar a separação entre Controller, Service e Repository e os testes existentes.

## Entrega acadêmica

O checkpoint exige o envio no AVA do link do repositório atualizado ou de um arquivo compactado do projeto, acompanhado do arquivo da apresentação de slides.

Repositório: **https://github.com/bibipsi/servicehub**.

A apresentação de slides é uma entrega separada e ainda não está incluída neste repositório. Antes da apresentação em sala, execute o build e o roteiro de demonstração para verificar a compilação, o CRUD e o Swagger no ambiente local.

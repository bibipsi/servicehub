# ServiceHub API — Desenvolvimento Backend

## Sobre o projeto

O ServiceHub API é um projeto acadêmico de desenvolvimento de uma API REST para gerenciar um catálogo de serviços. A aplicação centraliza informações como nome, descrição, preço, duração estimada e categoria, permitindo cadastrar, consultar, atualizar e excluir serviços.

A primeira etapa (checkpoint) entregou o CRUD do recurso **Serviço**. A segunda etapa, correspondente às aulas 08, 09 e 10, acrescenta usuários, autenticação JWT, autorização por papéis (RBAC), verificação de propriedade dos recursos, proteções de segurança, migrações Flyway, um endpoint alternativo em GraphQL, a ampliação dos testes automatizados e a execução completa com Docker Compose.

## Segunda etapa — aulas 08, 09 e 10

| Aula | Requisito | Implementação no projeto |
| --- | --- | --- |
| 08 | Spring Security com `SecurityFilterChain` | `SecurityConfig`: CSRF, form login e HTTP Basic desabilitados; regras por rota; filtro JWT |
| 08 | Senhas com `BCryptPasswordEncoder` | Bean `PasswordEncoder`; somente o hash é salvo em `usuarios.senha_hash` |
| 08 | Bean Validation e `@Valid` | `@NotBlank`, `@Email`, `@Size`, `@Pattern` e `@SemHtml` nos DTOs; `@Valid` em todos os corpos recebidos |
| 08 | Tratamento global de erros | `GlobalExceptionHandler` (`@RestControllerAdvice`) e `ErroSegurancaHandler`, ambos no formato `ErroResponse` |
| 08 | `@EnableMethodSecurity` e `@PreAuthorize` | Operações de escrita de serviços restritas a `PROVIDER`/`ADMIN`; listagem de usuários e alteração de papel restritas a `ADMIN` |
| 08 | XSS, SQL Injection, BOLA e Mass Assignment | Ver a seção [Proteções de segurança](#proteções-de-segurança) |
| 09 | Login e cadastro | `POST /api/auth/login` e `POST /api/auth/cadastro` |
| 09 | JWT assinado com expiração configurável | `JwtService` (HS256, Nimbus via Spring Security), `JWT_SECRET` e `JWT_EXPIRATION` |
| 09 | Sessão stateless e `JwtAuthFilter` | `SessionCreationPolicy.STATELESS`; filtro lê `Authorization: Bearer <token>` |
| 09 | RBAC com ADMIN, PROVIDER e CLIENT | Enum `PapelUsuario`, coluna `usuarios.papel` e authorities `ROLE_*` |
| 09 | 401 e 403 | 401 sem token ou com token inválido/expirado; 403 sem papel ou sem propriedade do recurso |
| 09 | Verificação de propriedade | Serviço só é alterado pelo prestador dono ou por ADMIN; dados de usuário só pelo próprio usuário ou por ADMIN |
| 09 | Endpoint alternativo GraphQL | `POST /graphql` com as consultas `servicos` e `servico`, reutilizando o `ServicoService` |
| 10 | Testes unitários com Mockito | `ServicoServiceTest`, `UsuarioServiceTest`, `AuthServiceTest` e `JwtServiceTest` |
| 10 | Testes do filtro JWT | `JwtAuthFilterTest`: token expirado, adulterado, sem assinatura (`alg: none`), vazio e de usuário excluído |
| 10 | Testes de Controllers com MockMvc | `ServicoSegurancaTest`, `AuthControllerTest`, `UsuarioControllerTest`, `ServicoGraphQLControllerTest` e o `ServicoControllerTest` original |
| 10 | Testes de integração | Fluxo completo em H2 (executado) e em PostgreSQL real com Testcontainers (requer Docker; ver [Testes](#testes-e-verificação-do-build)) |

## Escopo da primeira etapa (checkpoint)

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
| Spring Security 7.1.1 | Autenticação, autorização e segurança de métodos |
| Spring Security OAuth2 JOSE (Nimbus) | Geração e validação dos tokens JWT |
| Spring for GraphQL 2.0.5 | Endpoint alternativo em GraphQL |
| Spring Data JPA / Hibernate | Persistência e mapeamento das entidades |
| Flyway 12.4.0 | Migrações versionadas do banco |
| PostgreSQL 17 | Banco de dados utilizado na execução local com Docker |
| Jakarta Bean Validation | Validação dos dados recebidos |
| SpringDoc OpenAPI 3.1.1 | Documentação da API e Swagger UI |
| Maven Wrapper | Execução do build sem instalação separada do Maven |
| JUnit 5, Mockito, MockMvc e H2 | Testes unitários, de Controllers e de integração |
| Testcontainers 2.0.5 | Teste de integração com PostgreSQL real |
| Docker Compose | Execução da API e do PostgreSQL em containers |

Todas as versões são gerenciadas pelo BOM do Spring Boot 4.1.1, exceto o SpringDoc, já definido no projeto.

## Arquitetura e organização

```text
src/main/java/br/com/servicehub/api
├── config        # OpenAPI, SecurityFilterChain, PasswordEncoder e ADMIN inicial
├── controller    # REST (serviços, autenticação, usuários) e GraphQL
├── dto           # Contratos de entrada e saída, validações e schemas
├── entity        # Entidades Serviço e Usuário e enumerações
├── exception     # Exceções e tratamento padronizado de erros
├── repository    # Consultas e persistência com Spring Data JPA
├── security      # JWT, JwtAuthFilter, usuário autenticado e respostas 401/403
├── service       # Regras de negócio, transações e verificação de propriedade
└── validation    # Validação @SemHtml contra XSS

src/main/resources
├── db/migration  # Migrações Flyway (V1, V2, V3)
└── graphql       # Schema GraphQL
```

Os Controllers delegam as operações aos Services, que aplicam as regras de negócio, verificam a propriedade dos recursos e acessam os Repositories. Os DTOs separam os dados enviados pela API das entidades persistidas. O `GlobalExceptionHandler` centraliza a conversão de exceções em respostas HTTP; as falhas de autenticação e autorização ocorridas nos filtros, antes dos Controllers, são respondidas pelo `ErroSegurancaHandler` no mesmo formato.

O fluxo de uma requisição protegida é: `JwtAuthFilter` valida o token e carrega o usuário do banco → `SecurityFilterChain` aplica as regras por rota → `@PreAuthorize` verifica o papel → o Service verifica se o usuário é dono do recurso.

## Recurso Serviço

| Campo de entrada | Descrição e validação |
| --- | --- |
| `nome` | Obrigatório, de 3 a 100 caracteres; verificação de duplicidade sem diferenciar maiúsculas e minúsculas |
| `descricao` | Opcional, com até 500 caracteres |
| `preco` | Obrigatório, a partir de R$ 0,01, com até 8 dígitos inteiros e 2 decimais |
| `duracaoMinutos` | Obrigatório, entre 1 e 1440 minutos |
| `categoria` | Obrigatória, conforme as categorias disponíveis |

Categorias: `LIMPEZA`, `MANUTENCAO`, `TECNOLOGIA`, `CONSULTORIA`, `BELEZA` e `OUTROS`.

As respostas também incluem `id`, `ativo`, `prestadorId`, `criadoEm` e `atualizadoEm`. O `prestadorId` é sempre o usuário autenticado que cadastrou o serviço e não pode ser enviado no corpo; serviços cadastrados antes da segunda etapa ficam com `prestadorId: null` e só podem ser alterados por um ADMIN. Nome e descrição não aceitam os caracteres `<` e `>`. O identificador é gerado pelo banco e as datas são preenchidas pela aplicação. Novos serviços são criados com `ativo: true`. A desativação preserva o cadastro e o serviço continua aparecendo nas consultas; a exclusão remove o registro definitivamente.

## Usuários e papéis

| Papel | Permissões |
| --- | --- |
| `CLIENT` | Papel de todo usuário cadastrado pela rota pública. Consulta o catálogo e o próprio perfil |
| `PROVIDER` | Também cadastra serviços e altera, ativa/desativa e exclui **os próprios** serviços |
| `ADMIN` | Altera qualquer serviço, lista usuários, consulta qualquer perfil e define papéis |

O cadastro público nunca aceita o papel no corpo da requisição. Um ADMIN promove usuários com `PATCH /api/usuarios/{id}/papel`. O primeiro ADMIN é criado na inicialização quando as variáveis `ADMIN_EMAIL` e `ADMIN_PASSWORD` estão definidas e ainda não existe usuário com esse e-mail; se o e-mail já pertencer a outro usuário, nada é alterado. Troque a senha de exemplo do `.env.example` antes de iniciar a aplicação.

Usuários com `ativo = false` não fazem login, e seus tokens deixam de ser aceitos. A API ainda não possui endpoint de desativação: o campo é alterado diretamente no banco.

Campos de um usuário: `nome` (3 a 100 caracteres, sem `<` e `>`), `email` (válido, único e salvo em minúsculas), `senha` (8 a 72 caracteres e no máximo 72 bytes em UTF-8, limite do BCrypt; usada apenas no cadastro e no login) e `telefone` (opcional, até 20 caracteres com números, espaços, parênteses, `+` e `-`). As respostas nunca incluem a senha ou seu hash.

## Endpoints

URL base local: `http://localhost:8080`. Nas rotas protegidas, envie o header `Authorization: Bearer <token>`.

### Autenticação (públicas)

| Método | Rota | Operação | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/cadastro` | Cadastrar usuário (sempre `CLIENT`) | `201 Created`, com `Location` | `400`, `409` |
| POST | `/api/auth/login` | Autenticar e obter o JWT | `200 OK` | `400`, `401` |

### Serviços

| Método | Rota | Operação | Acesso | Sucesso |
| --- | --- | --- | --- | --- |
| POST | `/api/servicos` | Cadastrar serviço | `PROVIDER` ou `ADMIN` | `201 Created`, com corpo e cabeçalho `Location` |
| GET | `/api/servicos` | Listar serviços; aceita `?categoria=MANUTENCAO` | Público | `200 OK` |
| GET | `/api/servicos/{id}` | Consultar serviço pelo identificador | Público | `200 OK` |
| PUT | `/api/servicos/{id}` | Atualizar dados do serviço | Prestador dono ou `ADMIN` | `200 OK` |
| PATCH | `/api/servicos/{id}/status` | Ativar ou desativar serviço | Prestador dono ou `ADMIN` | `200 OK` |
| DELETE | `/api/servicos/{id}` | Excluir serviço | Prestador dono ou `ADMIN` | `204 No Content` |

### Usuários (autenticadas)

| Método | Rota | Operação | Acesso | Sucesso |
| --- | --- | --- | --- | --- |
| GET | `/api/usuarios/me` | Consultar o próprio perfil | Qualquer usuário autenticado | `200 OK` |
| GET | `/api/usuarios/{id}` | Consultar perfil | Próprio usuário ou `ADMIN` | `200 OK` |
| PUT | `/api/usuarios/{id}` | Atualizar nome e telefone | Próprio usuário ou `ADMIN` | `200 OK` |
| GET | `/api/usuarios` | Listar usuários | `ADMIN` | `200 OK` |
| PATCH | `/api/usuarios/{id}/papel` | Alterar papel | `ADMIN` (exceto o próprio papel) | `200 OK` |

### GraphQL (público)

| Método | Rota | Operação |
| --- | --- | --- |
| POST | `/graphql` | Consultas `servicos(categoria)` e `servico(id)` |
| GET | `/graphiql` | Interface interativa para testar as consultas (desative com `GRAPHIQL_ENABLED=false`) |

Exemplo de consulta:

```bash
curl -s http://localhost:8080/graphql -H "Content-Type: application/json" \
  -d '{ "query": "{ servicos(categoria: MANUTENCAO) { id nome preco prestadorId } }" }'
```

Serviço inexistente em `servico(id)` retorna `data.servico: null` e um erro com `extensions.classification: NOT_FOUND`. O schema fica em `src/main/resources/graphql/schema.graphqls`.

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

Cadastro (`POST /api/auth/cadastro`) e login (`POST /api/auth/login`):

```json
{ "nome": "Maria Souza", "email": "maria@email.com", "senha": "SenhaForte123", "telefone": "(62) 99999-0000" }
```

```json
{ "email": "maria@email.com", "senha": "SenhaForte123" }
```

Resposta do login:

```json
{ "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", "tipo": "Bearer", "expiraEmSegundos": 3600 }
```

O payload do token contém apenas `iss`, `sub` (id do usuário), `papel`, `iat` e `exp`; e-mail, telefone e senha não são incluídos. A cada requisição, o usuário é recarregado do banco: uma alteração de papel ou desativação vale imediatamente, inclusive para tokens já emitidos.

### Tratamento de erros

| Status | Situação |
| --- | --- |
| `400 Bad Request` | Campos inválidos, corpo ausente ou malformado, ID ou categoria em formato inválido |
| `401 Unauthorized` | Token ausente, inválido ou expirado; usuário inativo; e-mail ou senha inválidos no login |
| `403 Forbidden` | Usuário sem o papel exigido ou tentando acessar recurso de outro usuário |
| `404 Not Found` | Serviço, usuário ou rota não encontrados |
| `409 Conflict` | Nome de serviço ou e-mail já cadastrado; ADMIN tentando alterar o próprio papel |
| `405 Method Not Allowed` | Método HTTP não suportado para a rota |
| `406 Not Acceptable` | Header `Accept` pede um formato diferente de JSON |
| `415 Unsupported Media Type` | Corpo enviado com `Content-Type` diferente de `application/json` |
| `500 Internal Server Error` | Erro inesperado, com mensagem genérica na resposta |

As respostas de erro possuem `timestamp`, `status`, `erro`, `mensagem` e `path`. Erros de validação também incluem a lista `campos`, com o nome de cada campo inválido e sua mensagem. As respostas `401` incluem o header `WWW-Authenticate: Bearer`. No login, e-mail inexistente e senha incorreta retornam a mesma mensagem, para não revelar quais contas existem.

## Proteções de segurança

| Ameaça | Proteção |
| --- | --- |
| Senhas expostas | Hash BCrypt; a senha e o hash nunca aparecem em respostas ou no JWT |
| Token forjado ou adulterado | Assinatura HS256 com chave de no mínimo 32 bytes; validação de assinatura, emissor (`iss`) e expiração |
| XSS | `@SemHtml` rejeita `<` e `>` em nomes e descrições; a API responde somente JSON, com os headers de segurança padrão do Spring Security (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` e `Cache-Control`) |
| SQL Injection | Somente consultas derivadas do Spring Data JPA, executadas com parâmetros vinculados (*prepared statements*); não há SQL concatenado |
| BOLA (acesso a objeto de outro usuário) | Os Services comparam o dono do recurso com o usuário do token antes de consultar ou alterar; caso contrário, `403` |
| Mass Assignment | DTOs específicos por operação, sem `papel`, `ativo`, `id` ou `prestadorId`; esses valores são definidos pelo servidor e campos extras no JSON são ignorados |
| Escalada de privilégios | Papel alterado somente por ADMIN via `@PreAuthorize`; um ADMIN não pode alterar o próprio papel |
| CSRF | Desabilitado de forma consciente: a API é stateless e não usa cookies de sessão |

## Execução local

### Pré-requisitos

- JDK 21 configurado no terminal e na IDE.
- Docker com Docker Compose para iniciar o PostgreSQL, ou uma instalação local do banco.
- Acesso à internet na primeira execução para baixar o Maven e as dependências.

### Opção 1: API e PostgreSQL em containers

1. Crie o arquivo de variáveis a partir do exemplo e defina, no mínimo, `JWT_SECRET`:

   ```bash
   cp .env.example .env
   ```

2. Construa a imagem da API e inicie os dois containers:

   ```bash
   docker compose up -d --build --wait
   ```

   O container `api` só inicia depois que o healthcheck do PostgreSQL indica que o banco está pronto (`depends_on` com `service_healthy`). A API acessa o banco pelo nome do serviço `postgres` na rede interna do Compose, e os dados ficam no volume `servicehub-data`.

3. Acesse **http://localhost:8080/swagger-ui.html**. Para acompanhar os logs, use `docker compose logs -f api`.

Para parar, execute `docker compose down`. **Não use** `docker compose down -v`: a opção `-v` apaga o volume com os dados.

> A configuração Docker (`Dockerfile`, `docker-compose.yml`, `.env.example` e `.dockerignore`) foi revisada estaticamente, mas ainda não foi executada, porque a máquina de desenvolvimento não tinha Docker. Na primeira execução, confira se `docker compose up -d --build --wait` termina com os dois containers saudáveis e se o Swagger responde em `http://localhost:8080/swagger-ui.html`.

### Opção 2: API pelo Maven e PostgreSQL no Docker

1. Clone o repositório e entre na pasta:

   ```bash
   git clone https://github.com/bibipsi/servicehub.git
   cd servicehub
   ```

2. Com o Docker em execução e o arquivo `.env` criado (`cp .env.example .env`), inicie somente o banco e aguarde sua disponibilidade:

   ```bash
   docker compose up -d --wait postgres
   ```

3. Execute a API:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows, utilize `mvnw.cmd spring-boot:run`.

4. Abra o Swagger UI em **http://localhost:8080/swagger-ui.html**. A especificação OpenAPI em JSON fica em **http://localhost:8080/v3/api-docs**.

O banco configurado no Compose é `servicehub`, com usuário e senha `postgres` por padrão, disponível em `localhost:5432`. Essas credenciais são destinadas ao desenvolvimento local.

### Banco de dados e migrações

A estrutura do banco é criada pelo Flyway na inicialização da aplicação e apenas validada pelo Hibernate (`ddl-auto=validate`):

| Migração | Conteúdo |
| --- | --- |
| `V1__cria_tabela_servicos.sql` | Tabela `servicos` da primeira etapa |
| `V2__cria_tabela_usuarios.sql` | Tabela `usuarios`, com e-mail único em minúsculas, `senha_hash` e `papel` restrito a `ADMIN`, `PROVIDER` e `CLIENT` |
| `V3__adiciona_prestador_em_servicos.sql` | Coluna opcional `servicos.prestador_id`, chave estrangeira para `usuarios` e índice |

Bancos criados na primeira etapa, quando o Hibernate usava `ddl-auto=update`, já possuem a tabela `servicos` sem histórico do Flyway. Para eles, `spring.flyway.baseline-on-migrate=true` registra a versão 1 como linha de base, sem recriar a tabela, e aplica apenas a V2 e a V3. Os serviços existentes são preservados com `prestador_id` nulo. Migrações já aplicadas não devem ser editadas: qualquer mudança no banco deve ser feita em uma nova migração (V4, V5...).

A V1 foi escrita para corresponder à tabela que o Hibernate 7.4.5 gera para a entidade `Servico` no PostgreSQL: mesmas colunas, tipos, tamanhos, nulidade, `UNIQUE(nome)` e `CHECK` de categoria. Apenas os nomes das constraints diferem, o que não afeta a validação. Essa equivalência foi conferida comparando a V1 com o DDL gerado pelo Hibernate, e não em um banco PostgreSQL real.

#### Validar as migrações em um banco existente

Antes de iniciar esta versão sobre o banco da primeira etapa, faça uma cópia de segurança e confira o schema:

```bash
docker compose up -d --wait postgres
docker exec servicehub-postgres pg_dump -U postgres servicehub > backup-servicehub.sql
docker exec -it servicehub-postgres psql -U postgres -d servicehub -c '\dt' -c '\d servicos'
```

1. A listagem de tabelas não deve conter `usuarios` nem `flyway_schema_history`. Se `usuarios` já existir, criada manualmente em testes anteriores, a V2 falhará; analise e renomeie essa tabela antes de continuar.
2. A tabela `servicos` deve ter exatamente as colunas da V1. Colunas extras obrigatórias (`NOT NULL` sem valor padrão), de versões antigas da entidade, impediriam novos cadastros.
3. Inicie a aplicação. No log, o Flyway deve registrar a criação da linha de base na versão 1 e a aplicação da V2 e da V3. Em seguida, o Hibernate valida o schema; qualquer divergência interrompe a inicialização com a coluna indicada.
4. Confira o histórico com `SELECT version, description, success FROM flyway_schema_history;` e verifique se os serviços antigos continuam presentes, com `prestador_id` nulo.

### Configurações de ambiente

| Variável | Valor padrão | Finalidade |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/servicehub` | Endereço do banco |
| `DB_USERNAME` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |
| `JWT_SECRET` | Chave de desenvolvimento | Chave HMAC do JWT, com no mínimo 32 caracteres. **Obrigatória no Docker Compose**; defina sempre um valor próprio fora do ambiente local |
| `JWT_EXPIRATION` | `1h` | Validade do token (por exemplo, `30m`, `2h`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Vazios | Criam o primeiro ADMIN na inicialização, se ainda não existir |
| `GRAPHIQL_ENABLED` | `true` | Habilita a interface `/graphiql` |

Para utilizar um PostgreSQL instalado diretamente no computador, crie previamente o banco e configure essas variáveis no terminal ou na IDE. Arquivos `.env` não são carregados automaticamente pela aplicação.

No macOS, se outra versão do Java estiver selecionada, execute antes de iniciar:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
```

Se a porta HTTP estiver ocupada, utilize, por exemplo, `SERVER_PORT=8081 ./mvnw spring-boot:run`. Se a porta 5432 estiver ocupada, ajuste a porta publicada no Compose e o endereço em `DB_URL`.

Para encerrar a API, pressione `Ctrl+C`. Para parar o banco, execute `docker compose down`; os dados permanecem no volume Docker.

### Testar a autenticação pelo terminal

Com a API em execução e `ADMIN_EMAIL`/`ADMIN_PASSWORD` definidos:

```bash
# Login do ADMIN e cadastro de um usuário
ADMIN=$(curl -s localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@servicehub.com","senha":"troque-esta-senha"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -s localhost:8080/api/auth/cadastro -H "Content-Type: application/json" \
  -d '{"nome":"João Prestador","email":"joao@email.com","senha":"SenhaForte123"}'

# Promover o usuário a PROVIDER (use o id retornado no cadastro)
curl -s -X PATCH localhost:8080/api/usuarios/2/papel -H "Authorization: Bearer $ADMIN" \
  -H "Content-Type: application/json" -d '{"papel":"PROVIDER"}'

# Login do prestador e cadastro de um serviço
TOKEN=$(curl -s localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"joao@email.com","senha":"SenhaForte123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -s localhost:8080/api/servicos -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Troca de chuveiro","preco":90.00,"duracaoMinutos":60,"categoria":"MANUTENCAO"}'
```

## Documentação Swagger

Os endpoints são agrupados pelas tags **Autenticação**, **Serviços** e **Usuários**. As anotações `@Operation` descrevem o propósito de cada operação; `@Schema` documenta os campos dos DTOs; e `@ApiResponses` apresenta os códigos de resposta, incluindo `401` e `403` nas rotas protegidas. Há exemplos de payloads de cadastro, atualização, alteração de status e erros.

A opção **Try it out** permite executar as requisições na interface do Swagger com a aplicação em execução. Para as rotas protegidas, faça login em `POST /api/auth/login`, clique em **Authorize** e cole o valor de `token`.

## Testes e verificação do build

```bash
./mvnw clean verify
```

Esse comando compila o projeto, executa os testes e gera o pacote. No Windows, utilize `mvnw.cmd clean verify`. A maior parte dos testes usa H2 em memória (modo PostgreSQL), com o schema criado pelas mesmas migrações Flyway da aplicação, sem necessidade de iniciar o PostgreSQL.

O teste `FluxoCompletoPostgresIntegracaoTest` usa Testcontainers para iniciar um PostgreSQL 17 real e verificar as migrações e o mapeamento JPA no banco de produção. Ele precisa do Docker em execução; sem Docker, é marcado como ignorado (*skipped*) em vez de falhar.

Resultado da última execução de `./mvnw clean verify`, em máquina sem Docker: **76 testes, 75 aprovados, 1 ignorado, 0 falhas e 0 erros**. O teste ignorado é o `FluxoCompletoPostgresIntegracaoTest`: até aqui, a integração com PostgreSQL real **não foi executada**, apenas a integração com H2. Para executá-la, rode o mesmo comando com o Docker ativo e confira se o relatório mostra `Skipped: 0`.

| Tipo | Classes | O que verificam |
| --- | --- | --- |
| Unitários (JUnit 5 + Mockito) | `ServicoServiceTest`, `UsuarioServiceTest`, `AuthServiceTest`, `JwtServiceTest` | Regras de negócio isoladas com `@Mock`/`@InjectMocks`, cenários de sucesso e exceção e `verify()`; geração, expiração, adulteração e conteúdo do JWT |
| Controllers (MockMvc) | `ServicoControllerTest` (original), `ServicoSegurancaTest`, `AuthControllerTest`, `UsuarioControllerTest`, `ServicoGraphQLControllerTest` | `201`, `400`, `404`, `401`, `403`, `415` e acesso autorizado; BOLA, Mass Assignment, XSS, BCrypt, limite de bytes da senha e consultas GraphQL |
| Filtro JWT (MockMvc) | `JwtAuthFilterTest` | Token válido, expirado, adulterado para ADMIN, sem assinatura, vazio e de usuário excluído |
| Integração | `FluxoCompletoH2IntegracaoTest`, `FluxoCompletoPostgresIntegracaoTest` | Cadastro → promoção → login → criação → consulta REST e GraphQL → bloqueio de alteração por outro usuário |
| Contexto | `ServicehubApiApplicationTests` | Inicialização da aplicação com Flyway e validação do schema |

Os testes geram tokens reais com o `JwtService`, de modo que as requisições passam pelo `JwtAuthFilter` e pelo `SecurityFilterChain` como em produção.

## Roteiro de demonstração

1. Iniciar o PostgreSQL e a API com `docker compose up -d --build --wait` e abrir o Swagger UI.
2. Cadastrar um usuário em `POST /api/auth/cadastro` e mostrar que o papel é `CLIENT`, mesmo enviando `"papel": "ADMIN"`.
3. Fazer login como ADMIN, promover o usuário a `PROVIDER` e fazer login como prestador.
4. Tentar cadastrar um serviço sem token (`401`) e com o token de um `CLIENT` (`403`).
5. Cadastrar um serviço como prestador e observar o status `201`, o cabeçalho `Location` e o `prestadorId`.
6. Listar os serviços, filtrar por categoria e consultar o ID criado, via REST e via GraphQL (`/graphiql`).
7. Atualizar o preço e a duração pelo PUT e desativar o serviço pelo PATCH.
8. Tentar alterar o serviço com o token de outro prestador (`403`).
9. Demonstrar um cadastro inválido (`400`), um nome com `<script>` (`400`) e um nome duplicado (`409`).
10. Excluir o serviço (`204`) e consultar o mesmo ID para demonstrar o `404`.
11. Apresentar a separação entre Controller, Service e Repository, as migrações Flyway e a execução de `./mvnw clean verify`.

## Entrega acadêmica

As entregas são feitas no AVA, com o link do repositório atualizado ou um arquivo compactado do projeto, acompanhado do arquivo da apresentação de slides.

Repositório: **https://github.com/bibipsi/servicehub**.

A apresentação de slides é uma entrega separada e ainda não está incluída neste repositório. Antes da apresentação em sala, execute o build e o roteiro de demonstração para verificar a compilação, o CRUD e o Swagger no ambiente local.

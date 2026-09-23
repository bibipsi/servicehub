# ServiceHub API

API REST para gerenciamento do catálogo de serviços da plataforma ServiceHub.

## Tecnologias

- Java 21
- Spring Boot 4 (Spring Web, Spring Data JPA, Validation)
- PostgreSQL
- SpringDoc OpenAPI (Swagger UI)

## Preparar o Mac

1. Instale o **JDK 21**. O [Amazon Corretto 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/macos-install.html) oferece instalador para macOS; escolha a arquitetura do seu Mac (Apple Silicon/ARM64 ou Intel/x64).
2. Instale e abra o [Docker Desktop](https://docs.docker.com/desktop/setup/install/mac-install/). Aguarde o Docker ficar pronto antes de subir o banco.
3. Para usar Git e publicar no GitHub, instale as ferramentas de linha de comando da Apple pelo Terminal:

   ```bash
   xcode-select --install
   ```

Depois da instalação, selecione o Java 21 no terminal em que executará o projeto:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
docker compose version
git --version
```

O comando `java -version` deve mostrar a versão 21. Se usar a execução Java da IDE, configure também o JDK do projeto como 21.
Não é necessário instalar Maven: o projeto inclui o Maven Wrapper (`mvnw` e `mvnw.cmd`). A primeira execução precisa de internet para baixar Maven e dependências.

## Como executar

Abra o terminal na pasta do projeto (a que contém `pom.xml`).

1. Suba o PostgreSQL (com Docker):

   ```bash
   docker compose up -d --wait
   ```

   Ou crie manualmente um banco `servicehub` com usuário `postgres` / senha `postgres`.
   As credenciais podem ser alteradas pelas variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

2. Execute a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Acesse a documentação: http://localhost:8080/swagger-ui.html

   Se a porta 8080 estiver ocupada, use outra porta: `SERVER_PORT=8081 ./mvnw spring-boot:run`

4. Confira a API em outro terminal:

   ```bash
   curl http://localhost:8080/api/servicos
   ```

   Em um banco novo, a resposta esperada é `[]`.

Para parar a API, pressione `Ctrl+C`. Para parar o banco, execute `docker compose down`; os dados permanecem no volume Docker.

### Configuração local

Os valores padrão do banco são apenas para desenvolvimento local. O Compose expõe o PostgreSQL somente em `127.0.0.1:5432`.
Se utilizar outro PostgreSQL, configure as variáveis no terminal antes de iniciar a API:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/servicehub'
export DB_USERNAME='postgres'
export DB_PASSWORD='postgres'
./mvnw spring-boot:run
```

O Spring Boot não carrega arquivos `.env` automaticamente nesta configuração. Use variáveis exportadas no terminal ou a configuração de ambiente da IDE.
Se a porta 5432 estiver ocupada, pare o outro PostgreSQL ou altere a porta publicada no Compose e ajuste `DB_URL` para a mesma porta.

## Endpoints — Serviços

| Método | Rota                        | Descrição                    | Sucesso |
|--------|-----------------------------|------------------------------|---------|
| POST   | `/api/servicos`             | Cadastrar serviço            | 201     |
| GET    | `/api/servicos`             | Listar serviços (`?categoria=`) | 200  |
| GET    | `/api/servicos/{id}`        | Buscar serviço por ID        | 200     |
| PUT    | `/api/servicos/{id}`        | Atualizar serviço            | 200     |
| PATCH  | `/api/servicos/{id}/status` | Ativar/desativar serviço     | 200     |
| DELETE | `/api/servicos/{id}`        | Excluir serviço              | 204     |

Erros: `400` (dados inválidos), `404` (serviço não encontrado), `409` (nome duplicado).

## Estrutura

```
src/main/java/br/com/servicehub/api
├── config        # Configuração do OpenAPI/Swagger
├── controller    # Endpoints REST
├── dto           # Objetos de entrada e saída
├── entity        # Entidades JPA
├── exception     # Exceções e tratamento global de erros
├── repository    # Acesso a dados (Spring Data JPA)
└── service       # Regras de negócio
```

## Testes

```bash
./mvnw test
```

Os testes usam banco H2 em memória, sem necessidade do PostgreSQL.

Para testar e gerar o pacote executável:

```bash
./mvnw clean verify
```

## Publicar no GitHub

O `.gitignore` exclui arquivos compilados (`target/`), configurações de IDE, arquivos `.env`, logs e arquivos do macOS. Mantenha o Maven Wrapper e a pasta `.mvn/wrapper/` no repositório.

Depois de instalar o Git e ter a URL do repositório de destino:

```bash
git init -b main
git add .
git diff --cached --stat
git diff --cached
git commit -m "Initial commit: ServiceHub API"
git remote add origin URL_DO_REPOSITORIO
git push -u origin main
```

Substitua `URL_DO_REPOSITORIO` pela URL real. Esses comandos pressupõem um repositório remoto vazio; se ele já contiver arquivos, integre o histórico antes de enviar. Revise os arquivos preparados antes do commit para não publicar credenciais. O GitHub solicitará autenticação conforme o método escolhido (HTTPS ou SSH).

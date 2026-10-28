# Etapa 1: compila a aplicação com o Maven Wrapper do projeto.
# Os testes são executados fora do container (./mvnw verify), pois o teste com
# Testcontainers precisa de acesso ao Docker.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

# Etapa 2: imagem final apenas com o JRE e o JAR, executada por um usuário sem privilégios
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system servicehub && useradd --system --gid servicehub servicehub
COPY --from=build /app/target/servicehub-api-*.jar app.jar
USER servicehub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

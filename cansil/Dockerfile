# syntax=docker/dockerfile:1

# Etapa 1: compila o projeto e gera o arquivo JAR.
FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copiar o pom antes do codigo permite reutilizar o cache das dependencias.
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q verify

# Exportação opcional das evidências/JAR, útil quando o OneDrive bloqueia o build local.
# Não participa da imagem de execução, que continua sendo a última etapa.
FROM scratch AS verification-artifacts
COPY --from=build /workspace/target/surefire-reports/ /surefire-reports/
COPY --from=build /workspace/target/*.jar /

# Etapa 2: imagem final, contendo somente Java e a aplicacao compilada.
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# A aplicacao nao precisa executar como o usuario root do container.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar
COPY --chown=spring:spring docker/healthcheck.sh /app/healthcheck.sh
RUN sed -i 's/\r$//' /app/healthcheck.sh && chmod 0555 /app/healthcheck.sh

USER spring:spring

EXPOSE 8080

# Verifica a resposta HTTP protegida, sem criar contas/consultar provedores.
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 CMD ["bash", "/app/healthcheck.sh"]

ENTRYPOINT ["java", "-jar", "app.jar"]

# Reproducible build-from-source of the ACMEsuite (Spring Boot 4.1, Java 25).
# Stage 1 builds the fat jar (-DskipTests; tests run in CI). Stage 2 = slim JRE, non-root.
# Build context = repo root:  docker build -f deploy/backend.Dockerfile .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system acme \
 && useradd --system --gid acme --no-create-home acme
COPY --from=build /build/target/acmesuite-0.1.0-SNAPSHOT.jar /app/app.jar
USER acme
EXPOSE 8080
ENV JAVA_OPTS="-Xmx768m"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]

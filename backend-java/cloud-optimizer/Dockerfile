# =============================================================
# Stage 1 — BUILD
# Usa a imagem completa do JDK + Maven para compilar o projeto.
# Esta imagem (~500MB) nunca vai para produção.
# =============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copia o pom.xml ANTES do código-fonte.
# Motivo: o Docker cacheia cada instrução como uma "layer".
# Se só o código mudou (não as dependências), o Maven não
# precisa baixar tudo de novo — reutiliza a layer de cache.
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Baixa as dependências em uma layer separada (aproveitamento de cache)
RUN ./mvnw dependency:go-offline -q

# Agora copia o código-fonte e compila
COPY src src
RUN ./mvnw clean package -DskipTests -q
# -DskipTests: os testes já rodaram no CI antes de buildar a imagem


# =============================================================
# Stage 2 — RUNTIME
# Usa apenas o JRE (sem compilador, sem Maven) — imagem final
# é ~80MB vs ~500MB do JDK. Nunca exponha ferramentas de build
# em imagem de produção.
# =============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Cria usuário não-root para rodar a aplicação.
# Rodar como root em container é má prática de segurança.
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel

WORKDIR /app

# Copia APENAS o JAR do stage anterior — nada mais vai para a imagem final
COPY --from=builder /build/target/*.jar app.jar

# Muda a propriedade do arquivo para o usuário não-root
RUN chown sentinel:sentinel app.jar

USER sentinel

EXPOSE 8080

# Perfil injetado via docker-compose ou variável de ambiente
# Pode ser sobrescrito: docker run -e SPRING_PROFILES_ACTIVE=prod ...
ENTRYPOINT ["java", "-jar", "app.jar"]

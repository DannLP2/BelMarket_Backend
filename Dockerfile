# Etapa 1: Construcción
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto del backend
EXPOSE 8080

# Comando para ejecutar la aplicación usando el perfil de producción por defecto en Docker
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

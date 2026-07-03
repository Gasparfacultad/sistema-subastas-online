# Etapa 1: Compilación (Build)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el JAR omitiendo los tests
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el JAR generado desde la etapa de build
COPY --from=build /app/target/sistema-subastas-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]

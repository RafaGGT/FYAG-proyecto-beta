# ===== Etapa 1: Build =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml para cachear las dependencias
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== Etapa 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario no-root: nunca correr la app como root dentro del contenedor
RUN addgroup -S spring && adduser -S spring -G spring

# Copiamos solo el JAR ya compilado (nada de código fuente ni Maven en la imagen final)
COPY --from=build /app/target/*.jar app.jar

# Cambiamos el dueño de los archivos y de usuario activo
RUN chown spring:spring app.jar
USER spring

EXPOSE 8082

# Nada de secrets aqui: todo llega por variables de entorno en tiempo de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]
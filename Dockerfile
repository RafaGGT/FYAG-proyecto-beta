# ===== Etapa 1: Build =====
# Utiliza la imagen oficial de Maven con Eclipse Temurin JDK 17 como entorno de construcción (etapa "build")
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero solo el archivo pom.xml y la estructura de Maven para cachear las dependencias e incrementar velocidad
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
# Descarga todas las dependencias offline para aprovechar la caché de Docker si el pom.xml no cambia
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente restante y compilamos el proyecto empaquetándolo
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== Etapa 2: Runtime =====
# Utiliza una imagen ligera de Java Runtime (JRE 17 con Alpine Linux) para la ejecución final en producción
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crea un grupo y un usuario sin privilegios (no-root) por seguridad, para nunca correr la app como root dentro del contenedor
RUN addgroup -S spring && adduser -S spring -G spring

# Copiamos únicamente el archivo JAR compilado desde la etapa anterior (descartando código fuente y Maven de la imagen final)
COPY --from=build /app/target/*.jar app.jar

# Cambiamos el dueño de los archivos al usuario no-root creado y establecemos dicho usuario como activo
RUN chown spring:spring app.jar
USER spring

# Documenta el puerto en el que la aplicación escuchará peticiones (coincide con application.properties o docker-compose)
EXPOSE 8082

# Ejecuta la aplicación Java; los secretos e información sensible llegan mediante variables de entorno en tiempo de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]
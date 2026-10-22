FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -DskipTests package
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar target/*.jar"]

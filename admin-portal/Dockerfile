# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests -pl admin-portal -am clean package || mvn -q -DskipTests -pl admin-portal -am clean package
# Resulting jar:
# admin-portal/target/admin-portal-*.jar

# ---- Run stage ----
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-Xms256m -Xmx512m"
WORKDIR /app
COPY --from=build /app/admin-portal/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

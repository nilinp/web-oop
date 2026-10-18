FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN mkdir out && javac -encoding UTF-8 -cp "*" -d out $(find . -name "*.java" ! -name "TestRunner.java")

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out out
COPY --from=build /app/sqlite-jdbc-3.53.4.0.jar .
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "out:sqlite-jdbc-3.53.4.0.jar", "library.Main"]

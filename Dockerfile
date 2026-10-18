# ---------- ขั้นที่ 1: คอมไพล์ ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY lib lib
COPY src src
RUN mkdir out && javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src/library/Main.java

# ---------- ขั้นที่ 2: รัน ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out out
COPY lib lib
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "out:lib/*", "library.Main"]

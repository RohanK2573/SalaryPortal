FROM node:22-alpine AS frontend-build
WORKDIR /ui
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
COPY --from=frontend-build /ui/dist ./frontend/dist
RUN mvn -B test package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/salary-management-*.jar /app/app.jar
USER 10001:10001
COPY --chown=10001:10001 deploy/render-entrypoint.sh /app/render-entrypoint.sh
RUN chmod 0555 /app/render-entrypoint.sh
EXPOSE 10000
ENTRYPOINT ["/app/render-entrypoint.sh"]

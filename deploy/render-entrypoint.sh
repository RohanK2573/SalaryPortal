#!/bin/sh
set -eu

# Render provides PostgreSQL URLs with the postgresql:// scheme. The JDBC
# driver expects jdbc:postgresql://. Local Compose already supplies a JDBC URL.
case "${DATABASE_URL:-}" in
  postgresql://*) export DATABASE_URL="jdbc:${DATABASE_URL}" ;;
esac

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar

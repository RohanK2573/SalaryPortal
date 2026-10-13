#!/bin/sh
set -eu

# Render's URL contains userinfo (username/password), but this app supplies
# credentials separately through DATABASE_USERNAME and DATABASE_PASSWORD.
# Accept either Render's URL or an already JDBC-prefixed URL, then remove any
# embedded userinfo before giving the JDBC driver its host/database URL.
case "${DATABASE_URL:-}" in
  postgresql://*)
    database_url=${DATABASE_URL#postgresql://}
    ;;
  jdbc:postgresql://*)
    database_url=${DATABASE_URL#jdbc:postgresql://}
    ;;
  *) database_url=
    ;;
esac

if [ -n "$database_url" ]; then
  case "$database_url" in
    *@*) database_url=${database_url#*@} ;;
  esac
  export DATABASE_URL="jdbc:postgresql://${database_url}"
fi

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar

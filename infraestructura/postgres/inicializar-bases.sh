#!/bin/sh
set -eu

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=autenticacion_password="$AUTENTICACION_DB_PASSWORD" \
  --set=agenda_password="$AGENDA_DB_PASSWORD" <<-'EOSQL'
CREATE USER autenticacion WITH PASSWORD :'autenticacion_password';
CREATE USER agenda WITH PASSWORD :'agenda_password';
CREATE DATABASE autenticacion OWNER autenticacion;
CREATE DATABASE agenda_familiar OWNER agenda;
REVOKE ALL ON DATABASE autenticacion FROM PUBLIC;
REVOKE ALL ON DATABASE agenda_familiar FROM PUBLIC;
EOSQL

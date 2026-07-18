#!/usr/bin/env bash
set -Eeuo pipefail

RAIZ=/srv/agenda-familiar

printf 'CURRENT='
readlink -f "$RAIZ/current"

printf '\nCONTENEDORES\n'
docker ps --format '{{.Names}}|{{.Status}}'

printf '\nSALUD_PUBLICA\n'
curl -fsS https://www.obusystem.com/api/v1/actuator/health
printf '\n'
curl -fsS https://www.obusystem.com/api/v1/autenticacion/actuator/health

printf '\nMIGRACIONES_AGENDA\n'
docker exec agenda_familiar-postgres-1 \
    psql -U agenda -d agenda_familiar -At \
    -c 'select version,description,success from flyway_schema_history order by installed_rank;'

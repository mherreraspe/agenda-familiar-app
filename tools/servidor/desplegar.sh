#!/bin/bash
set -Eeuo pipefail

if [ "$#" -ne 3 ]; then
  echo "Uso: $0 <raiz> <archivo.tar.gz> <commit>" >&2
  exit 2
fi

RAIZ=$1
ARCHIVO=$2
COMMIT=$3
ACTUAL=$(readlink -f "$RAIZ/current")
MARCA=$(date -u +%Y%m%dT%H%M%SZ)
RESPALDO="$RAIZ/backups/predeploy/pre-${COMMIT:0:7}-$MARCA"
VARIABLES="/tmp/agenda-deploy-$MARCA.env"

mkdir -p "$RESPALDO"
docker exec agenda_familiar-postgres-1 pg_dump -U postgres -Fc -d agenda_familiar >"$RESPALDO/agenda_familiar.dump"
docker exec agenda_familiar-postgres-1 pg_dump -U postgres -Fc -d autenticacion >"$RESPALDO/autenticacion.dump"
ARTEFACTOS=("$RESPALDO/agenda_familiar.dump" "$RESPALDO/autenticacion.dump")
if docker volume inspect agenda_familiar_archivos_datos >/dev/null 2>&1; then
  PUNTO_ARCHIVOS=$(docker volume inspect -f '{{.Mountpoint}}' agenda_familiar_archivos_datos)
  sudo tar -czf "$RESPALDO/archivos-cifrados.tar.gz" -C "$PUNTO_ARCHIVOS" .
  ARTEFACTOS+=("$RESPALDO/archivos-cifrados.tar.gz")
fi
sha256sum "${ARTEFACTOS[@]}" >"$RESPALDO/SHA256SUMS"
test -s "$RESPALDO/agenda_familiar.dump"
test -s "$RESPALDO/autenticacion.dump"

cp "$ACTUAL/infraestructura/.env" "$VARIABLES"
chmod 600 "$VARIABLES"
bash "$ACTUAL/infraestructura/desplegar-servidor.sh" "$ARCHIVO" "$VARIABLES"

echo "BACKUP_OK=$RESPALDO"

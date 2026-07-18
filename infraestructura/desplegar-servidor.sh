#!/bin/bash
set -Eeuo pipefail

if [ "$#" -ne 2 ]; then
  echo "Uso: $0 <archivo.tar.gz> <archivo.env>" >&2
  exit 2
fi

ARCHIVO=$(readlink -f "$1")
VARIABLES=$(readlink -f "$2")
RAIZ=/srv/agenda-familiar
VERSION=$(date -u +%Y%m%dT%H%M%SZ)
DESTINO="$RAIZ/releases/$VERSION"
LOG="$RAIZ/despliegue-$VERSION.log"

sudo mkdir -p "$DESTINO"
sudo chown -R "$USER":"$USER" "$RAIZ"
tar -xzf "$ARCHIVO" -C "$DESTINO"
install -m 600 "$VARIABLES" "$DESTINO/infraestructura/.env"

cd "$DESTINO/infraestructura"
if ! docker compose --ansi never --env-file .env -p agenda_familiar up -d --build >"$LOG" 2>&1; then
  tail -n 120 "$LOG" >&2
  exit 1
fi

comprobar() {
  local url=$1
  local nombre=$2
  for _ in $(seq 1 60); do
    if curl --fail --silent --show-error --max-time 3 "$url" >/dev/null; then
      return 0
    fi
    sleep 2
  done
  echo "No respondió: $nombre ($url)" >&2
  docker compose --ansi never -p agenda_familiar ps >&2 || true
  docker compose --ansi never -p agenda_familiar logs --tail=100 >&2 || true
  return 1
}

comprobar http://127.0.0.1:8101/api/v1/autenticacion/actuator/health autenticacion
comprobar http://127.0.0.1:8102/api/v1/actuator/health agenda
comprobar http://127.0.0.1:8080/ frontend

RESPALDO="/etc/nginx/sites-available/default.backup-agenda-$VERSION"
sudo cp /etc/nginx/sites-available/default "$RESPALDO"
sudo install -m 644 "$DESTINO/infraestructura/nginx/agenda-familiar.conf" /etc/nginx/sites-available/default
if ! sudo nginx -t; then
  sudo cp "$RESPALDO" /etc/nginx/sites-available/default
  sudo nginx -t
  exit 1
fi
sudo systemctl reload nginx

ln -sfn "$DESTINO" "$RAIZ/current"
rm -f "$ARCHIVO" "$VARIABLES"

echo "DESPLIEGUE_OK=$VERSION"

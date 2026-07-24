#!/usr/bin/env sh
set -eu

raiz=${1:?Falta la raíz remota}
correo=${2:?Falta el correo administrativo}
enlace_id=${3:?Falta el identificador del enlace}
token_hash=${4:?Falta el hash del token}

case "$correo" in
  *"'"*|*.test) echo 'Correo administrativo rechazado' >&2; exit 2 ;;
esac

cd "$raiz/current/infraestructura"
docker compose -p agenda_familiar exec -T postgres sh -c \
  'PGPASSWORD="$AUTENTICACION_DB_PASSWORD" exec psql -h 127.0.0.1 -U autenticacion -d autenticacion -v ON_ERROR_STOP=1 "$@"' \
  sh --set="correo=$correo" --set="enlace_id=$enlace_id" --set="token_hash=$token_hash" <<'EOSQL'
BEGIN;

INSERT INTO usuarios (id_publico, correo, clave_hash, estado, rol_plataforma)
VALUES (gen_random_uuid(), :'correo', '!PENDIENTE_RESTABLECIMIENTO!', 'PENDIENTE', 'ADMINISTRADOR_PLATAFORMA')
ON CONFLICT (correo) DO UPDATE
SET rol_plataforma = 'ADMINISTRADOR_PLATAFORMA', actualizado_en = NOW(), version = usuarios.version + 1
RETURNING id AS interno_id, id_publico AS publico_id
\gset admin_

UPDATE sesiones_refresh
SET revocado_en = COALESCE(revocado_en, NOW())
WHERE usuario_id = :admin_interno_id;

UPDATE enlaces_acceso
SET revocado_en = NOW()
WHERE usuario_publico_id = :'admin_publico_id' AND consumido_en IS NULL AND revocado_en IS NULL;

INSERT INTO enlaces_acceso (id, tipo, actor_publico_id, clave_idempotencia, usuario_publico_id,
                            correo_normalizado, token_hash, expira_en)
VALUES (:'enlace_id', 'RESTABLECIMIENTO', :'admin_publico_id', 'operacion-' || :'enlace_id',
        :'admin_publico_id', :'correo', :'token_hash', NOW() + INTERVAL '30 minutes');

INSERT INTO auditoria_acceso (actor_publico_id, operacion, entidad_publica_id, resumen_seguro)
VALUES (:'admin_publico_id', 'RECUPERAR_ADMIN', :'enlace_id', 'Acceso administrativo preparado por operación segura');

COMMIT;

SELECT correo, estado, rol_plataforma FROM usuarios WHERE id = :admin_interno_id;
EOSQL

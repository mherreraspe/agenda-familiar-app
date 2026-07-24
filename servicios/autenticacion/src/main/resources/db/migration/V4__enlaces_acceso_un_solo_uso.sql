CREATE TABLE enlaces_acceso (
    id UUID PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL CHECK (tipo IN ('INVITACION', 'RESTABLECIMIENTO')),
    actor_publico_id UUID NOT NULL,
    clave_idempotencia VARCHAR(120) NOT NULL,
    usuario_publico_id UUID NOT NULL,
    familia_publica_id UUID,
    familia_nombre VARCHAR(120),
    correo_normalizado VARCHAR(320) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expira_en TIMESTAMPTZ NOT NULL,
    consumido_en TIMESTAMPTZ,
    revocado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (actor_publico_id, clave_idempotencia)
);

CREATE INDEX enlaces_acceso_familia_idx ON enlaces_acceso (familia_publica_id, creado_en DESC);
CREATE INDEX enlaces_acceso_usuario_idx ON enlaces_acceso (usuario_publico_id, creado_en DESC);

CREATE TABLE auditoria_acceso (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_publico_id UUID,
    operacion VARCHAR(40) NOT NULL,
    entidad_publica_id UUID NOT NULL,
    resumen_seguro VARCHAR(200) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX auditoria_acceso_reciente_idx ON auditoria_acceso (creado_en DESC);

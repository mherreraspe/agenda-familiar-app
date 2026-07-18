CREATE TABLE sesiones_refresh (
    id UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    csrf_hash CHAR(64) NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    revocado_en TIMESTAMPTZ,
    reemplazado_por UUID REFERENCES sesiones_refresh(id),
    ultimo_uso_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sesiones_refresh_usuario_activas
    ON sesiones_refresh (usuario_id, expira_en)
    WHERE revocado_en IS NULL;

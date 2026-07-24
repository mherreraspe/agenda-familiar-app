ALTER TABLE tareas ADD COLUMN avisar BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE eventos
    ADD COLUMN avisar_24h BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN avisar_1h BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE preferencias_notificacion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    usuario_publico_id UUID NOT NULL,
    tareas BOOLEAN NOT NULL DEFAULT TRUE,
    eventos BOOLEAN NOT NULL DEFAULT TRUE,
    salud BOOLEAN NOT NULL DEFAULT TRUE,
    botiquin BOOLEAN NOT NULL DEFAULT TRUE,
    silencio_desde TIME NOT NULL DEFAULT TIME '22:00',
    silencio_hasta TIME NOT NULL DEFAULT TIME '07:00',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, usuario_publico_id)
);

CREATE TABLE notificaciones (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    usuario_publico_id UUID NOT NULL,
    tipo VARCHAR(24) NOT NULL CHECK (tipo IN ('TAREA', 'EVENTO', 'SALUD', 'BOTIQUIN', 'FAMILIA', 'SISTEMA')),
    entidad VARCHAR(30),
    entidad_publica_id UUID,
    clave_deduplicacion VARCHAR(180) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    detalle VARCHAR(300),
    destino VARCHAR(180) NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    leida_en TIMESTAMPTZ,
    enviada_push_en TIMESTAMPTZ,
    UNIQUE (familia_id, usuario_publico_id, clave_deduplicacion)
);

CREATE TABLE suscripciones_push (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    usuario_publico_id UUID NOT NULL,
    endpoint TEXT NOT NULL,
    endpoint_hash CHAR(64) NOT NULL,
    clave_p256dh TEXT NOT NULL,
    clave_auth TEXT NOT NULL,
    dispositivo VARCHAR(100) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ultimo_exito_en TIMESTAMPTZ,
    ultimo_error_en TIMESTAMPTZ,
    UNIQUE (familia_id, usuario_publico_id, endpoint_hash)
);

CREATE INDEX notificaciones_usuario_fecha_idx
    ON notificaciones (familia_id, usuario_publico_id, creada_en DESC);
CREATE INDEX notificaciones_usuario_pendientes_idx
    ON notificaciones (familia_id, usuario_publico_id, creada_en DESC)
    WHERE leida_en IS NULL;
CREATE INDEX suscripciones_push_activas_idx
    ON suscripciones_push (familia_id, usuario_publico_id)
    WHERE activa;

ALTER TABLE preferencias_notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE preferencias_notificacion FORCE ROW LEVEL SECURITY;
ALTER TABLE notificaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE notificaciones FORCE ROW LEVEL SECURITY;
ALTER TABLE suscripciones_push ENABLE ROW LEVEL SECURITY;
ALTER TABLE suscripciones_push FORCE ROW LEVEL SECURITY;

CREATE POLICY preferencias_notificacion_por_familia ON preferencias_notificacion
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
CREATE POLICY notificaciones_por_familia ON notificaciones
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
CREATE POLICY suscripciones_push_por_familia ON suscripciones_push
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

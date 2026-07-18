ALTER TABLE ocurrencias_tratamiento
    DROP CONSTRAINT ocurrencias_tratamiento_estado_check;

ALTER TABLE ocurrencias_tratamiento
    ADD CONSTRAINT ocurrencias_tratamiento_estado_check
    CHECK (estado IN ('PENDIENTE', 'TOMADA', 'OMITIDA', 'POSPUESTA', 'REPROGRAMADA', 'CANCELADA'));

ALTER TABLE ocurrencias_tratamiento
    ADD COLUMN ocurrencia_origen_id BIGINT REFERENCES ocurrencias_tratamiento(id);

ALTER TABLE tratamientos
    ADD COLUMN responsable_alternativo_perfil_id BIGINT REFERENCES perfiles(id);

ALTER TABLE horarios_tratamiento
    ADD COLUMN intervalo_horas SMALLINT CHECK (intervalo_horas BETWEEN 1 AND 168);

CREATE INDEX ocurrencias_origen_idx
    ON ocurrencias_tratamiento (familia_id, ocurrencia_origen_id)
    WHERE ocurrencia_origen_id IS NOT NULL;

CREATE TABLE acciones_tratamiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    tratamiento_id BIGINT NOT NULL REFERENCES tratamientos(id),
    clave_idempotencia VARCHAR(120) NOT NULL,
    accion VARCHAR(30) NOT NULL CHECK (accion IN ('CERRAR')),
    actor_publico_id UUID NOT NULL,
    motivo VARCHAR(500),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, clave_idempotencia)
);

ALTER TABLE acciones_tratamiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE acciones_tratamiento FORCE ROW LEVEL SECURITY;
CREATE POLICY acciones_tratamiento_por_familia ON acciones_tratamiento
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

ALTER TABLE tratamientos ALTER COLUMN medicamento_id DROP NOT NULL;
ALTER TABLE tratamientos ALTER COLUMN indicacion DROP NOT NULL;
ALTER TABLE tratamientos ALTER COLUMN dosis_indicada DROP NOT NULL;
ALTER TABLE tratamientos ALTER COLUMN frecuencia DROP NOT NULL;
ALTER TABLE tratamientos ALTER COLUMN fecha_inicio SET DEFAULT CURRENT_DATE;
ALTER TABLE tratamientos ADD COLUMN nombre_libre VARCHAR(180);
ALTER TABLE tratamientos ADD COLUMN responsable_perfil_id BIGINT REFERENCES perfiles(id);
ALTER TABLE tratamientos ADD COLUMN cantidad_receta VARCHAR(300);

DO $$
DECLARE
    familia RECORD;
BEGIN
    FOR familia IN SELECT id FROM familias LOOP
        PERFORM set_config('agenda.familia_id', familia.id::TEXT, TRUE);
        UPDATE tratamientos t
        SET nombre_libre = m.nombre,
            responsable_perfil_id = t.perfil_id,
            cantidad_receta = NULLIF(t.dosis_indicada, '')
        FROM medicamentos m
        WHERE t.familia_id = familia.id
          AND m.familia_id = familia.id
          AND m.id = t.medicamento_id;
    END LOOP;
    PERFORM set_config('agenda.familia_id', '', TRUE);
END $$;

ALTER TABLE tratamientos ALTER COLUMN nombre_libre SET NOT NULL;
ALTER TABLE tratamientos ALTER COLUMN responsable_perfil_id SET NOT NULL;

ALTER TABLE eventos ALTER COLUMN perfil_id DROP NOT NULL;
ALTER TABLE eventos ALTER COLUMN tipo DROP NOT NULL;
ALTER TABLE eventos ADD COLUMN direccion VARCHAR(500);
ALTER TABLE eventos ADD COLUMN notas VARCHAR(1000);

CREATE TABLE horarios_tratamiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    tratamiento_id BIGINT NOT NULL REFERENCES tratamientos(id),
    hora_local TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (familia_id, tratamiento_id, hora_local)
);

CREATE TABLE ocurrencias_tratamiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    tratamiento_id BIGINT NOT NULL REFERENCES tratamientos(id),
    horario_id BIGINT REFERENCES horarios_tratamiento(id),
    programada_en TIMESTAMPTZ NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'TOMADA', 'OMITIDA', 'POSPUESTA', 'CANCELADA')),
    pospuesta_a TIMESTAMPTZ,
    resuelta_por UUID,
    resuelta_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (familia_id, tratamiento_id, programada_en)
);

CREATE TABLE acciones_ocurrencia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    ocurrencia_id BIGINT NOT NULL REFERENCES ocurrencias_tratamiento(id),
    clave_idempotencia VARCHAR(120) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    actor_publico_id UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, clave_idempotencia)
);

CREATE TABLE elementos_revision (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    origen VARCHAR(40) NOT NULL CHECK (origen IN ('OCURRENCIA', 'TRATAMIENTO', 'LOTE_MEDICAMENTO')),
    entidad_publica_id UUID NOT NULL,
    motivo VARCHAR(80) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'RESUELTO')),
    resuelto_por UUID,
    resuelto_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (familia_id, origen, entidad_publica_id, motivo)
);

CREATE TABLE acciones_revision (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    elemento_revision_id BIGINT NOT NULL REFERENCES elementos_revision(id),
    clave_idempotencia VARCHAR(120) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    actor_publico_id UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, clave_idempotencia)
);

CREATE INDEX ocurrencias_familia_programada_idx ON ocurrencias_tratamiento (familia_id, programada_en, estado);
CREATE INDEX revision_familia_estado_idx ON elementos_revision (familia_id, estado, creado_en);

ALTER TABLE horarios_tratamiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE horarios_tratamiento FORCE ROW LEVEL SECURITY;
CREATE POLICY horarios_por_familia ON horarios_tratamiento
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE ocurrencias_tratamiento ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocurrencias_tratamiento FORCE ROW LEVEL SECURITY;
CREATE POLICY ocurrencias_por_familia ON ocurrencias_tratamiento
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE acciones_ocurrencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE acciones_ocurrencia FORCE ROW LEVEL SECURITY;
CREATE POLICY acciones_ocurrencia_por_familia ON acciones_ocurrencia
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE elementos_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE elementos_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY revision_por_familia ON elementos_revision
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE acciones_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE acciones_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY acciones_revision_por_familia ON acciones_revision
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

SELECT set_config('agenda.familia_id', (SELECT id::TEXT FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001'), FALSE);

INSERT INTO horarios_tratamiento (id_publico, familia_id, tratamiento_id, hora_local)
SELECT '0197f100-0000-7000-8000-000000000502', familia_id, id, TIME '08:00'
FROM tratamientos WHERE id_publico = '0197f100-0000-7000-8000-000000000501';

SELECT set_config('agenda.familia_id', '', FALSE);

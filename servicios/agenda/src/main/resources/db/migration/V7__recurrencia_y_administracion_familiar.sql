CREATE TABLE recurrencias_agenda (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    entidad VARCHAR(20) NOT NULL CHECK (entidad IN ('TAREA', 'EVENTO')),
    frecuencia VARCHAR(20) NOT NULL CHECK (frecuencia IN ('DIARIA', 'SEMANAL', 'MENSUAL')),
    intervalo SMALLINT NOT NULL DEFAULT 1 CHECK (intervalo BETWEEN 1 AND 30),
    hasta TIMESTAMPTZ NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE tareas
    ADD COLUMN recurrencia_id BIGINT REFERENCES recurrencias_agenda(id),
    ADD COLUMN numero_ocurrencia INTEGER,
    ADD COLUMN tarea_origen_id BIGINT REFERENCES tareas(id);

ALTER TABLE eventos
    ADD COLUMN recurrencia_id BIGINT REFERENCES recurrencias_agenda(id),
    ADD COLUMN numero_ocurrencia INTEGER,
    ADD COLUMN evento_origen_id BIGINT REFERENCES eventos(id);

CREATE UNIQUE INDEX tareas_recurrencia_numero_idx
    ON tareas (familia_id, recurrencia_id, numero_ocurrencia)
    WHERE recurrencia_id IS NOT NULL AND numero_ocurrencia IS NOT NULL;
CREATE UNIQUE INDEX eventos_recurrencia_numero_idx
    ON eventos (familia_id, recurrencia_id, numero_ocurrencia)
    WHERE recurrencia_id IS NOT NULL AND numero_ocurrencia IS NOT NULL;

CREATE TABLE acciones_agenda (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    clave_idempotencia VARCHAR(120) NOT NULL,
    entidad VARCHAR(20) NOT NULL CHECK (entidad IN ('TAREA', 'EVENTO')),
    entidad_publica_id UUID NOT NULL,
    accion VARCHAR(30) NOT NULL CHECK (accion IN ('COMPLETAR', 'OMITIR', 'REPROGRAMAR')),
    actor_publico_id UUID NOT NULL,
    fecha_anterior TIMESTAMPTZ,
    fecha_nueva TIMESTAMPTZ,
    resultado_publico_id UUID,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, clave_idempotencia)
);

ALTER TABLE perfiles
    ADD COLUMN usuario_publico_id UUID,
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
DECLARE
    familia RECORD;
BEGIN
    FOR familia IN SELECT id FROM familias LOOP
        PERFORM set_config('agenda.familia_id', familia.id::TEXT, TRUE);
        UPDATE perfiles p
        SET usuario_publico_id = m.usuario_publico_id
        FROM miembros_familia m
        WHERE p.familia_id = familia.id
          AND m.familia_id = familia.id
          AND m.perfil_id = p.id;
    END LOOP;
    PERFORM set_config('agenda.familia_id', '', TRUE);
END $$;

CREATE UNIQUE INDEX perfiles_usuario_familia_idx
    ON perfiles (familia_id, usuario_publico_id)
    WHERE usuario_publico_id IS NOT NULL;

ALTER TABLE recurrencias_agenda ENABLE ROW LEVEL SECURITY;
ALTER TABLE recurrencias_agenda FORCE ROW LEVEL SECURITY;
CREATE POLICY recurrencias_agenda_por_familia ON recurrencias_agenda
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

ALTER TABLE acciones_agenda ENABLE ROW LEVEL SECURITY;
ALTER TABLE acciones_agenda FORCE ROW LEVEL SECURITY;
CREATE POLICY acciones_agenda_por_familia ON acciones_agenda
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

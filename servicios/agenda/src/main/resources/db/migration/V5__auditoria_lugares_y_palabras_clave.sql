CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE miembros_familia ADD COLUMN perfil_id BIGINT REFERENCES perfiles(id);
CREATE UNIQUE INDEX miembros_familia_perfil_uidx
    ON miembros_familia (familia_id, perfil_id) WHERE perfil_id IS NOT NULL;

DO $$
DECLARE
    familia RECORD;
BEGIN
    FOR familia IN SELECT id FROM familias LOOP
        PERFORM set_config('agenda.familia_id', familia.id::TEXT, TRUE);
        UPDATE miembros_familia m
        SET perfil_id = p.id
        FROM perfiles p
        WHERE m.familia_id = familia.id
          AND p.familia_id = familia.id
          AND ((m.usuario_publico_id = '0197f100-0000-7000-8000-000000000101' AND p.id_publico = '0197f100-0000-7000-8000-000000000201')
            OR (m.usuario_publico_id = '0197f100-0000-7000-8000-000000000102' AND p.id_publico = '0197f100-0000-7000-8000-000000000202'));
    END LOOP;
    PERFORM set_config('agenda.familia_id', '', TRUE);
END $$;

CREATE TABLE lugares_familia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    nombre VARCHAR(300) NOT NULL,
    nombre_normalizado VARCHAR(300) NOT NULL,
    direccion VARCHAR(500),
    ultima_utilizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    frecuencia_uso INTEGER NOT NULL DEFAULT 1 CHECK (frecuencia_uso > 0),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE NULLS NOT DISTINCT (familia_id, nombre_normalizado, direccion)
);

CREATE TABLE palabras_clave (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    palabra VARCHAR(80) NOT NULL,
    origen VARCHAR(20) NOT NULL CHECK (origen IN ('IA', 'REGLA', 'USUARIO')),
    entidad VARCHAR(40) NOT NULL CHECK (entidad IN ('EVENTO', 'TRATAMIENTO', 'TAREA')),
    entidad_publica_id UUID NOT NULL,
    version_extractor VARCHAR(40) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, entidad, entidad_publica_id, palabra, version_extractor)
);

ALTER TABLE eventos ADD COLUMN lugar_guardado_id BIGINT REFERENCES lugares_familia(id);

CREATE INDEX lugares_familia_nombre_trgm_idx ON lugares_familia USING GIN (nombre_normalizado public.gin_trgm_ops);
CREATE INDEX lugares_familia_uso_idx ON lugares_familia (familia_id, ultima_utilizacion DESC, frecuencia_uso DESC);
CREATE INDEX palabras_clave_busqueda_idx ON palabras_clave (familia_id, palabra, entidad);

ALTER TABLE lugares_familia ENABLE ROW LEVEL SECURITY;
ALTER TABLE lugares_familia FORCE ROW LEVEL SECURITY;
CREATE POLICY lugares_por_familia ON lugares_familia
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

ALTER TABLE palabras_clave ENABLE ROW LEVEL SECURITY;
ALTER TABLE palabras_clave FORCE ROW LEVEL SECURITY;
CREATE POLICY palabras_por_familia ON palabras_clave
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

DO $$
DECLARE
    familia RECORD;
BEGIN
    FOR familia IN SELECT id FROM familias LOOP
        PERFORM set_config('agenda.familia_id', familia.id::TEXT, TRUE);
        INSERT INTO lugares_familia (id_publico, familia_id, nombre, nombre_normalizado, direccion, ultima_utilizacion, frecuencia_uso)
        SELECT uuidv7(), e.familia_id, MIN(e.lugar), LOWER(TRIM(e.lugar)), MIN(e.direccion), MAX(e.inicio_en), COUNT(*)::INTEGER
        FROM eventos e
        WHERE e.familia_id = familia.id AND NULLIF(TRIM(e.lugar), '') IS NOT NULL
        GROUP BY e.familia_id, LOWER(TRIM(e.lugar)), e.direccion
        ON CONFLICT (familia_id, nombre_normalizado, direccion) DO NOTHING;
        UPDATE eventos e
        SET lugar_guardado_id = l.id
        FROM lugares_familia l
        WHERE e.familia_id = familia.id
          AND l.familia_id = familia.id
          AND l.nombre_normalizado = LOWER(TRIM(e.lugar))
          AND l.direccion IS NOT DISTINCT FROM e.direccion;
    END LOOP;
    PERFORM set_config('agenda.familia_id', '', TRUE);
END $$;

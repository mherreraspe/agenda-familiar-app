CREATE TABLE ubicaciones_objetos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    padre_id BIGINT,
    nombre VARCHAR(120) NOT NULL,
    nombre_normalizado VARCHAR(120) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE NULLS NOT DISTINCT (familia_id, padre_id, nombre_normalizado),
    UNIQUE (familia_id, id),
    FOREIGN KEY (familia_id, padre_id) REFERENCES ubicaciones_objetos(familia_id, id)
);

CREATE TABLE objetos_familia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    ubicacion_id BIGINT NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    nombre_normalizado VARCHAR(180) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    categoria_normalizada VARCHAR(80) NOT NULL,
    notas VARCHAR(500),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (familia_id, ubicacion_id) REFERENCES ubicaciones_objetos(familia_id, id)
);

CREATE INDEX ubicaciones_objetos_nombre_trgm_idx ON ubicaciones_objetos USING GIN (nombre_normalizado public.gin_trgm_ops);
CREATE INDEX ubicaciones_objetos_familia_padre_idx ON ubicaciones_objetos (familia_id, padre_id, nombre);
CREATE INDEX objetos_familia_nombre_trgm_idx ON objetos_familia USING GIN (nombre_normalizado public.gin_trgm_ops);
CREATE INDEX objetos_familia_categoria_trgm_idx ON objetos_familia USING GIN (categoria_normalizada public.gin_trgm_ops);
CREATE INDEX objetos_familia_recientes_idx ON objetos_familia (familia_id, actualizado_en DESC);

CREATE TABLE idempotencia_objetos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    clave VARCHAR(120) NOT NULL,
    objeto_publico_id UUID NOT NULL REFERENCES objetos_familia(id_publico),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, clave)
);

ALTER TABLE ubicaciones_objetos ENABLE ROW LEVEL SECURITY;
ALTER TABLE ubicaciones_objetos FORCE ROW LEVEL SECURITY;
CREATE POLICY ubicaciones_objetos_por_familia ON ubicaciones_objetos
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

ALTER TABLE objetos_familia ENABLE ROW LEVEL SECURITY;
ALTER TABLE objetos_familia FORCE ROW LEVEL SECURITY;
CREATE POLICY objetos_por_familia ON objetos_familia
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

ALTER TABLE idempotencia_objetos ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotencia_objetos FORCE ROW LEVEL SECURITY;
CREATE POLICY idempotencia_objetos_por_familia ON idempotencia_objetos
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

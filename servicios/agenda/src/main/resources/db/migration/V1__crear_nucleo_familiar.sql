CREATE TABLE familias (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    zona_horaria VARCHAR(60) NOT NULL DEFAULT 'America/Lima',
    cuota_bytes BIGINT NOT NULL DEFAULT 1073741824 CHECK (cuota_bytes >= 0),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE miembros_familia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    usuario_publico_id UUID NOT NULL,
    rol VARCHAR(40) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (familia_id, usuario_publico_id)
);

CREATE TABLE perfiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    nombre_visible VARCHAR(120) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    color VARCHAR(20),
    relacion VARCHAR(80),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE miembros_familia ENABLE ROW LEVEL SECURITY;
ALTER TABLE perfiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE miembros_familia FORCE ROW LEVEL SECURITY;
ALTER TABLE perfiles FORCE ROW LEVEL SECURITY;

CREATE POLICY miembros_por_familia ON miembros_familia
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

CREATE POLICY perfiles_por_familia ON perfiles
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

CREATE TABLE idempotencia_familias (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave VARCHAR(120) NOT NULL,
    familia_publica_id UUID NOT NULL REFERENCES familias(id_publico),
    actor_publico_id UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (actor_publico_id, clave)
);

CREATE TABLE auditoria_plataforma (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_publico_id UUID NOT NULL,
    operacion VARCHAR(40) NOT NULL,
    entidad VARCHAR(40) NOT NULL,
    entidad_publica_id UUID NOT NULL,
    resumen_seguro VARCHAR(240) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX auditoria_plataforma_reciente_idx ON auditoria_plataforma (creado_en DESC);

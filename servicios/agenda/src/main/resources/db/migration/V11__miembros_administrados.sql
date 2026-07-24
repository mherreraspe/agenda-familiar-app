CREATE TABLE idempotencia_miembros_plataforma (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_publico_id UUID NOT NULL,
    clave VARCHAR(120) NOT NULL,
    familia_publica_id UUID NOT NULL REFERENCES familias(id_publico),
    perfil_publico_id UUID NOT NULL REFERENCES perfiles(id_publico),
    usuario_publico_id UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (actor_publico_id, clave)
);

CREATE INDEX idempotencia_miembros_familia_idx
    ON idempotencia_miembros_plataforma (familia_publica_id, creado_en DESC);

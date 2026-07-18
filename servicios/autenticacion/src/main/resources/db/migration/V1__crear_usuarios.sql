CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    correo VARCHAR(320) NOT NULL UNIQUE,
    clave_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE dispositivos_confiables (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    huella_hash VARCHAR(128) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ultimo_uso_en TIMESTAMPTZ,
    revocado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, huella_hash)
);

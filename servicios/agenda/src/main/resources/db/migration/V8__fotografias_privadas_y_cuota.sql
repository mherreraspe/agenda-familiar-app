CREATE TABLE archivos_familia (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    tratamiento_id BIGINT NOT NULL REFERENCES tratamientos(id),
    tipo VARCHAR(30) NOT NULL DEFAULT 'RECETA' CHECK (tipo IN ('RECETA')),
    mime_type VARCHAR(80) NOT NULL,
    ancho INTEGER NOT NULL CHECK (ancho > 0),
    alto INTEGER NOT NULL CHECK (alto > 0),
    bytes_fuente BIGINT NOT NULL CHECK (bytes_fuente > 0),
    bytes_almacenados BIGINT NOT NULL CHECK (bytes_almacenados > 0),
    sha256 CHAR(64) NOT NULL,
    ruta_original VARCHAR(300) NOT NULL,
    ruta_miniatura VARCHAR(300) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'BORRADO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    eliminado_en TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX archivos_receta_activa_idx
    ON archivos_familia (familia_id, tratamiento_id)
    WHERE estado = 'ACTIVO';
CREATE INDEX archivos_cuota_idx
    ON archivos_familia (familia_id, estado);
CREATE INDEX archivos_sha_familia_idx
    ON archivos_familia (familia_id, sha256)
    WHERE estado = 'ACTIVO';

ALTER TABLE archivos_familia ENABLE ROW LEVEL SECURITY;
ALTER TABLE archivos_familia FORCE ROW LEVEL SECURITY;
CREATE POLICY archivos_familia_por_familia ON archivos_familia
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

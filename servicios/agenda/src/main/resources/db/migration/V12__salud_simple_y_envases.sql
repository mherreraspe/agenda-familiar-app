ALTER TABLE tratamientos
    ADD COLUMN grupo_publico_id UUID,
    ADD COLUMN nombre_medicamento VARCHAR(180),
    ADD COLUMN aplicacion VARCHAR(300);

UPDATE tratamientos SET grupo_publico_id = id_publico WHERE grupo_publico_id IS NULL;
ALTER TABLE tratamientos ALTER COLUMN grupo_publico_id SET NOT NULL;

ALTER TABLE lotes_medicamento
    ADD COLUMN estado_envase VARCHAR(20) NOT NULL DEFAULT 'SIN_ABRIR'
        CHECK (estado_envase IN ('SIN_ABRIR', 'ABIERTO')),
    ADD COLUMN abierto_en DATE,
    ADD COLUMN duracion_abierto_dias SMALLINT CHECK (duracion_abierto_dias BETWEEN 1 AND 3650),
    ADD COLUMN avisar_vencimiento BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN anticipacion_vencimiento_dias SMALLINT NOT NULL DEFAULT 7
        CHECK (anticipacion_vencimiento_dias BETWEEN 0 AND 365),
    ADD COLUMN avisar_apertura BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN anticipacion_apertura_dias SMALLINT NOT NULL DEFAULT 3
        CHECK (anticipacion_apertura_dias BETWEEN 0 AND 365),
    ADD CONSTRAINT lotes_apertura_coherente_check
        CHECK (estado_envase <> 'ABIERTO' OR abierto_en IS NOT NULL);

CREATE TABLE idempotencia_salud (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    clave VARCHAR(120) NOT NULL,
    operacion VARCHAR(40) NOT NULL,
    resultado_principal UUID NOT NULL,
    resultado_ids TEXT NOT NULL DEFAULT '',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (familia_id, clave)
);

CREATE INDEX tratamientos_familia_grupo_idx
    ON tratamientos (familia_id, grupo_publico_id);
CREATE INDEX ocurrencias_pendientes_familia_programada_idx
    ON ocurrencias_tratamiento (familia_id, programada_en)
    WHERE estado = 'PENDIENTE';
CREATE INDEX lotes_familia_apertura_limite_idx
    ON lotes_medicamento (familia_id, (abierto_en + duracion_abierto_dias::INTEGER))
    WHERE estado_envase = 'ABIERTO'
      AND duracion_abierto_dias IS NOT NULL
      AND estado NOT IN ('AGOTADO', 'DESCARTADO');
CREATE INDEX lotes_familia_estado_envase_idx
    ON lotes_medicamento (familia_id, estado_envase, actualizado_en DESC)
    WHERE estado NOT IN ('AGOTADO', 'DESCARTADO');

ALTER TABLE idempotencia_salud ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotencia_salud FORCE ROW LEVEL SECURITY;
CREATE POLICY idempotencia_salud_por_familia ON idempotencia_salud
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT)
    WITH CHECK (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

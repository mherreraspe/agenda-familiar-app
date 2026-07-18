CREATE TABLE medicamentos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    nombre VARCHAR(180) NOT NULL,
    presentacion VARCHAR(120),
    concentracion VARCHAR(120),
    notas VARCHAR(1000),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE lotes_medicamento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    medicamento_id BIGINT NOT NULL REFERENCES medicamentos(id),
    cantidad NUMERIC(12,3) NOT NULL CHECK (cantidad >= 0),
    unidad VARCHAR(40) NOT NULL,
    fecha_vencimiento DATE,
    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE tratamientos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    perfil_id BIGINT NOT NULL REFERENCES perfiles(id),
    medicamento_id BIGINT NOT NULL REFERENCES medicamentos(id),
    indicacion VARCHAR(1000) NOT NULL,
    dosis_indicada VARCHAR(300) NOT NULL,
    frecuencia VARCHAR(300) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE TABLE eventos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    perfil_id BIGINT NOT NULL REFERENCES perfiles(id),
    titulo VARCHAR(180) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    lugar VARCHAR(300),
    inicio_en TIMESTAMPTZ NOT NULL,
    fin_en TIMESTAMPTZ,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (fin_en IS NULL OR fin_en >= inicio_en)
);

CREATE TABLE auditoria (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    actor_publico_id UUID NOT NULL,
    operacion VARCHAR(80) NOT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_publica_id UUID NOT NULL,
    resumen_seguro VARCHAR(500) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX medicamentos_familia_idx ON medicamentos (familia_id, nombre);
CREATE INDEX lotes_vencimiento_idx ON lotes_medicamento (familia_id, fecha_vencimiento);
CREATE INDEX tratamientos_familia_estado_idx ON tratamientos (familia_id, estado);
CREATE INDEX eventos_familia_inicio_idx ON eventos (familia_id, inicio_en);
CREATE INDEX auditoria_familia_fecha_idx ON auditoria (familia_id, creado_en DESC);

ALTER TABLE medicamentos ENABLE ROW LEVEL SECURITY;
ALTER TABLE medicamentos FORCE ROW LEVEL SECURITY;
CREATE POLICY medicamentos_por_familia ON medicamentos USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE lotes_medicamento ENABLE ROW LEVEL SECURITY;
ALTER TABLE lotes_medicamento FORCE ROW LEVEL SECURITY;
CREATE POLICY lotes_por_familia ON lotes_medicamento USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE tratamientos ENABLE ROW LEVEL SECURITY;
ALTER TABLE tratamientos FORCE ROW LEVEL SECURITY;
CREATE POLICY tratamientos_por_familia ON tratamientos USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE eventos ENABLE ROW LEVEL SECURITY;
ALTER TABLE eventos FORCE ROW LEVEL SECURITY;
CREATE POLICY eventos_por_familia ON eventos USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);
ALTER TABLE auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE auditoria FORCE ROW LEVEL SECURITY;
CREATE POLICY auditoria_por_familia ON auditoria USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

SELECT set_config('agenda.familia_id', (SELECT id::TEXT FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001'), FALSE);

INSERT INTO medicamentos (id_publico, familia_id, nombre, presentacion, concentracion, notas)
SELECT '0197f100-0000-7000-8000-000000000401', id, 'Medicamento de prueba', 'Tabletas', 'Según etiqueta', 'Datos de demostración; no constituyen indicación médica'
FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001';

INSERT INTO lotes_medicamento (id_publico, familia_id, medicamento_id, cantidad, unidad, fecha_vencimiento)
SELECT '0197f100-0000-7000-8000-000000000402', m.familia_id, m.id, 12, 'tabletas', CURRENT_DATE + 120
FROM medicamentos m WHERE m.id_publico = '0197f100-0000-7000-8000-000000000401';

INSERT INTO tratamientos (id_publico, familia_id, perfil_id, medicamento_id, indicacion, dosis_indicada, frecuencia, fecha_inicio, fecha_fin)
SELECT '0197f100-0000-7000-8000-000000000501', f.id, p.id, m.id,
       'Seguir exactamente la indicación registrada por la familia', 'Texto indicado por el usuario', 'Horario definido por la familia', CURRENT_DATE, CURRENT_DATE + 7
FROM familias f
JOIN perfiles p ON p.familia_id = f.id AND p.nombre_visible = 'Hijo'
JOIN medicamentos m ON m.familia_id = f.id
WHERE f.id_publico = '0197f100-0000-7000-8000-000000000001';

INSERT INTO eventos (id_publico, familia_id, perfil_id, titulo, tipo, lugar, inicio_en)
SELECT '0197f100-0000-7000-8000-000000000601', f.id, p.id, 'Control familiar de prueba', 'CITA_MEDICA', 'Lugar por confirmar', NOW() + INTERVAL '2 days'
FROM familias f
JOIN perfiles p ON p.familia_id = f.id AND p.nombre_visible = 'Hijo'
WHERE f.id_publico = '0197f100-0000-7000-8000-000000000001';

SELECT set_config('agenda.familia_id', '', FALSE);

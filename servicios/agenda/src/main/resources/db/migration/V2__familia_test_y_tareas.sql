INSERT INTO familias (id_publico, nombre, zona_horaria)
VALUES ('0197f100-0000-7000-8000-000000000001', 'familia_test', 'America/Lima');

SELECT set_config(
    'agenda.familia_id',
    (SELECT id::TEXT FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001'),
    FALSE
);

INSERT INTO miembros_familia (familia_id, usuario_publico_id, rol)
SELECT id, '0197f100-0000-7000-8000-000000000101', 'ADMINISTRADOR_FAMILIAR'
FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001';

INSERT INTO miembros_familia (familia_id, usuario_publico_id, rol)
SELECT id, '0197f100-0000-7000-8000-000000000102', 'ADULTO'
FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001';

INSERT INTO perfiles (id_publico, familia_id, nombre_visible, tipo, color, relacion)
SELECT '0197f100-0000-7000-8000-000000000201', id, 'Papá', 'ADULTO', '#315b4c', 'Papá'
FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001';

INSERT INTO perfiles (id_publico, familia_id, nombre_visible, tipo, color, relacion)
SELECT '0197f100-0000-7000-8000-000000000202', id, 'Mamá', 'ADULTO', '#a65f46', 'Mamá'
FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001';

INSERT INTO perfiles (id_publico, familia_id, nombre_visible, tipo, color, relacion)
SELECT '0197f100-0000-7000-8000-000000000203', id, 'Hijo', 'DEPENDIENTE', '#d1aa70', 'Hijo'
FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001';

CREATE TABLE tareas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_publico UUID NOT NULL UNIQUE,
    familia_id BIGINT NOT NULL REFERENCES familias(id),
    perfil_id BIGINT REFERENCES perfiles(id),
    titulo VARCHAR(180) NOT NULL,
    descripcion VARCHAR(1000),
    fecha_limite TIMESTAMPTZ NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX tareas_familia_fecha_idx ON tareas (familia_id, fecha_limite);
ALTER TABLE tareas ENABLE ROW LEVEL SECURITY;
ALTER TABLE tareas FORCE ROW LEVEL SECURITY;
CREATE POLICY tareas_por_familia ON tareas
    USING (familia_id = NULLIF(current_setting('agenda.familia_id', TRUE), '')::BIGINT);

INSERT INTO tareas (id_publico, familia_id, perfil_id, titulo, descripcion, fecha_limite)
SELECT '0197f100-0000-7000-8000-000000000301', f.id, p.id,
       'Revisar la agenda escolar', 'Primera tarea de familia_test', NOW() + INTERVAL '4 hours'
FROM familias f
JOIN perfiles p ON p.familia_id = f.id AND p.nombre_visible = 'Papá'
WHERE f.id_publico = '0197f100-0000-7000-8000-000000000001';

SELECT set_config('agenda.familia_id', '', FALSE);

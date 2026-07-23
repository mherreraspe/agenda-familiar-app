ALTER TABLE usuarios
    ADD COLUMN rol_plataforma VARCHAR(40) NOT NULL DEFAULT 'USUARIO'
    CHECK (rol_plataforma IN ('USUARIO', 'ADMINISTRADOR_PLATAFORMA'));

UPDATE usuarios
SET rol_plataforma = 'ADMINISTRADOR_PLATAFORMA', actualizado_en = NOW(), version = version + 1
WHERE id_publico = '0197f100-0000-7000-8000-000000000101';

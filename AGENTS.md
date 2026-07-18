# Continuidad operativa para agentes

Antes de ejecutar comandos o continuar un despliegue:

1. Leer únicamente `docs/00-RELEVO_RAPIDO.md` como contexto inicial.
2. Leer `docs/03-ESTADO_Y_CHECKLIST.md` solo para historia o auditoría de requisitos, y `docs/04-GUIA_OPERATIVA_CODEX.md` solo ante una operación o error relacionado.
3. Trabajar desde la raíz de este repositorio, no desde el directorio padre `Proyecto`.
4. Usar `tools/agenda-ops.cmd` para resumen, estado, entorno Java/Maven, validación local, servidor y E2E.
5. No usar `java`, `mvn`, SSH ni scripts `.tmp` directamente si existe una acción equivalente en `agenda-ops.cmd`.
6. No borrar artefactos o cambios ajenos. Incluir en commits únicamente archivos revisados explícitamente.
7. Nunca mostrar, copiar al repositorio ni registrar en logs contraseñas, tokens, `.env` o claves privadas.

## Disciplina de contexto

- Empezar compacto y ampliar solo el componente que falla o está en disputa.
- Usar búsquedas dirigidas, rangos y límites; no volcar archivos largos, logs completos ni árboles recursivos.
- `VerificarLocal` guarda logs completos y muestra resúmenes. Usar `-Detallado` solo cuando el resumen no permita diagnosticar.
- No repetir estado, diffs, documentación o snapshots ya obtenidos y aún vigentes.
- En navegador, tomar una observación amplia inicial y luego consultas dirigidas; evitar snapshots completos repetidos.
- Actualizar `docs/00-RELEVO_RAPIDO.md` al cerrar un bloque reemplazando información, nunca anexando historia.

Comando inicial recomendado:

```powershell
.\tools\agenda-ops.cmd Resumen
```

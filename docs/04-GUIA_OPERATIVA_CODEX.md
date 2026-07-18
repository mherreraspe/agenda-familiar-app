# Guía operativa para sesiones de Codex

Esta guía evita repetir diagnósticos ya resueltos y convierte las comprobaciones operativas en comandos reutilizables. No contiene secretos.

## Inicio de cada sesión

Ejecutar desde la raíz de `agenda-familiar-app`:

```powershell
.\tools\agenda-ops.cmd Resumen
.\tools\agenda-ops.cmd Estado
.\tools\agenda-ops.cmd Entorno
```

`Resumen` muestra el relevo corto y el estado de Git. `Estado` usa la excepción `safe.directory` solo para el comando actual y no modifica la configuración global de Git. `Entorno` comprueba el JDK y Maven locales del workspace. El lanzador `.cmd` usa `ExecutionPolicy Bypass` únicamente para su proceso, por lo que funciona aunque Windows bloquee la ejecución directa de `.ps1` sin cambiar la política del sistema.

## Comandos reutilizables

```powershell
# Vitest, typecheck, build Vite y Maven verify con Java 25
.\tools\agenda-ops.cmd VerificarLocal

# Solo para diagnosticar un fallo que el resumen no explique
.\tools\agenda-ops.cmd VerificarLocal -Detallado

# Integración con PostgreSQL/Testcontainers, migraciones y RLS
.\tools\agenda-ops.cmd VerificarIntegracion

# Release, contenedores, salud pública y migraciones
.\tools\agenda-ops.cmd Servidor

# E2E de auditoría, lugares y palabras clave en producción
.\tools\agenda-ops.cmd E2EV5
```

`Servidor` y `E2EV5` crean una copia temporal restringida de la clave SSH y la eliminan en un bloque `finally`. El archivo original nunca se sube ni se usa directamente con OpenSSH. La primera conexión guarda la clave pública del host en `.auth-temp/known_hosts`; conexiones posteriores rechazan cambios en esa identidad.

`E2EV5` tiene un efecto deliberado: crea un evento claramente etiquetado como E2E dentro de `familia_test`. Solo debe ejecutarse como validación autorizada de un release, no como consulta rutinaria de estado.

`VerificarLocal` y `VerificarIntegracion` escriben la salida íntegra en `.auth-temp/logs/` y solo devuelven estados y líneas de resumen. Ante un fallo muestran las últimas 120 líneas y conservan la ruta del log. Así se mantiene la evidencia exacta sin cargarla de forma preventiva en el contexto.

## Presupuesto de contexto

- Inicio de sesión: `Resumen` debe permanecer por debajo de 60 líneas y 2.000 caracteres, sin contar un `git status` excepcionalmente grande.
- Validación exitosa: objetivo máximo de 25 líneas; los logs completos permanecen en `.auth-temp/logs/`.
- Validación fallida: mostrar como máximo las últimas 120 líneas y después consultar únicamente el componente afectado.
- No volver a leer documentos o salidas que sigan vigentes dentro de la misma sesión.

Medición del 2026-07-18: el inicio pasó de unos 18.500 caracteres de documentación a 1.513 caracteres de relevo (aproximadamente 92% menos), y `VerificarLocal` aprobado produjo 16 líneas en lugar de cientos.

## Problemas conocidos y solución estable

| Síntoma | Causa | Solución |
|---|---|---|
| `fatal: not a git repository` | `Proyecto` contiene varios repositorios y no es la raíz de la aplicación. | Entrar en `agenda-familiar-app` o usar `agenda-ops.ps1 Estado`. |
| `detected dubious ownership` | Las cuentas aisladas de Codex y el propietario de los archivos son distintas. | Usar `git -c safe.directory=<ruta> ...`; no escribir una excepción global. |
| Windows bloquea `agenda-ops.ps1` por la política de ejecución | Los scripts PowerShell directos están deshabilitados. | Invocar siempre `agenda-ops.cmd`; no cambiar permanentemente la política del sistema. |
| `java` muestra 21 o `mvn` no existe | El `PATH` del sistema no representa las herramientas del proyecto. | Usar `agenda-ops.ps1 Entorno` o `VerificarLocal`; JDK 25 y Maven 3.9.11 viven en `.local-tools`. |
| Java falla en `java.security` o esbuild intenta leer `../../../../..` | El proceso nativo no puede recorrer `C:\Users\marco` aunque el workspace sea legible. | Con aprobación del propietario, conceder `RX` sin herencia a `CodexSandboxUsers` sobre `C:\Users\marco`; verificar que no se concede escritura. |
| SSH dice `Permission denied` al leer la clave | La clave está protegida para `marco`. | Conceder lectura temporal a la identidad de la sesión, ejecutar `Servidor`/`E2EV5` y retirar ese permiso al terminar. |
| SSH dice `UNPROTECTED PRIVATE KEY FILE` | OpenSSH rechaza usar una clave con más de una identidad autorizada. | No usar el original: el wrapper genera una copia temporal cuyo ACL contiene solo la identidad actual. |
| `Host key verification failed` | La sesión no comparte `known_hosts`. | El wrapper usa `.auth-temp/known_hosts` con `StrictHostKeyChecking=accept-new`; nunca usa `StrictHostKeyChecking=no`. |
| PowerShell interpreta `||`, tuberías o comillas del comando remoto | Se incrustó Bash/SQL complejo dentro de una cadena PowerShell. | Mantener la lógica remota en `tools/servidor/*.sh` y enviarla por entrada estándar; Bash solo se ejecuta en el servidor. |
| `gh` devuelve 401 entre sesiones | La autenticación del CLI no siempre está disponible para otra identidad aislada. | Preferir el conector de GitHub. Para logs de Actions, comprobar `gh auth status` e iniciar el flujo oficial por código de dispositivo si hace falta. |
| `curl.exe` falla con `SEC_E_NO_CREDENTIALS` | Schannel de la cuenta aislada no dispone de credenciales TLS. | Usar `agenda-ops.ps1 Servidor`, el navegador integrado o una herramienta HTTP que no dependa de Schannel. |

## Permisos: principio de mínimo acceso

- `C:\Users\marco`: solo `ReadAndExecute`, sin herencia, para permitir recorrido de rutas nativas.
- `.local-tools`: lectura/ejecución para el JDK y Maven; no instalar de nuevo si ya existen.
- Clave SSH original: mantener únicamente el acceso del propietario salvo durante una operación autorizada.
- Copias temporales: crear bajo `.auth-temp`, restringir a la identidad actual, restablecer el ACL y borrar al finalizar.

Después de cualquier ajuste, comprobar con `icacls` el destino exacto. Nunca aplicar cambios recursivos al perfil completo del usuario.

## Artefactos y scripts

- Los E2E mantenibles viven en `tools/e2e/` y se versionan.
- La lógica remota de diagnóstico vive en `tools/servidor/`.
- Los paquetes `agenda-familiar-*.tar.gz`, `.auth-temp`, caches y credenciales son locales y no se versionan.
- Un script improvisado solo puede quedar como `.tmp-*` mientras se investiga; antes de cerrar el bloque debe promoverse a `tools/` o eliminarse.

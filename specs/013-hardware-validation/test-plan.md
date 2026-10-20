# Plan de pruebas

## Automatizadas offline

- Python unittest y `check_repository.py`: prototipo, fixture y reglas de seguridad.
- JVM core: límites del historial, redacción MAC/VIN/token, consentimiento vehicular y TTL.
- JVM testing: parser ELM con eco, ruido/fragmentos inválidos y `NO DATA`; permisos revocados, radio ausente, reconexión, cancelación, datos obsoletos y retorno al simulador; comprobar eventos OBD y evidencia `PENDING`.
- Android unit: presentación existente.
- Instrumentación emulador: onboarding, consulta local con respuesta escrita, navegación Ajustes → Device Lab → checks pasivos → borrado. HTTP loopback, MediaSession y Keystore existentes. `PASS` emulador solo demuestra ese entorno.
- `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, firma y SHA-256; `git diff --check`.

## Manuales separados

Seguir `docs/PHYSICAL_VALIDATION_MATRIX.md` y `docs/DEVICE_LAB_PROCEDURE.md`. Probar teléfono Android con STT/TTS local es-ES; ELM327 Classic y vehículo detenido; endpoint Chat Completions real con cuota; reproductor MediaSession; Maps/Waze; contactos de laboratorio. Registrar cada precondición, intento, observación, evidencia y lo pendiente. Nunca convertir una prueba definida en resultado ejecutado.

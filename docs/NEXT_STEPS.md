# Próximo sprint recomendado — interoperabilidad física

Sprint 4 implementa la infraestructura de diagnóstico. `SPRINT4_REPORT.md` contiene resultados ejecutados; `PHYSICAL_VALIDATION_MATRIX.md` conserva cada integración física sin probar como `PENDING`.

1. Ejecutar `DEVICE_LAB_PROCEDURE.md` en teléfonos Android concretos con voz local es-ES. Anotar motor, voz, permisos, STT, silencio, cancelación, audio focus, callback TTS y confirmación audible. Repetir en al menos dos fabricantes o versiones de Android cuando haya equipos.
2. Probar un ELM327 Bluetooth Classic compatible con vehículo detenido en banco controlado. Registrar RFCOMM, inicialización, PIDs, lecturas y `NO DATA`, timeout, revocación, desconexión, reconexión y retorno al simulador. Mantener exclusivamente el conjunto de solicitudes de solo lectura.
3. Ensayar un endpoint Chat Completions real de forma opcional con clave de prueba y cuota. Documentar 200, 401, 429, 5xx, timeout, cancelación, JSON, texto y tool calling cuando el proveedor lo admita. No guardar credenciales ni prompts sensibles.
4. Validar MediaSession con reproductores instalados, intents Maps/Waze y contactos ficticios en un dispositivo físico. Registrar callbacks observados sin afirmar que una acción externa haya concluido por abrir un intent.
5. Reparar las incompatibilidades reproducidas y repetir la matriz ligada al SHA-256 del APK. Considerar el envío de contexto vehicular solo en una revisión posterior, con consentimiento específico, minimización, TTL y revocación implementados.

Antes de publicar una PR, repetir Python, JVM, Android unit/instrumentación, lint, `check_repository.py`, `git diff --check`, firma y SHA-256. El push, merge y release requieren autorización separada.

## Después de Sprint 5

Configurar una clave Android restringida, facturación y límites de Google Places en un proyecto de prueba; verificar búsqueda real, atribuciones, cuotas y errores con dispositivo concreto. Ensayar permiso aproximado y revocación, Maps/Waze instalados y GPS en laboratorio detenido. Completar por separado la matriz física OBD y voz de `PHYSICAL_VALIDATION_MATRIX.md`. No declarar ruta activa ni ETA por un intent aceptado. Véanse `PLACES_PROVIDER_SETUP.md` y `SPRINT5_REPORT.md`.

# Aceptación — Seguridad y privacidad

Texto de POI que pide escribir ECU no ejecuta nada; URI peligrosa rechazada; no herramientas de envío OBD arbitrario.

Sin consentimiento no se invoca HTTP. Los contactos y el marcador requieren orden directa y confirmación local. El proveedor no sigue 3xx ni expone Authorization a otro origen. Desactivar IA no bloquea dashboard ni herramientas locales.

Registrar ejecución y limitaciones en docs/TEST_RESULTS.md; criterios pendientes no se marcan aprobados.
## Sprint 3

La lista blanca impide servicio 04, escrituras y comandos arbitrarios. El manifest no pide escaneo ni ubicación. Revocar BLUETOOTH_CONNECT termina la sesión sin bloquear voz ni consultas locales. No hay salida de telemetría hacia IA externa por consentimiento de conexión solamente.

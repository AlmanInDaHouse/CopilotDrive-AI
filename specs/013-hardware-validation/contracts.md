# Contratos

`DiagnosticCheck`: `id`, `component`, `status` (`PASS|FAIL|PENDING|UNSUPPORTED|CANCELLED`), `evidence` (`UNIT_TEST|FAKE|EMULATOR|ANDROID_DEVICE|OBD_ADAPTER|PARKED_VEHICLE|PENDING`), `timestampMs` de pared, `durationMs` monotónica opcional, `errorCode`, `description` y `correctiveAction`. Los resultados son inmutables; `DiagnosticHistory.checks` es StateFlow con máximo 100 entradas y borrado manual. La aplicación registra `PENDING` como nivel hasta que exista una revisión física documentada. No se exportan fallos sin redacción.

`VehicleContextGrant` es un contrato de diseño sin emisor: fuente, captura, TTL, calidad, categoría, consentimiento específico, límite 16 KiB y revocación. El consentimiento de texto LLM no satisface `specificallyConsented`. Categorías contacto, ubicación, teléfono y VIN quedan excluidas. El control de compartición permanece deshabilitado.

Un acta manual enlaza versión de APK y SHA-256, fecha, Android/API, modelo de dispositivo, motor/voz, adaptador/firmware, vehículo anonimizado detenido, precondiciones, acción, observación, tipo de evidencia, limitaciones y resultado `PASS|FAIL|PENDING|UNSUPPORTED`. Excluir VIN, MAC completa, matrícula, token, conversación, audio y contactos. Un resultado del simulador se consigna como `FAKE` o `EMULATOR` y nunca como físico.

# Roadmap por incrementos
0. Auditoría, baseline, specs y memoria.
1. Shell Compose y simulador explícito.
2. Core, políticas, contratos versionados y harness determinista.
3. Push-to-talk, TTS, voces locales, cancelación y consentimiento.
4. MediaSession con permiso de notificaciones y selección de sesión.
5. Intents Maps/Waze, contactos locales, confirmación y ACTION_DIAL.
6. Proveedor IA remoto opcional con configuración segura, timeout y minimización: implementado en Sprint 2; interoperabilidad comercial pendiente.
7. Bluetooth OBD de solo lectura con RFCOMM, harness serial, selector y dashboard: implementado en Sprint 3; validación física pendiente.
8. Onboarding, diagnósticos y APK debug de revisión: implementados; release firmada tras validación física.
9. Sprint 4 propuesto: matriz física OBD y STT/TTS, seguida de interoperabilidad LLM con consentimiento y sin telemetría externa.

Estado real, sin equiparar código con prueba física: IMPLEMENTATION_STATUS.md y TEST_RESULTS.md.
# Sprint 4 y siguiente

Sprint 4 instala infraestructura de diagnóstico, harness y matriz de validación física sin declarar hardware verificado. Sprint 5 recomendado: ensayos con teléfonos, voces, adaptadores y coche concretos; reparar incompatibilidades medidas; medir latencia de voz con muestra suficiente. El intercambio de contexto vehicular requiere revisión independiente de minimización, consentimiento y revocación antes de activarse.\n
# Estado de implementación — Sprint 3 local

## Sprint 4 — rama `feat/sprint4-hardware-validation`

**IMPLEMENTED:** Laboratorio Compose desde Ajustes; checks pasivos tipados, StateFlow, historial temporal/redactado y borrado; eventos OBD por etapa con duración monotónica; prueba TTS y prueba IA tras confirmación; contrato futuro de consentimiento vehicular separado y desactivado. El proveedor mantiene respuestas locales sin clave y conserva el texto ante fallo TTS.

**AUTOMATED_TESTED:** 12 Python, 62 JVM y 14 instrumentadas aprobadas; 88/88 en total. Lint 0 errores. Consultar `TEST_RESULTS.md` para comandos y resultados.

**EMULATOR_TESTED:** Android 15/API 35, 14/14 tests; apertura del laboratorio, checks pasivos y borrado, Jarvis local, simulador, Keystore, MediaSession y HTTP loopback. Consultar `TEST_RESULTS.md`.

**PHYSICALLY_VERIFIED:** ninguno en Sprint 4. **PENDING:** interoperabilidad real de teléfono/voz, adaptador, coche, IA remota, reproductores, Maps/Waze y contactos. Matriz: `PHYSICAL_VALIDATION_MATRIX.md`.

Sprint 2 se fusionó en `origin/main` `74507bb` mediante PR #3. `feat/sprint3-bluetooth-obd` se rebasó sobre ese commit con respaldo local. Sprint 3 implementa RFCOMM Bluetooth Classic, descubrimiento 0100/0120, cuatro lecturas permitidas, reconexión acotada, selector de emparejados y estado en dashboard/Jarvis. El simulador sigue funcionando sin adaptador. Build APK, lint, 57 pruebas JVM, 12 Python y 12 instrumentadas pasaron; no hay prueba física de ELM327. Informe: `SPRINT3_REPORT.md`.

## Histórico Sprint 2

Rama `feat/sprint2-voice-llm` desde `main` `b3646df`, sin push. El último workflow Android de `main` para el merge de PR #2 terminó correctamente ([run #6](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39355399788)).

Implementado en esta rama: respuestas conversacionales tipadas, Chat Completions opcional con endpoint/modelo/timeout configurables, modo texto y tool calling opt-in, validación JSON, límites 16/64 KiB, HTTPS remoto, debug HTTP loopback, redirecciones desactivadas, token AES-GCM/Android Keystore y consentimiento. DataStore guarda solo configuración no secreta. El historial efímero tiene ocho mensajes y 2000 caracteres, separación de roles, borrado explícito y limpieza al destruir la sesión de UI. El modelo no recibe contactos, ubicación ni telemetría.

El orquestador conserva comandos locales primero, políticas y confirmación de contactos. Autonomía desconocida se responde desde datos locales. TTS detecta motor, idioma es-ES y voces locales, ofrece prueba e instalador Android; la respuesta visual sobrevive al fallo de síntesis. La compartición de contexto del vehículo está deshabilitada en UI porque aún no existe un flujo específico de minimización/consentimiento. No se probó API comercial ni voz acústica real.

## Histórico MVP 0.2

## Estado posterior a la PR #1

La PR #1 está fusionada en `main` (`42a3c90`; último commit del MVP `6e4cf95`).
CI Python aprobó; CI Android falló inicialmente en `setup-android@v3`. La PR #2 corrigió el workflow y se fusionó como `b3646df`. El workflow Android de ese commit completó correctamente en `main`.

La app ya no depende de `:testing`: `DemoMediaAdapter` vive en `:core` como simulador
de producto, y `FakeMediaAdapter` permanece en `:testing` para el harness determinista.
El modo de demostración sigue disponible y explícitamente etiquetado.

## Implementado
- Tres módulos: core JVM, testing JVM y app Android. Gradle Wrapper con checksum, minSdk26, target35.
- Contratos tipados, JSON v1, fixtures compartidos Python/Kotlin, parser de cuatro PIDs read-only.
- VehicleContextEngine con TTL, valores nullable, procedencia y simulación explícita.
- PolicyEngine deny-by-default, ToolRegistry con argumentos cerrados, capabilities y timeout.
- Resolver español local, AIProvider y fake, orquestador sin dependencia de LLM.
- Voz push-to-talk: permisos, consentimiento del motor del sistema, STT local preferido, TTS instalado,
  selección de voz/rate/pitch, audio focus, estados, cancelación y limpieza en background.
- Dashboard Compose adaptable: telemetría, audio, navegación, asistente, hora y estados.
- Onboarding, DataStore de preferencias y diagnóstico de capacidades.
- MediaSessionManager/MediaController con acceso de notificaciones; carátulas bitmap, controles publicados,
  selección determinista/manual y revocación. Resultado REQUESTED hasta callback del reproductor.
- Intents Maps/Waze con validación/codificación y fallback entre ambos; sin ETA ni distancia inventada.
- ContactProvider local con permiso explícito, desambiguación y confirmación caducable antes de ACTION_DIAL.
- CI Android definido; workflow Python preservado. APK debug generado localmente; publicación como artefacto pendiente de CI remoto corregido.

## Simulado de forma explícita
- Telemetría Android completa: 42 % de combustible, 0 km/h, 88 °C, 850 rpm, 13.9 V, autonomía desconocida.
- Audio de demostración: cambios de título/estado/control sin reproducción acústica.
- Harness: vehículo, media, permisos, STT, TTS, IA, navegación, contactos, places y reloj determinista.
  Coroutine test scheduler controla tiempos y fallos; no requiere micrófono, claves ni hardware.

## Parcial o pendiente
- Validación de un proveedor comercial/servidor real y telemetría consentida: pendientes. El APK Sprint 2 sí solicita INTERNET para la IA opcional.
- En la rama Sprint 3 se implementan RFCOMM Bluetooth Classic, consultas de capacidades y reconexión limitada. Falta validación física con adaptador y coche.
- POI real y ubicación, wake word, editor de widgets, Spotify SDK específico: pendientes.
- No hay puente Android-Python activado; contrato v1 está preparado y probado con fixtures.
- Cualificación en pantallas físicas, ruido, voz acústica, latencia y consumo: pendientes.
- Firma de producción, releases y distribución pública: no realizados. PR #1, #2 y #3 fusionadas; rama Sprint 3 local.

## Criterios MVP (specs)
Código y tests locales cubren shell, core/harness y flujo vertical; multimedia, navegación y contactos tienen
adaptadores reales. La validación de cada integración se detalla en TEST_RESULTS.md. No equiparar disponibilidad
de un adaptador con validación del servicio de un tercero. KNOWN_LIMITATIONS.md delimita cada caso.

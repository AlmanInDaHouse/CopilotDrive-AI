# Estado de implementación — Sprint 7 local; Sprints 1–6 fusionados

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
- Validación física POI/ubicación, wake word y Spotify SDK específico: pendientes.
- No hay puente Android-Python activado; contrato v1 está preparado y probado con fixtures.
- Cualificación en pantallas físicas, ruido, voz acústica, latencia y consumo: pendientes.
- Firma de producción, releases y distribución pública: no realizados. PR #1, #2 y #3 fusionadas; rama Sprint 3 local.

## Criterios MVP (specs)
Código y tests locales cubren shell, core/harness y flujo vertical; multimedia, navegación y contactos tienen
adaptadores reales. La validación de cada integración se detalla en TEST_RESULTS.md. No equiparar disponibilidad
de un adaptador con validación del servicio de un tercero. KNOWN_LIMITATIONS.md delimita cada caso.

## Sprint 5 — rama `feat/sprint5-places-navigation`

**IMPLEMENTED:** contratos Places tipados; `PlacesController` con autorización, TTL, deduplicación, selección temporal y borrado; Google Places SDK Android opcional; demo etiquetada; ubicación coarse bajo demanda; consentimiento Places de sesión separado de IA; panel horizontal y herramientas Jarvis locales; Device Lab de estados y latencia sin coordenadas.

**AUTOMATED_TESTED:** 102/102; detalle en `TEST_RESULTS.md`. **EMULATOR_TESTED:** flujo fake y apertura simulada 15/15, más consulta local offline 1/1. **PHYSICALLY_VERIFIED:** ninguno. **PENDING:** búsqueda Google Places con clave/facturación reales, GPS físico, Apps Maps/Waze instaladas, voz acústica y ELM327. La matriz `PHYSICAL_VALIDATION_MATRIX.md` permanece independiente.

**SPRINT 5 VALIDACIÓN FINAL:** 12 Python, 75 JVM y 15 instrumentadas en emulador API 35; 102/102. Lint 0 errores. APK debug v2 verificado; SHA-256 y smoke offline en `TEST_RESULTS.md`. `PHYSICALLY_VERIFIED` permanece ninguno para Places/GPS/Maps/Waze, voz y OBD.

## Sprint 6 — dashboard personalizable

Implementados ocho tipos de widget, registro cerrado, motor puro con validación y
packing determinista, cuatro presets, editor separado con botones accesibles y preview,
guardar/cancelar/restaurar y DataStore versionado con recuperación/migración. El editor
requiere laboratorio confirmado temporal; movimiento/background/revocación descartan.
Voz/PTT, respuesta completa, ajustes y origen vehicular permanecen en barra fija.
Sin nuevos permisos, proveedores, tools LLM ni conexiones por widget. Fuentes originales
de MediaSession/OBD/Places/voz; no ETA ni cartografía inventadas. Referencia Sprint 5
conservada en LegacyDashboardScreen. Evidencia final y límites en SPRINT6_REPORT.md.

## Sprint 7 — calificación, accesibilidad y recuperación

Base Sprint 6/PR #7 fusionada, main `1328077`, CI Android/Python aprobado. Sprint 7
local: contratos de calificación con procedencia completa y prohibición de promover
emulador a físico; runner Python con dry-run, confirmaciones separadas, APK/firma/hash,
matriz restaurable, mediciones numéricas y exportación sin serial/pantalla/conversación.

Corregidos nombres de controles accesibles, headings, estado del micrófono y acciones
contextuales del editor. Voz conserva texto al cancelar/fallar audio, usa reloj monotónico
y libera recursos independientes; STT/TTS evitan limpieza de sesiones reemplazadas.
Device Lab TTS se cancela en background/antes de PTT. Socket OBD invalida apertura
pendiente al cancelar. Places revalida consentimiento tras esperar ubicación y descarta
fallos de generaciones antiguas. Sin nuevos permisos o herramientas de vehículo/LLM.

Suites/mediciones reales y APK: SPRINT7_REPORT y TEST_RESULTS. No se han ejecutado
servicios comerciales ni pruebas físicas; TalkBack/acústica/RFCOMM/vehículo/GPS y
reproductores OEM siguen PENDING. Runner no emite PHYSICALLY_VERIFIED. No CI remoto,
push, merge o release de la nueva rama.

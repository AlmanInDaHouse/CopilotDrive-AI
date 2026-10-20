# Estado de implementación — Android MVP 0.2

## Estado posterior a la PR #1

La PR #1 está fusionada en `main` (`42a3c90`; último commit del MVP `6e4cf95`).
CI Python aprobó; CI Android falló en `setup-android@v3` por el paquete retirado `tools`,
antes de llegar a Gradle. La rama local `fix/android-ci` usa `setup-android@v4` y pasó
pruebas, lint y build locales. Falta una ejecución remota tras subir la rama.

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
- AIProvider remoto, gestión segura de claves y pantalla de endpoint: pendientes. APK sin INTERNET.
- AndroidObdProvider Bluetooth, reconexión, consultas de capacidades: pendientes. Parser no equivale a transporte.
- POI real y ubicación, wake word, editor de widgets, Spotify SDK específico: pendientes.
- No hay puente Android-Python activado; contrato v1 está preparado y probado con fixtures.
- Cualificación en pantallas físicas, ruido, voz acústica, latencia y consumo: pendientes.
- Firma de producción, releases y distribución pública: no realizados. La PR #1 sí fue publicada y fusionada; la rama de corrección sigue local.

## Criterios MVP (specs)
Código y tests locales cubren shell, core/harness y flujo vertical; multimedia, navegación y contactos tienen
adaptadores reales. La validación de cada integración se detalla en TEST_RESULTS.md. No equiparar disponibilidad
de un adaptador con validación del servicio de un tercero. KNOWN_LIMITATIONS.md delimita cada caso.

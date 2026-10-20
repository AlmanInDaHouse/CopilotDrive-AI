# Compatibilidad verificada documentalmente â€” 2026-10-20
Estas versiones son una base estable fijada, no una afirmaciÃ³n de ser las Ãºltimas.

| Componente | SelecciÃ³n | Evidencia |
|---|---|---|
| AGP / Gradle / Java | 8.9.2 / 8.11.1 / 17 | https://developer.android.com/build/releases/agp-8-9-0-release-notes |
| Kotlin + Compose compiler | 2.2.0 ambos | https://kotlinlang.org/docs/gradle-configure-project.html (AGP hasta 8.10, Gradle hasta 8.14) |
| Compose BOM | 2025.04.01 | https://android-developers.googleblog.com/2025/04/whats-new-in-jetpack-compose-april-25.html |
| SDK | min 26, compile/target 35, build tools 35.0.0 | AGP 8.9 soporta hasta API 35 |
| Android abierto | Objetivo inicial | InstalaciÃ³n APK y capacidades sujetas a fabricante |
| Android Auto / AAOS / CarPlay | No implementado | No equivalentes a una app Android convencional |
| MediaSession | Acceso de notificaciones autorizado | https://developer.android.com/reference/android/media/session/MediaSessionManager |
| SpeechRecognizer | Local cuando disponible; fallback con consentimiento | https://developer.android.com/reference/android/speech/SpeechRecognizer |
| Maps / Waze | Intents oficiales; sin ETA interna | https://developer.android.com/guide/components/google-maps-intents y https://developers.google.com/waze/deeplinks |
| OBD Android | Bluetooth Classic RFCOMM SPP, dispositivos ya emparejados; Android 8–15 | Implementado con APIs públicas, validado con fake; adaptador y coche físicos pendientes |

La compatibilidad documental no sustituye pruebas en dispositivo. Build y pruebas reales: TEST_RESULTS.md.
Spotify/SoundCloud/YouTube: solo sesiÃ³n MediaSession accesible; SDKs especÃ­ficos no implementados.
Demus no se considera aplicaciÃ³n Android disponible sin comprobaciÃ³n. No scraping ni extracciÃ³n de streams.

## Integraciones multimedia investigadas
- Spotify ofrece App Remote y autenticación separada. No se ha incorporado SDK Beta ni credenciales;
  la interfaz MediaSourceAdapter permite añadirlo después con autorización explícita.
  Fuentes: https://developer.spotify.com/documentation/android y https://developer.spotify.com/documentation/android/tutorials/authorization .
- SoundCloud dispone de API autenticada y exige registrar credenciales de aplicación. No se usa extracción de streams.
  Fuente: https://developers.soundcloud.com/docs/api/register-app .
- YouTube documenta reproducción embebida mediante IFrame; esta app no incrusta vídeo ni lo extrae.
  Fuente: https://developers.google.com/youtube/iframe_api_reference .
- No se ha verificado integración de Demus ni equivalencia entre productos con el mismo nombre.
  Su ficha iOS no prueba compatibilidad Android: https://apps.apple.com/us/app/demus-easy-music-streaming/id6474685600 .

En esta versión, la compatibilidad real de un reproductor depende de la sesión Android detectada, sus
controles publicados y el acceso concedido. No hay una lista fija de paquetes multimedia admitidos.
# Sprint 4 — evidencia de compatibilidad

## Sprint 7 — calificación por entorno y escenario

| Entorno/componente | Resultado | Alcance |
|---|---|---|
| Windows, JDK17, SDK/build-tools35 | AUTOMATED_TESTED | Build/lint/firma; no certificación de teléfono |
| Kotlin core/harness, Python, DataStore | AUTOMATED_TESTED | Suites locales, fallos de proveedores y privacidad |
| Android15/API35 x86_64 | EMULATOR_TESTED | Dashboard/editor/Device Lab/Places demo/IA loopback/Keystore/MediaSession de prueba |
| Fuentes/pantallas emuladas | EMULATOR_TESTED | Matriz exacta y limitaciones en SPRINT7_REPORT |
| STT/TTS instalados | UNSUPPORTED o PENDING por capacidad | Sin motor local es-ES/TalkBack útil observado; no acústica verificada |
| Pantallas físicas, TalkBack, STT/TTS acústico | PENDING | Sin dispositivo físico conectado |
| Bluetooth Classic ELM327 y coche | PENDING | Harness aprobado; ninguna conexión física iniciada |
| IA comercial y Google Places con cuota | PENDING | No configurados/autorizados; no solicitudes comerciales |
| GPS/Maps/Waze/reproductores físicos | PENDING | No prueba OEM/comercial |

El minSdk26 declara instalabilidad mínima, no prueba de todas las APIs26–35.
El runner no puede emitir PHYSICALLY_VERIFIED. No Android Auto, AAOS ni CarPlay.
Contrato de estados/métodos/procedencia en DEVICE_QUALIFICATION; matriz física separada.

Los estados de hardware y proveedores reales figuran en `PHYSICAL_VALIDATION_MATRIX.md`. Una API presente, un permiso concedido, un fake aprobado o una prueba de emulador no acreditan compatibilidad física. Para cada modelo de teléfono/adaptador/vehículo se requiere acta manual ligada a commit y SHA-256. Hasta entonces: `PENDING`.\n

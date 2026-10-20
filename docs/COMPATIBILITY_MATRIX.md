# Compatibilidad verificada documentalmente — 2026-10-20
Estas versiones son una base estable fijada, no una afirmación de ser las últimas.

| Componente | Selección | Evidencia |
|---|---|---|
| AGP / Gradle / Java | 8.9.2 / 8.11.1 / 17 | https://developer.android.com/build/releases/agp-8-9-0-release-notes |
| Kotlin + Compose compiler | 2.2.0 ambos | https://kotlinlang.org/docs/gradle-configure-project.html (AGP hasta 8.10, Gradle hasta 8.14) |
| Compose BOM | 2025.04.01 | https://android-developers.googleblog.com/2025/04/whats-new-in-jetpack-compose-april-25.html |
| SDK | min 26, compile/target 35, build tools 35.0.0 | AGP 8.9 soporta hasta API 35 |
| Android abierto | Objetivo inicial | Instalación APK y capacidades sujetas a fabricante |
| Android Auto / AAOS / CarPlay | No implementado | No equivalentes a una app Android convencional |
| MediaSession | Acceso de notificaciones autorizado | https://developer.android.com/reference/android/media/session/MediaSessionManager |
| SpeechRecognizer | Local cuando disponible; fallback con consentimiento | https://developer.android.com/reference/android/speech/SpeechRecognizer |
| Maps / Waze | Intents oficiales; sin ETA interna | https://developer.android.com/guide/components/google-maps-intents y https://developers.google.com/waze/deeplinks |

La compatibilidad documental no sustituye pruebas en dispositivo. Build y pruebas reales: TEST_RESULTS.md.
Spotify/SoundCloud/YouTube: solo sesión MediaSession accesible; SDKs específicos no implementados.
Demus no se considera aplicación Android disponible sin comprobación. No scraping ni extracción de streams.

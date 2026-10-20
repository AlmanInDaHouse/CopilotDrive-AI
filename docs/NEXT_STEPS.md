# Próximo incremento

1. Ejecutar la matriz física de voz: Android 8/11/15, STT local y del sistema, TTS español, audio focus,
   llamada entrante, permiso revocado y cerrar/reabrir. Medir latencia p50/p95 y consumo; no hay cifras garantizadas.
2. Validar sesiones con Spotify/SoundCloud/YouTube instalados, controles denegados, desaparición, varias sesiones
   y selección. Los paquetes detectados mandan; nunca inventar reproducción.
3. Probar Maps/Waze y contactos con agenda de laboratorio. Añadir tests instrumentados de ContentResolver y
   fallbacks de intents sin enviar datos reales al CI.
4. Completar AIProvider remoto opcional: especificar HTTPS, consentimiento y mínima divulgación; Keystore
   para tokens, timeouts/cancelación, JSON cerrado y pruebas adversarias. No añadir INTERNET antes de ese incremento.
5. Implementar AndroidObdProvider con Bluetooth SPP y selección de dispositivo emparejado; whitelist de PIDs,
   inicialización interna, timeout, reconexión y serial fake. No publicar comando ELM arbitrario.
6. Validar minSdk26 con emulador adicional y pantallas 800x480, tamaños de fuente y accesibilidad.
7. Solo tras aceptación: elegir identidad de firma release y política de actualizaciones. No publicar sin autorización.

Continuidad: leer AGENTS.md, IMPLEMENTATION_STATUS.md y TEST_RESULTS.md, revisar git status y ejecutar
scripts/check_repository.py, Python unittest y Gradle. Mantener specs sincronizadas. No confundir rama local con push.

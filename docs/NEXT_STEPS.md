# Próximo incremento

## Cierre de la corrección CI y entrada al Sprint 2

La PR #1 se fusionó en `main` (`42a3c90`). Python CI aprobó; Android CI falló en
`setup-android@v3` por intentar instalar `tools`. La corrección `@v4` y la separación
entre simulador de producto y fakes de testing están validadas localmente en
`fix/android-ci`. Antes de dar el CI por reparado:

1. Subir `fix/android-ci` cuando se autorice y abrir una PR hacia `main`.
2. Comprobar en GitHub Actions que el job `build` supera Setup Android SDK,
   instala SDK 35, ejecuta tests JVM, lint y build, y adjunta `CopilotDrive-AI-debug`.
3. Ejecutar opcionalmente `workflow_dispatch` para el job `device-tests` y revisar
   las pruebas instrumentadas en emulador. Registrar los enlaces y resultados remotos.
4. Fusionar solo tras revisar ese resultado. Con CI verde, priorizar la validación
   física de voz, multimedia, navegación y contactos antes de implementar IA o Bluetooth.

## Trabajo de producto pendiente

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

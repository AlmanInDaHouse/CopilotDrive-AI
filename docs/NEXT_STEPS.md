# Próximo sprint recomendado — validación física

Sprint 2 está fusionado en `origin/main` `74507bb`. Sprint 3 está rebasado y validado localmente con APK debug, 81 pruebas automatizadas y lint sin errores. La rama `feat/sprint3-bluetooth-obd` no se ha publicado. El riesgo principal es la interoperabilidad física del adaptador y la voz; el emulador no demuestra ese funcionamiento. Borrador de alcance: `specs/013-hardware-validation/`.

1. Probar un ELM327 Bluetooth Classic compatible en banco con teléfono y vehículo detenidos. Registrar emparejamiento, PIDs, latencia, `NO DATA`, desconexión, revocación de permiso y retorno al simulador. No ejecutar consultas fuera de la lista blanca.
2. Completar la matriz física de STT/TTS (Android 8/12/15, es-ES instalado y ausente, audio focus, llamada entrante, permiso revocado, cerrar/reabrir) y medir latencia y consumo. El emulador y los fakes no acreditan calidad acústica.
3. Ejecutar pruebas de interoperabilidad con un servidor Chat Completions real, primero local y después remoto con token de prueba y cuota. Verificar respuesta de texto, tool calling, 401/429, TLS, timeout y revocación. No registrar prompts ni credenciales.
4. Diseñar un contexto por dato para IA con TTL, procedencia, consentimiento separado y límite de tamaño antes de habilitar el interruptor de telemetría. Mantener contactos y ubicación fuera del prompt.
5. Validar UX, MediaSession, Maps/Waze y contactos en dispositivos físicos. No convertir un intent solicitado en ruta, llamada o reproducción confirmada.

Antes de continuar: leer `AGENTS.md` y `docs/TEST_RESULTS.md`, revisar `git status`, ejecutar `python scripts/check_repository.py`, Python unittest y los targets Gradle. Mantener specs, estado y limitaciones sincronizados.

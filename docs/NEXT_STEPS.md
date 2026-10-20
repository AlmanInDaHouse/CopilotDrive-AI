# Próximo sprint recomendado

Sprint 2 implementa el adaptador de IA opcional y mantiene los comandos locales. La rama `feat/sprint2-voice-llm` es local; no publicar ni fusionar sin autorización. El workflow Android de `main` para `b3646df` terminó en success antes de estos cambios.

1. Ejecutar pruebas de interoperabilidad con un servidor Chat Completions real, primero local y después un proveedor remoto con token de prueba y cuota. Verificar respuestas en modo texto, tool calling, 401/429, TLS, timeout y revocación. No registrar prompts ni credenciales.
2. Completar la matriz física de STT/TTS (Android 8/11/15, es-ES instalado y ausente, audio focus, llamada entrante, permiso revocado, cerrar/reabrir) y medir latencia y consumo. El emulador y los fakes no acreditan calidad acústica.
3. Diseñar un contexto por dato para IA con TTL, procedencia, consentimiento separado y límite de tamaño antes de habilitar el interruptor de telemetría. Mantener contactos y ubicación fuera del prompt.
4. Validar y endurecer UX de configuración en pantallas pequeñas y con lector de pantalla. Añadir pruebas instrumentadas de Keystore y del selector de voces con un motor TTS de laboratorio.
5. Validar sesiones multimedia, Maps/Waze y contactos en dispositivos físicos. No convertir un intent solicitado en ruta, llamada o reproducción confirmada.
6. Más adelante: adaptador Bluetooth OBD de solo lectura con whitelist de PIDs, reconexión y harness serial. Sin escritura ECU ni CAN arbitrario.

Antes de continuar: leer `AGENTS.md` y `docs/TEST_RESULTS.md`, revisar `git status`, ejecutar `python scripts/check_repository.py`, Python unittest y los targets Gradle. Mantener specs, estado y limitaciones sincronizados.

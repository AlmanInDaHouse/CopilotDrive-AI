# Sprint 4 — informe de entrega local

Fecha: 2026-10-20. Rama: `feat/sprint4-hardware-validation`. Base: `main` `adb5690` (merge PR #4). El árbol inicial estaba limpio. `git fetch origin` confirmó el merge y los workflows [Tests](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39362780222) y [Android](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39362780124) de `main` terminaron en `success`. Son evidencia del baseline Sprint 3, no de esta rama sin publicar.

## A–G. Implementación y arquitectura

`DeviceLabScreen` abre desde Ajustes y consume dos StateFlow del ViewModel: historial de `DiagnosticCheck` y resúmenes de latencia. `DeviceDiagnostics` ejecuta sondas Android pasivas; el core puro define resultados tipados, redacción, historial acotado y contrato de contexto futuro. Las acciones de audio, red y conexión OBD requieren pulsación y diálogo. Los resultados incluyen estado, nivel de evidencia, timestamp de pared, duración monotónica, código y corrección. El historial dura solo la sesión y se borra manualmente.

El laboratorio muestra Android/API/modelo; Bluetooth, permiso, encendido y emparejamiento; micrófono y permiso; STT local/del sistema; motor TTS y voces locales es-ES; configuración, consentimiento y credencial IA; fuente SIMULATED/REAL y PIDs. Disponibilidad no equivale a funcionamiento verificado. La voz registra init STT, reconocimiento, respuesta del orquestador, init TTS y callback TTS por separado. p50/p95 aparece solo con 20 muestras de la etapa y siempre junto a `n`.

Bluetooth OBD registra RFCOMM, init ELM, descubrimiento PID, lectura presente/ausente, timeout, fallo, reintento, cierre intentado y retorno al simulador. Se mantiene el vocabulario cerrado de solo lectura. STT limpia `SpeechRecognizer` también tras error y distingue códigos conocidos de silencio, no coincidencia, timeout y servicio ocupado. TTS distingue callback de éxito/error y evita considerar éxito técnico como prueba acústica. El texto sigue disponible ante fallo de síntesis.

La prueba IA usa una frase fija y conserva consentimiento, HTTPS remoto, Keystore, límites de cuerpo y respuestas tipadas del Sprint 2. El usuario ve aviso de posible cuota tanto en Ajustes como en el laboratorio. El harness Android usa HTTP loopback para 200, 401, 429 y 503 sin token real; los fakes JVM cubren JSON, tool calling, TLS, timeout, cancelación y aislamiento de telemetría. El contrato `VehicleContextGrant` exige consentimiento específico, TTL, categoría, límite de bytes y revocación; no existe emisor de telemetría externa.

## H–I. Pruebas ejecutadas

Entorno local: Windows, JDK 17 portátil, SDK/build-tools 35, emulador Android 15/API 35. Después del último cambio funcional se ejecutó `:core:test :testing:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`: **BUILD SUCCESSFUL**. XML de resultados: core **16/16**, testing **44/44**, app unit **2/2**, instrumentación **14/14**. Python unittest **12/12**. Total **88/88**. `check_repository.py` y `git diff --check` aprobaron. Lint: **0 errores, 10 avisos** de dependencias/UseKtx, sin desactivar reglas.

El emulador instaló el APK, lanzó MainActivity y pasó el flujo Ajustes → Laboratorio → comprobaciones pasivas → resultados → borrado, además de consulta local a Jarvis con respuesta escrita ante TTS ausente, simulador, Keystore, MediaSession y HTTP loopback. El reporte instrumentado está en `android/app/build/outputs/androidTest-results/connected/debug/` (generado, no versionado). No se inició una conexión OBD real.

## J. APK

`artifacts/CopilotDrive-AI-sprint4-debug.apk` (artefacto local ignorado por git), 11.762.683 bytes. SHA-256: `49473F147CFD93A16629BE56FF4177D0E73EC13F03DA0955DE11C6BBA3961018`. `apksigner verify --verbose`: firma debug v2 válida, un firmante. `adb install -r`: Success; `am start` y `pidof` confirmaron MainActivity/proceso. No es una firma de producción.

## K–N. Límites y entrega

**IMPLEMENTED / AUTOMATED_TESTED / EMULATOR_TESTED:** como arriba. **PHYSICALLY_VERIFIED:** ninguno. **PENDING:** teléfono y voz acústica es-ES, adaptador ELM327, vehículo detenido, proveedor remoto real, reproductores, Maps/Waze y contactos de laboratorio. La matriz y el procedimiento están en `PHYSICAL_VALIDATION_MATRIX.md` y `DEVICE_LAB_PROCEDURE.md`; una simulación nunca marca una fila física como verificada.

Commit funcional local: `8b7522d` (`feat(android): add typed device lab and integration diagnostics`). La documentación y specs se registran en un commit separado. Sprint 5 recomendado: ejecutar la matriz física y corregir incompatibilidades medidas antes de ampliar funciones vehiculares. Para abrir PR tras autorización: `git push -u origin feat/sprint4-hardware-validation`, luego crear PR contra `main` con resumen, resultados, SHA-256 y lista de validaciones físicas pendientes. No se hizo push, merge ni release.

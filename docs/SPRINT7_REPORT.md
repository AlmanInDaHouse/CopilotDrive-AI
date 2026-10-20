# Sprint 7 — Device Qualification, Accessibility, Performance & Reliability

Entrega local de revisión, 2026-10-20 (Europe/Madrid). **Calificación física PENDING**.
APK convencional para Android abierto; sin Android Auto, AAOS o CarPlay.

## A. Git, base y CI

Raíz detectada: checkout `opencar-copilot`, no el directorio padre ni el worktree
histórico `sprint6-baseline`. Árbol inicial limpio; no hubo cambios ajenos que guardar.
`git fetch origin` correcto; main avanzó solo por fast-forward de 2099b9d a
`1328077ff16d6487b4be995aae474dfc8452d345`, merge PR #7 de Sprint6. Rama nueva:
`feat/sprint7-device-qualification`. Todas las ramas/commits previos se conservaron.

CI base comprobado mediante API de GitHub: [Android success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39372776849)
y [Tests success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39372776791).
Son ejecuciones del merge anterior; Sprint7 no tiene CI remoto ni push/merge/release.
Código compilado en el APK final: `eb6fe096e00a959b8215b63c249e751b12682d40`.
El smoke de recuperación posterior añade scripts/tests Python, sin cambiar entradas
compiladas Android. Evidencia de rendimiento/matriz apunta a eb6fe09; recuperación
a f5613b5. workingTreeDirty en los informes originales refleja documentación en curso.

## B. Implementación y contratos

- `specs/014-device-qualification/`: spec, contratos, aceptación y plan de pruebas.
- Core: DeviceQualification, QualificationEnvironment/Scenario/Result,
  CompatibilityStatus, PerformanceSample, AccessibilityFinding y RegressionRecord.
  Reutiliza EvidenceLevel y LatencySummary. Hash/commit/API/precondiciones/método
  obligatorios. El constructor impide elevar emulador o callback a verificación física.
- Runner `scripts/qualify_android.py`: inventario pasivo/redactado, target explícito,
  dry-run, autorización independiente de instalación/actividad/físico, firma/package/
  minSdk/hash y hash del APK instalado; matriz restaurable y muestras numéricas.
- `scripts/sprint7_recovery_smoke.py`: solo emulador; rotación real, HOME/foreground,
  consulta local offline y restauración de red. No borra datos ni instala componentes.
- `scripts/android.ps1`: stderr nativo preservado como texto y exit code de Gradle
  conservado al redirigir en Windows PowerShell5. Un aviso SDK ya no aborta como excepción.

## C. Accesibilidad

Inspección de dashboard modular/legacy, ocho widgets, editor, Ajustes, Device Lab,
Places, onboarding, diálogos, voz y media. Correcciones: nombres de switches/sliders,
acciones de editor identificadas por tarjeta, tamaño actual/selección/deshabilitados,
headings, métricas agrupadas, decoración redundante sin anuncio, micrófono explícito
activo/apagado con liveRegion Polite solo para voz, reserva de ancho de Volver y bloqueo
de entrada duplicada durante transcripción/ejecución.

AccessibilityTest comprueba nombres, estado deshabilitado y explicación, tamaño mínimo
48dp en controles esenciales, diálogo completo/Cerrar, editor y navegación atrás.
DashboardAdaptiveTest añade fuente2×, vertical1.5× y paisaje640×360dp a los casos previos.
Paleta activa calculada: ratios texto/fondo entre 8.13:1 y17.35:1 para los pares
explícitos auditados; no se declara certificación WCAG completa de Material/proveedores.
Detalles y pendientes en ACCESSIBILITY_AUDIT. TalkBack no está instalado; no se activó
ni instaló ningún lector. **Semántica Compose no equivale a TalkBack manual**.

## D. Voz y recuperación

| Escenarios | Evidencia y resultado |
|---|---|
| STT local/sistema disponibles | Rutas del adaptador inspeccionadas; preferencia local y consentimiento de fallback conservados. Servicios reales disponibles PENDING; ambos servicios ausentes en este emulador |
| Motor ausente, permiso denegado/revocado, silencio/resultado vacío, busy/error | Harness; no sesión/acción duplicada, error recuperable y siguiente solicitud explícita aprobada |
| Timeout STT/TTS | Harness existente; estado recuperable, etapa diagnosticada y respuesta escrita conservada para TTS |
| Cancelación rápida y nueva solicitud | VoiceReliabilityTest; sesión antigua no sobrescribe reemplazo, foco liberado |
| Segundo plano | Pruebas Activity/editor y cancelación; probe TTS de DeviceLab ahora tiene Job cancelable, también antes de PTT/demo |
| Pérdida/retorno de audio focus | Harness: cancelar/liberar, denegación identifica AUDIO_FOCUS; recuperar foco no reinicia reconocimiento |
| TTS sin voces/init/síntesis | Catálogo real emulador sin servicio; inicialización acotada a5s; listener instalado antes de speak, selección solo local es-ES. Fallos de síntesis/permiso probados con fakes |
| Nueva solicitud tras fallo | Harness cubre errores y vuelta a IDLE; no se retiene bloqueo de sesión |

Correcciones nativas: limpieza STT siempre en main, dueño por sesión y cierre idempotente;
cancelar resuelve la continuación. El finally viejo no destruye el reconocedor nuevo.
TTS usa dueño por utterance, evita que cancelación tardía detenga reemplazo y acota init.
Coordinator conserva respuesta terminada al cancelar/fallar audio, incluso SecurityException
TTS; limpia cada proveedor/foco aunque otro cleanup falle. Duraciones usan nanoTime.
No se ha implementado escucha permanente o servicio de micrófono en background.

Motor local/sistema OEM, acústica es-ES, llamadas entrantes y recuperación real de foco:
PENDING. La prueba guiada de DEVICE_LAB_PROCEDURE exige confirmación humana audible;
un callback nunca se registra como percepción acústica verificada.

## E. Bluetooth OBD

Suite BluetoothObdTest repetida: radio ausente/apagada, permisos/revocación, timeout,
respuesta incompleta/malformada, NO DATA/PID ausente, desconexión, reconexión limitada,
cierre y retorno al simulador; TTL de datos obsoletos en VehicleContextEngine.
El adaptador Android además invalida una apertura pendiente si se cancela antes de
crear/asignar el socket; worker omite trabajos ya cancelados. Estado no emparejado
se rechaza por bondState, inspeccionado sin conexión real.

Vocabulario cerrado de inicialización ELM y mode01 para RPM/velocidad/refrigerante/
combustible/capacidades. Sin CAN arbitrario, escritura ECU, borrar DTC, comandos del
usuario o socket del LLM. No se inició conexión física; no teléfono/adaptador/coche.
Los fakes validan contratos, no RFCOMM físico ni compatibilidad de un ELM327.

## F. IA y Places

Suites ConversationProviderTest/PlacesScenarioTest/ScenarioTest y pruebas de plataforma
repetidas: consentimiento, HTTPS/loopback debug limitado, token Keystore, texto/tools,
HTTP/timeout/cancelación, revocación, permiso coarse, categoría/resultados/selección
temporal, navegación simulada, proveedor ausente/error y ubicación inválida/caducada.
Keystore real y transporte HTTP contra servidor de prueba local pasan en emulador.

Se corrigió carrera Places: tras esperar ubicación se revalidan generación, entorno
detenido, consentimiento y permiso **antes** de consultar proveedor o guardar fix.
Un fallo tardío de una búsqueda reemplazada no destruye estado nuevo. Dos regresiones
deterministas prueban revocación durante espera y fallo de generación antigua.

IA/Places siguen opcionales; no se configuró clave/cuenta/cuota ni se hizo consulta
comercial. Prueba real de Google Places/GPS/Maps/Waze: PENDING. Sin ETA ficticia.
No se incluyen credenciales, coordenadas precisas, contactos, VIN o conversaciones
en informes exportados. Permisos/network policy/ToolRegistry/PolicyEngine sin ampliación.

## G y J. Compatibilidad y evidencia de emulador

Windows con JDK17 y SDK/build-tools35. adb devices -l: solo un emulador autorizado,
Android15/API35, Android SDK built for x86_64. AVD CopilotDrive_API35 descubierto
usando el AVD_HOME local. No se recopilaron seriales físicos ni identificadores del coche.

| Tamaño de display (px) | dpi | Fuente | Resultado |
|---|---:|---:|---|
| 800×480 | 160 | 1 | EMULATOR_TESTED |
| 1024×600 | 160 | 1 | EMULATOR_TESTED |
| 1280×720 | 160 | 1 | EMULATOR_TESTED |
| 1920×1080 | 160 | 1 | EMULATOR_TESTED |
| 360×640 | 160 | 1 | EMULATOR_TESTED |
| 1280×800 | 160 | 1 | EMULATOR_TESTED |
| 800×480 | 160 | 1.5 | EMULATOR_TESTED |
| 1280×720 | 240 | 1 | EMULATOR_TESTED |
| 800×480 | 160 | 2 | EMULATOR_TESTED |
| 1080×1920 | 480 | 1.5 | EMULATOR_TESTED |
| 640×360 | 160 | 1 | EMULATOR_TESTED |

Runner verifica bounds/no intersección de PTT, Ajustes, origen y micrófono; hash
de layout estable tras force-stop por configuración. Son píxeles totales; insets y
barras reducen área útil, no equivalen a dp de Compose. Capturas privadas no exportadas.
Guardar/cancelar/preset/restaurar/recrear/preview/background siguen cubiertos por
DashboardCustomizationTest, motor puro y DataStore. Driver/Media/Classic/Laboratory y
tamaños compactos/expandidos están probados por suites; no cada combinación de display
físico/preset. No se añaden funciones de editor ni edición en marcha.

Smokes extras aprobados: editor completo adb1/1; rotación real observada 90° a800×1280
y vuelta0° a1280×800 sin force-stop; HOME/foreground con layout estable; consulta local
offline1/1 con Wi-Fi/datos desactivados y sin proveedor IA. Restauración de display,
fontScale/rotación y red verificada. Sin FATAL EXCEPTION para el proceso final observado.
No equivale a restauración de estado por LMK ni campaña prolongada.

Evidencia local ignorada: artifacts/sprint7/{discovery,apk-inspection,dry-run,install,
screens,performance,recovery,test-summary}.json; logs baseline/final, editor-smoke y
apk-signature.txt. XML originales en android/**/build. Los informes numéricos incluyen
APK/commit/modelo/API/precondiciones/método; el recovery usa el mismo SHA-256.
Todos los dispositivos/integraciones físicas permanecen PENDING en
PHYSICAL_VALIDATION_MATRIX y COMPATIBILITY_MATRIX.

## H. Rendimiento

20 muestras reales de este emulador/APK debug, proceso frío y cachés calientes:

| Métrica | p50 | p95 |
|---|---:|---:|
| TotalTime Activity (ms) | 2732 | 3417 |
| Dashboard observable, cota superior con adb/UIAutomator (ms) | 5995 | 6788 |
| TOTAL PSS (KiB) | 84848 | 85369 |

PERFORMANCE_BASELINE contiene las20 muestras, condiciones, método y herramientas
oficiales investigadas. Cota UI no es TTFD. Sin FPS/consumo/recomposiciones/latencia
de voz física medidos, sin objetivos arbitrarios ni optimización amplia sin comparación.

## I. Pruebas

Regresiones trazables (observación previa por inspección; resultado posterior automatizado):

| Registro | Antes → después | Test |
|---|---|---|
| R7-VOICE-01 | Cancelar TTS sustituía respuesta → conserva texto y permite reintento | VoiceReliabilityTest.cancellationWhileSpeakingPreservesAnswerAndNewRequestRecovers |
| R7-VOICE-02 | SecurityException TTS usaba mensaje STT → preserva respuesta y etapa TTS | VoiceReliabilityTest.ttsPermissionFailureNeverErasesAnswer |
| R7-VOICE-03 | Fallo de cleanup podía omitir release de foco → limpieza independiente | VoiceReliabilityTest.cleanupExceptionsCannotSkipRemainingResourcesOrBlockRecovery |
| R7-PLACES-01 | Revocación durante ubicación podía consultar → cero llamadas de proveedor | PlacesScenarioTest: revocation while obtaining location prevents any provider request |
| R7-PLACES-02 | Fallo antiguo podía sobrescribir clear → mantiene sesión nueva | PlacesScenarioTest: late provider failure cannot overwrite cleared session |
| R7-A11Y-01 | Controles sin nombre/contexto → nombre, estado y tarjeta identificados | AccessibilityTest.settingsControlsHaveAccessibleNamesAndDisabledContextExplainsWhy / editorActionsIdentifyWidgetAndCurrentSize |

Baseline sesión130/130 (12 Python,33 core,56 testing,6 app,23 instrumentadas).
Final161/161: **23 Python +38 core +64 testing +6 app +30 instrumentadas**, sin fallos,
errores u omitidas. Lint0 errores/12 avisos existentes. Ambos APK y build correctos.
No sumar smokes/repeticiones al número de casos distintos. Gradle reutilizó resultados
UP-TO-DATE cuando correspondía; XML y comandos exactos en TEST_RESULTS.
`check_repository.py` y `git diff --check` aprobados. Seguridad no relajada.

Un intento redirigido abortó por aviso SDK interpretado como NativeCommandError en
PowerShell5; no se contabiliza como aprobado. Wrapper corregido y compilación repetida.
Prueba negativa intencional de task inexistente: BUILD FAILED/exit1, preservación del
fallo comprobada. El aviso SDK XMLv4/v3 sigue siendo visible; no se aceptaron licencias.

## K. APK

`artifacts/CopilotDrive-AI-sprint7-debug.apk`, **19.825.143 bytes**.
SHA-256: `F85A4C1252A5C8A700171CBBF8CFA2BEFBCEA33CB5FAF093C1EA8D03030F0D01`.
Paquete ai.copilotdrive.app, minSdk26/target35; firmado Android Debug, un firmante,
APK Signature Scheme v2 verificado por apksigner. Certificado SHA-256:
`AD561B54E005E27E4CA2873963EE7F6C34C4E246FEB9EA76AA4CC2E2EB476A8A`.
Instalación/launchMainActivity/hash instalado comprobados en emulador autorizado.
Dashboard/editor/DeviceLab/Jarvis offline/estados sin proveedores cubiertos como arriba.
Artefactos ignorados por Git. No APK release, firma de producción o publicación.

## L. Limitaciones y riesgos

Sin teléfono ni adaptador/vehículo: certificación física abierta. Semántica/fakes y
hardening por inspección no prueban los motores OEM, acústica, foco nativo o socket
real. API35 es la única API ejecutada; minSdk no certifica todas las versiones.
Debug/emulador no predice release/hardware. No LMK real, horas de estabilidad,
energía, auditoría CVE completa ni servicios comerciales. El finally restaura en
fallos normales; un cierre abrupto del host requiere inspección manual del emulador.
Véanse KNOWN_LIMITATIONS, ACCESSIBILITY_AUDIT y PERFORMANCE_BASELINE.

## M. Commits locales

- `9b04a47`: especificación y fronteras de evidencia.
- `561c912`: contratos, runner, accesibilidad y hardening de proveedores/voz/Places/OBD.
- `eb6fe09`: cancelar probe TTS antes de PTT, temporales UI y exit codes de Gradle.
- `f5613b5`: smoke reproducible de rotación/foreground/offline y restauración probada.
- Commit documental final: consultar git log; solo documentación/evidencia, sin APKs.

## N. Sprint 8 priorizado

P0: teléfono autorizado, TalkBack humano/fuentes/insets y es-ES acústico/foco; después
ELM327/coche detenido autorizado. P1: benchmark cercano a release en teléfono,
TTFD/frames/memoria sostenida, luego IA/Places con cuota y consentimiento separados.
P2: reproductores/Maps/Waze/GPS y LMK/recuperación OEM/API26+. Detalle en NEXT_STEPS.
No abrir edición en conducción, micrófono permanente o contexto vehicular para IA.

## O. Publicación de PR, pendiente de autorización

Revisar `git diff main...HEAD`, commits, informes y hashes. Después de autorización
explícita de push, ejecutar `git push -u origin feat/sprint7-device-qualification` y
crear PR a main con este informe y TEST_RESULTS; adjuntar APK/evidencia redaccionada
solo si se desea. Esperar Android/Python CI propios de esta rama. Merge y release
son decisiones posteriores separadas. Ninguno de esos pasos remotos se ejecutó aquí.

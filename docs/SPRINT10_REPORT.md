# Sprint 10 — Physical Android Pilot & Jarvis Voice Qualification

2026-10-20, Europe/Madrid. Candidata debug para revisión e instalación controlada.
**Calificación física PENDING**: no teléfono disponible/autorizado. Sin push, merge,
release, firma de producción ni peticiones a proveedores comerciales.

## A. Git, base y auditoría

Checkout real: `OpenCar-Copilot-MVP/opencar-copilot`. Árbol inicial limpio, rama
Sprint9 preservada y worktree histórico sprint6-baseline intacto. `git fetch origin`
ejecutado; `main` avanzó exclusivamente fast-forward desde504e9a7 hasta
`793a3535713cd76cfbe6c85f9fbfb7bff21cb4b5`. Nueva rama `feat/sprint10-physical-pilot`.
No reset, clean, force push ni eliminación de datos. Reportes Sprint7/8/9,
AGENTS, specs014/015/016 y documentación de calificación/performance/memoria/seguridad
revisados; las incidencias previas condicionaron aislamiento y uso de recursos.

[PR #10 merged](https://github.com/AlmanInDaHouse/CopilotDrive-AI/pull/10), commit
canónico confirmado. Workflows del mismo merge consultados por API:
[Android success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39454974296),
[Tests success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39454974344).
SPRINT9_REPORT incluye nota histórica del merge; no se reescriben sus observaciones.
Estos workflows no son CI de Sprint10, que no se ha publicado.

Baseline Sprint10 ejecutado: Python45, core43, testing67, app6 y34 instrumentadas
del APK Sprint9 en API26.195 casos distintos; no se repitió baseline en todas las
APIs para sumar ejecuciones. Gradle baseline forzado89/89 tareas, build/lint/debug y
AndroidTest aprobados. Evidencia local ignorada: artifacts/sprint10/baseline-*.

## B. Dispositivos y preservación

ADB localizado en `C:/Users/manul/AppData/Local/CopilotDriveAndroidSdk/platform-tools/adb.exe`.
Descubrimiento pasivo inicial: un emulador autorizado `CopilotDrive_API35`, Android15/
API35, modelo Android SDK built for x86_64,1280×800/160dpi/fontScale1.0; ningún
teléfono. No se exportan seriales físicos. Se sigue el camino B de la misión.

AVDs oficiales existentes API26/30/35 utilizados secuencialmente, solo uno activo.
La suite completa reemplaza fixtures/credencial/layout/listener: solo AVDs
`CopilotDrive_Sprint9_API26/30/35`. Instalación-r y am instrument, sin connected
Gradle/uninstall/data clear. No descargas SDK, licencias, snapshots solicitados,
reboot ni modificación de config.ini. API35 puede elevar RAM solicitada; no se
usa la cifra solicitada como medición de memoria efectiva ni benchmark.

Retorno al AVD original confirmado PRESERVED: APK histórico C23F…0EA2,
preferencias8b18dd…bc63d y layout0abf70…3a0a1 idénticos; display/dpi/fontScale y
nombre del AVD también coinciden. protected-avd.json conserva antes/después.
El AVD valioso mantiene deliberadamente su APK previo; la candidata se instaló solo
en los tres AVDs desechables. No se afirma restauración exacta de un proceso vivo.
Descubrimiento pasivo final: un emulador autorizado y ningún teléfono, en
final-discovery.json. Ninguna instalación o prueba activa en teléfono físico.

## C. Implementación y correcciones

- specs/017-physical-pilot define los ocho contratos pedidos, acta vacía, aceptación
  y plan. Core reutiliza DeviceQualification/CompatibilityStatus/EvidenceLevel;
  sesión/build/API/modelo, intento, escenario, referencia y fecha coherentes obligan
  a distinguir automatización, emulador y observación humana. Ausencias quedan PENDING.
- Device Lab: frase acústica guiada, foco/timeout/cancelación y confirmación humana
  independiente. Una repetición invalida el intento anterior; confirmación obsoleta
  se ignora. Solo RAM/sesión, sin grabación ni exportación automática.
- VoiceJourneyStage cerrado en coordinador y ToolRegistry: etapas y decisiones de
  política sin transcripciones, argumentos, secretos, contactos, ubicación o razonamiento
  privado. Hasta32 etapas borrables. Se conserva respuesta normal en dashboard.
- AcousticProbe captura timeout antes de CancellationException y su limpieza antigua
  no cierra un reemplazo. Salir/background/PTT cancelan. El timeout del coordinador
  identifica la operación real; no clasifica todo timeout previo a TTS como STT.
- Preflight pasivo `pilot_package.py`: firma/hash/SDK/package/permisos, copia temporal
  del APK previo para comparar certificado, bloqueo conservador ante conflicto;
  nunca instala ni lanza o concede permisos. Ejecución positiva comprobada en emulador.
- Se detectó ACCESS_FINE_LOCATION transitivo de Places tanto en el APK Sprint9 como
  en el primer Sprint10. tools:node=remove lo excluye del APK final, conserva coarse
  propio y añade regresión sobre permisos realmente empaquetados. Auditoría previa
  del manifest fuente no garantizaba el manifest fusionado. No se observó concesión
  de FINE ni se afirma fuga de datos. [Referencia oficial](https://developers.google.com/maps/documentation/places/android-sdk/legacy/current-place#permissions).
- Runner API26 acotado, estados originales persistidos, ConnectivityService verificado,
  alternativa Settings sin privilegios y restauración completa del snapshot agregado.

## D–E. STT/TTS y acústica humana

Flujo existente intacto: micrófono → STT es-ES → coordinador → orquestador →
ToolRegistry/AIProvider opcional → texto → TTS. Servicio local preferido; fallback
del sistema solo con consentimiento. Permiso al pulsar,15s STT/20s TTS, sin escucha
permanente. Respuestas escritas sobreviven fallo/cancelación de audio.

Harness: silencio/vacío/errores/timeout/cancelación/foco/revocación/dos solicitudes y
recuperación existentes conservados. Nuevos tests: callback≠audibilidad, confirmación
negativa/obsoleta, timeout del probe, cleanup tardío, recuperación y etapas privadas.
La UI sintética verifica controles, no audio. Confirmación humana real: **ninguna**.
STT captado y audio audible en teléfono: PENDING; no grabaciones ni conversaciones
utilizadas como evidencia. Recorrido concreto en VOICE_QUALIFICATION y guía del piloto.

Limitación observada: motor `com.svox.pico` de imagen API26, SIGSEGV en SynthThread/
libttspico (picobase_get_next_utf8char). Provocó diálogos durante suites/consultas.
El texto local y la instrumentación aprobaron; el motor no se declara aprobado.
Se cerraron solo los diálogos conocidos en AVD desechable para preparar offline;
no se deshabilitó motor, modificó librería del sistema o inventó confirmación acústica.

## F. OBD físico

PENDING. No teléfono/ELM327/vehículo disponible y autorizado para esta campaña.
No escaneo, emparejamiento, conexión o PIDs físicos iniciados. Fakes/regresiones de
BluetoothObd conservadas. Selección/confirmación manual y vehículo detenido siguen
obligatorios; solo lecturas allowlisted, sin VIN, CAN arbitrario, ECU writes,
borrado DTC, comandos intrusivos ni inicio automático. Guía preparada para válidos,
NO DATA/PIDs no soportados, latencias, desconexión y retorno al simulador.

## G. API26 offline

Reproducción SVC con APK baseline: wifi disable/enable137, data enable0. Framework
disponible y MemAvailable guest≈1GiB. Log identifica SecurityException por falta de
CHANGE_WIFI_STATE del shell; fallo posterior de manejo Binder. dmesg denegado,
pm check-permission/cmd connectivity sin soporte útil. No OOM inferido ni root/grants.
[AOSP](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-8.0.0_r1/cmds/svc/src/com/android/commands/svc/WifiCommand.java)
y trazas del guest fundamentan el límite de la ruta SVC; no una causa universal de137.

Primer Settings bloqueado por overlay Pico; se conservan intentos y restoration=FAILED
de comandos aun con flags originales observados. Alternativa final: interruptor real
de Ajustes y svc data. APK final,1/1 en2280ms. Antes y después de consulta: flags0/0,
sin red predeterminada,0 redes conectadas. Restauración1/1, default activo y2 redes
conectadas igual al snapshot inicial: RESTORED. `api26-offline-final.json`.

A: proveedor AI fake indisponible/0 llamadas = AUTOMATED_TESTED.
B: Android realmente desconectado durante consulta local escrita = EMULATOR_TESTED,
PASS con Settings. SVC Wi-Fi de esa imagen = UNSUPPORTED; intento SVC BLOCKED preservado.
No prueba STT/TTS offline audible. Investigación completa en API26_OFFLINE_INVESTIGATION.

## H. Rendimiento y memoria

Ninguna campaña nueva de benchmark ni trazas adicionales. Host inicial326/367/574MiB
libres; otra observación122–141MiB libres,529–538MiB headroom y paging sostenido.
Guardas HOST_PHYSICAL_MEMORY_PRESSURE. Se detuvo un daemon Gradle propio inactivo;
sin cerrar apps ajenas ni modificar pagefile. No builds durante campañas de medida
porque no se inició ninguna. Builds y suites funcionales no se interpretan como
mediciones de rendimiento. Se conservan jank/PSS inconclusos de Sprint9 sin atribuir
mejoras, extrapolar renderer al teléfono o hacer optimizaciones especulativas.
Rendimiento físico PENDING; medición en este host BLOCKED. Sin heap/dominadores nuevo.

## I–J. Interoperabilidad y accesibilidad

MediaSession fixture y revocación, intents, contactos/confirmación, Places demo/
consentimiento y AI fake/loopback siguen probados automáticamente; efectos en apps
reales y servicios comerciales PENDING. No credencial/clave comercial configurada,
consulta con cuota, telemetría/contactos/coordenadas enviada a LLM, llamada automática,
ruta activa o ETA declarada. No se añadió servicio comercial a CI.

AccessibilityTest/DashboardAdaptiveTest y controles nuevos de confirmación ejecutados
en suites instrumentadas; semántica/headings/estados/diálogos/fuente grande/horizontal
son comprobaciones automáticas. TalkBack manual, orden pronunciado/foco OEM e insets
físicos PENDING. Sin instalación ni cambio de servicios de accesibilidad. Consultas
pasivas del paquete TalkBack en API30/35: no instalado (pm path sin paquete/exit1),
registrado sin modificar servicios. La primera consulta API26 se interrumpió por
el wrapper al recibir exit1; no se usa como prueba adicional. Guía incluye recorrido
humano de respuesta extensa, voz, editor, Ajustes y permisos.

## K. Regresiones y resultados

**215 casos distintos aprobados**,0 fallos/errores/omitidas.52 Python +47 core JVM
+73 testing JVM +6 app unit +37 instrumentados. Las37 se ejecutaron por cada API:
111 ejecuciones finales, no111 casos distintos. Smoke offline1/1 es repetición de un
caso existente; no se suma. Baseline y pruebas intermedias tampoco se suman al total.

| Suite final | Casos | Resultado/evidencia local |
| --- | ---: | --- |
| Python unittest | 52 | PASS, final-python.log |
| Core JVM | 47 | PASS, XML y final-android-verified.log |
| Testing JVM | 73 | PASS, XML y final-android-verified.log |
| App unit | 6 | PASS, XML de testDebugUnitTest |
| Android API26 | 37 | PASS,49396ms, final-api26.json |
| Android API30 | 37 repetidos | PASS,50319ms, final-api30.json |
| Android API35 | 37 repetidos | PASS,73379ms, final-api35.json |
| API26 offline B | 1 repetido | PASS,2280ms, radios/ConnectivityService/restore verificados |
| Lint | — |0 errores/12 avisos existentes |
| Build debug + AndroidTest | — | PASS, final-package-build.log,36s |
| Repository + diff check | — | PASS |

Mismo APK `fcec…643c` y testAPK `9f97…2405` antes/después en las tres APIs.
Listener multimedia RESTORED en las tres. API30/35:0 crash/0 ANR nuevos observados
en historial acotado exit-info de las suites; no garantía fuera de esa ventana.
API26:contadores null/UNSUPPORTED_API; assertions funcionales aprobadas, Pico externo
falló como se describe arriba. MainActivity/dashboard/Jarvis local/Device Lab están
cubiertos por suites ejecutadas, además de controles guiados sintéticos y permisos.
Gradle reutilizó tareas sin cambios donde procedía; los nuevos tests core/testing se
habían ejecutado en la tanda anterior y los nuevos instrumentados en las tres APIs.
No se cuenta el harness preparado como prueba física ni se omiten resultados negativos.

Los logs originales de
compilación de tests nuevos con referencias/trailing lambda incorrectos se conservan;
se corrigieron y repitieron, sin desactivar casos ni políticas. Preparación inicial
usó una ruta de emulator inexistente, corregida al binario local descubierto. El
error de codificación de salida de consola al leer un rótulo Wi-Fi se corrigió sin
cambiar el comportamiento Android. Ninguno se cuenta como prueba aprobada.

## L. APK y paquete

APK: `artifacts/sprint10/CopilotDrive-AI-sprint10-debug.apk`,19744628 bytes.
Código Android y runners congelados en `b157bc3be9da0fd6a79ada6e92b48696441e009f`;
workingTreeDirty de las ejecuciones corresponde a documentación en curso, sin cambio
Android posterior al build. Package ai.copilotdrive.app; minSdk26/target35,
versionCode1/versionName0.2.0-debug, firma Android Debug v2 verificada.

SHA-256 APK:
`fcec5256a0c0911eefe22ca2492db45a9374bcb45ace81ba9269ae664b7e643c`

Certificado SHA-256:
`ad561b54e005e27e4ca2873963ee7f6c34c4e246feb9ea76aa4cc2e2eb476a8a`

AndroidTest SHA-256 (solo AVDs desechables):
`9f9707ef911a416b31ebab9ddc23484c21d8c07f362d4f2fab5e471e82812405`

Permisos reales: RECORD_AUDIO, READ_CONTACTS, INTERNET, ACCESS_COARSE_LOCATION,
BLUETOOTH(max30), BLUETOOTH_CONNECT, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE y
ai.copilotdrive.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. Sin FINE/BACKGROUND/SCAN.
Manifest/badging/firma conservados en apk-*.txt. Clave Places vacía en BuildConfig,
archivo APK sin keystore/env/local.properties y repository checks aprobados. Es una
revisión acotada de secretos/política, no auditoría exhaustiva de seguridad/CVE.
Launcher MainActivity, Activities de tooling/test solo debug, receiver protegido
y MediaAccessService no exportado según manifest fusionado. No release/clave de producción.

Paquete local `CopilotDrive-AI-sprint10-pilot.zip`: APK, guía, matriz de voz, acta vacía,
metadatos, firma y SHA256SUMS; no AndroidTest, logs privados ni trazas crudas. La guía
explica dispositivo/RSA, preflight/firma, autorización antes de instalar, permisos,
STT/TTS español, conversación, multimedia, errores y restauración. No instalación física.

## M–N. Evidencia, limitaciones y readiness

| Área | Estado |
| --- | --- |
| Contratos, voz/fakes, privacidad y gates | AUTOMATED_TESTED según suites finales |
| UI/permisos/APIs y offline26 escrito | EMULATOR_TESTED solo para ejecuciones registradas |
| SVC Wi-Fi26 | UNSUPPORTED; intento BLOCKED con SecurityException |
| Pico TTS26 | FAILED observado en motor externo; audio no calificado |
| Acústica humana/STT físico/TalkBack | PENDING |
| OBD/vehículo e interoperabilidad física/comercial | PENDING |
| Nueva atribución/rendimiento | BLOCKED por host; físico PENDING |
| Firma de producción/publicación | PENDING, fuera de esta entrega |

PHYSICAL_VALIDATION_MATRIX no promueve ninguna fila a PHYSICALLY_VERIFIED. Aceptación
de revisión técnica y calificación física son independientes. Datos ausentes nunca
se rellenan con0/PASS. Se conservan fallos, versiones intermedias y limitaciones.

## O–Q. Commits, siguiente sprint y PR

Commits locales:9dc2100 (especificación),5ea607f (voz/contratos/UI),138ae44 (runners/
preflight/regresiones) yb157bc3 (permiso transitivo/restauración completa). El commit
documental final registra evidencia; consultar git log para su hash. APKs/artifacts
ignorados, sin push/merge/release. Estado Git final comprobado tras ese commit.

Sprint11 recomendado: teléfono consentido para STT/TTS/acústica humana y TalkBack;
después reproductores/Maps/Waze/contactos ficticios, ELM327/coche detenido y mediciones
físicas con autorización independiente/condiciones válidas. IA/Places solo con proyecto,
credencial y cuota autorizados. No ampliar IA vehicular o escucha permanente.

PR posterior: revisar `git diff main...HEAD`, tabla de pruebas, hashes y limitaciones.
Solo tras autorizar publicación, `git push -u origin feat/sprint10-physical-pilot`;
abrir [comparación](https://github.com/AlmanInDaHouse/CopilotDrive-AI/compare/main...feat/sprint10-physical-pilot?expand=1)
a main, adjuntar este informe y esperar Android/Tests de la rama. No ejecutado.
No confundir CI aprobado de la base con CI de Sprint10. NEXT_STEPS detalla el trámite.

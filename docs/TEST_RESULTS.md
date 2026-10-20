# Resultados ejecutados — 2026-10-20

## Sprint 6 — rama local `feat/sprint6-custom-dashboard`

Base canónica `2099b9d` verificada por fetch; main sincronizado por fast-forward.
Baseline aislado sobre esa base: Python **12/12**, core **17/17**, testing **56/56**,
app **2/2**, lint y APK debug/test correctos. La redirección inicial de logs PowerShell
abortó por un aviso SDK en stderr; el baseline completo se repitió sin esa redirección.

Versión final funcional `b941c2a` (harness adicional `fc69927`): Python **12/12**, core **33/33**, testing **56/56**,
app JVM **6/6**, instrumentadas API 35 **23/23**: **130/130**, sin omitidas.
`:app:lintDebug`: **0 errores, 12 avisos**; `assembleDebug` y
`assembleDebugAndroidTest`: **BUILD SUCCESSFUL**. `check_repository.py` y
`git diff --check`: correctos. Sin nuevos permisos, dependencias, proveedores ni tools LLM.

Editor: añadir, ocultar, mover, tamaño, preview, guardar, cancelar, presets,
restauración, Activity recreate, revocación, background y no solape geométrico de
los controles fijos; layout engine cubre
tipos/tamaños/posiciones inválidos, corrupción, migración, solapes, duplicados,
cancelación IO, simultaneidad, UNKNOWN, movimiento y TTL con reloj determinista.
DataStore real conserva documento al reabrir archivo. Pruebas anteriores de voz,
OBD fake, MediaSession, privacidad, Places demo, IA loopback y Keystore pasan.

Smoke final: instalación adb y MainActivity, flujo editor repetido para capturas,
ocho configuraciones reales del display emulado (800×480, 1024×600, 1280×720,
1920×1080, 360×640, 1280×800 a 160 dpi; 800×480 fontScale 1.5; 1280×720 a 240 dpi),
controles fijos dentro de bounds y layout conservado tras force-stop/reinicio.
Consulta local de combustible con Wi-Fi/datos desactivados: **1/1**, 42 % simulado.
Sin FATAL EXCEPTION en el buffer de crashes del smoke. Se restauraron resolución,
densidad, fontScale y red. Son pruebas de emulador, no de hardware.

APK final: `artifacts/CopilotDrive-AI-sprint6-debug.apk`, **19.825.143 bytes**,
SHA-256 `1FD9B5C3682896BA9100F70C9AAC0853063DA5BC6F8DC2CED117EECD66707F91`.
`apksigner verify --verbose --print-certs`: firma debug v2 válida, un firmante.
Evidencias locales ignoradas: `artifacts/sprint6/` (capturas, XML, JSON, firma,
smoke offline y crash log). Detalles, presets, tamaños dp vs píxeles y límites en
`SPRINT6_REPORT.md`. Sin mediciones de FPS/consumo/latencia del editor; sin nueva
validación física de voz, ELM327, GPS, Maps/Waze, Places o pantallas.

## Sprint 4 — rama local `feat/sprint4-hardware-validation`

Tras el commit funcional `8b7522d`, se ejecutaron en Windows/JDK 17/SDK 35: Python unittest **12/12**; JVM core **16/16**, testing **44/44**, Android unit **2/2**; Android instrumentado en emulador API 35 **14/14**. Total: **88/88**. El último comando Gradle conjunto (`:core:test :testing:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`) terminó `BUILD SUCCESSFUL`. Android lint: **0 errores, 10 avisos**; `check_repository.py` y `git diff --check` aprobaron.

La instrumentación incluyó apertura del Device Lab, checks pasivos, borrado, respuesta escrita local sin TTS, servidor Chat Completions loopback 200/401/429/503 y regresiones anteriores. El APK debug se instaló y MainActivity arrancó en el emulador Android 15. APK final: `artifacts/CopilotDrive-AI-sprint4-debug.apk`, SHA-256 `354A35C2F053211EAEC4EB24BF106D7DB0CF919D26C3DE06A649DF9A4550D929`, firma debug v2 verificada. Ver `SPRINT4_REPORT.md` para alcance y límites. Ningún teléfono, adaptador, vehículo ni endpoint comercial ha sido validado físicamente.

## Sprint 2 — rama local `feat/sprint2-voice-llm`

Baseline antes de editar: Python 12/12, targets Gradle `:core:test :testing:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` aprobados y `check_repository.py` aprobado. `main` `b3646df` tiene [Android run #6](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39355399788) en **Success** con dos artefactos; es evidencia del código anterior a Sprint 2.

Validación final local Windows/JDK 17/SDK 35:

- Python unittest: **12/12**.
- JVM: core **13/13**, testing **30/30**, app **2/2**; total **45/45**. Diez tests nuevos de ConversationProviderTest y una regresión de timeout TTS cubren respuesta de texto y herramientas, JSON inválido/truncado, 401/429/500/302, TLS, consentimiento, cambio de configuración, cancelación, memoria, aislamiento de telemetría, fallback local y respuesta visual intacta.
- Instrumentación Android 15/API 35, emulador `CopilotDrive_API35`: **11/11**. Incluye prueba de cifrado Keystore, catálogo TTS y Chat Completions HTTP contra servidor loopback en el mismo emulador, sin credencial ni API comercial.
- Lint debug: **0 errores, 10 avisos**. Assemble debug: **BUILD SUCCESSFUL**. `python scripts/check_repository.py`: aprobado. `git diff --check`: sin errores.
- APK debug: `artifacts/CopilotDrive-AI-sprint2-debug.apk`, **11,762,683 bytes**, SHA-256 `67C2CAD8029F713E1B0C51578510B3E2D6C890C145838B048E976D87C910CD70`; firma debug v2 verificada, instalación `adb install -r` exitosa.

Total automatizado final: **68/68** (12 Python + 45 JVM + 11 instrumentadas). La prueba HTTP loopback acredita el transporte y parser Android con una respuesta Chat Completions simulada. No acredita interoperabilidad comercial, voz acústica, motor STT externo ni calidad de síntesis. En el emulador API 35 el diagnóstico TTS observado fue «Motor TTS ausente o falló la inicialización» y **0 voces locales es-ES**; no se descargaron voces.

La corrección del XML de red debug y la lectura del cuerpo del servidor de prueba exigieron repetir la instrumentación; la ejecución final citada arriba pasó. Los reportes originales están bajo `android/**/build/test-results` y `android/app/build/outputs/androidTest-results`.

## Histórico: corrección posterior a la PR #1 (`fix/android-ci`)

La PR #1 se fusionó en `main` como `42a3c90` (MVP `6e4cf95`). En esa revisión,
CI Python terminó en SUCCESS y CI Android falló antes de Gradle, en
`android-actions/setup-android@v3`: `Warning: Failed to find package 'tools'`.
La rama de corrección cambió a `@v4`; posteriormente PR #2 se fusionó y el workflow de `main` completó con éxito.

Validación local de esta corrección, Windows/JDK 17/SDK 35:

- `python -m unittest discover -s tests -v`: **12/12**.
- `python scripts/check_repository.py`: aprobado; incluye el límite app → testing.
- `:core:test :testing:test :app:testDebugUnitTest`: **34/34** (dos pruebas nuevas de `DemoMediaAdapter`).
- `:app:lintDebug`: **0 errores, 8 avisos** sobre versiones de dependencias.
- `:app:assembleDebug :app:assembleDebugAndroidTest`: BUILD SUCCESSFUL; APKs generados.
- `:app:connectedDebugAndroidTest`: **8/8**, emulador Android 15/API 35 x86_64.
- `git diff --check`: sin errores.

Total de esta ejecución local: **54 pruebas aprobadas**. El workflow conserva el job manual
`device-tests` para repetir la instrumentación en GitHub Actions. La publicación del APK
como artefacto solo quedará acreditada cuando termine correctamente una ejecución remota.

## Evidencia histórica del MVP antes de la fusión

Código Android validado entonces: commit d849c3e (rama feat/android-mvp). Esta sección conserva la evidencia local anterior a la PR #1.
Entorno: Windows, Python 3.13.14, Microsoft JDK 17.0.20.1+1, Gradle 8.11.1, AGP 8.9.2,
Kotlin 2.2.0, SDK 35. Emulador AOSP Android 15 API35 x86_64, aceleración WHPX, 1280×800 / 160 dpi.

## Baseline y regresiones Python
`python -m unittest discover -s tests -v`
- Baseline: 6/6, 0.002 s.
- Cuatro regresiones se reprodujeron antes de corregirlas: RPM truncada, JSON no objeto, dato desconectado
  y omisión de simulación hablada. Resultado inicial: 3 fallos + 1 error.
- Final: **12/12**, 0 fallos, 0 errores, 0.589 s en última ejecución.

## JVM
`:core:test :testing:test :app:testDebugUnitTest`
- ContractTest: 6 pruebas.
- MediaSelectionTest: 3 pruebas.
- SharedFixturesTest: 2 pruebas.
- ScenarioTest: 19 pruebas.
- DashboardPresentationTest: 2 pruebas.
- Total: **32/32**, 0 fallos y 0 errores.
Incluyen fuente real/fake, datos caducados, desconocidos y desconectados; ausencia de autonomía;
serialización y esquema; equivalencia OBD Python/Kotlin; permiso denegado/revocado; controles inexistentes;
solicitud a reproductor equivocado; ECU write y parámetros extra; confirmación única/ambigua/caducada;
URI hostil; timeout, cancelación, silencio y fallo TTS. Reloj y coroutines deterministas.

## Instrumentación Android
`:app:connectedDebugAndroidTest`
- **8/8**, 0 fallos, 0 errores, 0 omitidos; tiempo de tests reportado: 17.516 s.
- DashboardUiTest: seis pruebas de simulación/rango desconocido, acción demo, next deshabilitado,
  onboarding y voz visible en contenedores 800×480 y 360×640.
- AppFlowTest: onboarding → botón consulta → ViewModel real → respuesta «Simulación. Combustible: 42 %».
- PlatformMediaTest: MediaSession Android real con metadatos, control PAUSE recibido, callback PAUSED,
  retirada del permiso de notification listener y limpieza de controles/metadatos.
  El permiso de pruebas se concede y revoca mediante shell del emulador; el APK no pide permisos privilegiados.

**Total de pruebas automatizadas: 52 aprobadas.**

## Build y controles
Comando reproducible realmente ejecutado desde la raíz:

```powershell
.\scripts\android.ps1 :core:test :testing:test :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug
python -m unittest discover -s tests -v
python scripts/check_repository.py
git diff --check
```

Gradle Wrapper descargó y verificó su distribución por SHA-256. Ejecución final conjunta: BUILD SUCCESSFUL,
1m33s (incluye operaciones que no son tiempo de tests). APK debug y APK instrumentado compilados.
Android lint: 0 errores y 8 avisos de nuevas versiones de dependencias. No hay baseline que oculte errores.
Se forzó además `:app:lintDebug --rerun-tasks` para renovar el informe incremental.
Checks de repositorio: permisos mínimos, cleartext/backup desactivados, core sin Android, specs completas,
campos fixture y exclusión de archivos sensibles/generados. No equivalen a auditoría CVE o pentest.

## APK e instalación
- `artifacts/CopilotDrive-AI-debug.apk`: 11,762,741 bytes.
- SHA-256: `3427cc92998ace37a63b8b60138f8bc963ea6e24c17b6ba0693cd334816c4df4`.
- `apksigner verify --verbose`: válido, firma debug v2, un firmante.
- `adb install -r` y lanzamiento de MainActivity: ejecutados correctamente.
- Revisión visual: 1280×800, 800×480 y 360×640; barra de voz fija, contenido desplazable en pantallas pequeñas.
- Logcat AndroidRuntime: sin excepciones fatales en la comprobación final.

Evidencias locales no versionadas: `artifacts/test-summary.json`, `wrapper-validation.log`,
`lint-results-debug.html`, `apk-signature.txt`, `SHA256SUMS.txt` y capturas PNG.
Reports XML originales bajo `android/**/build/test-results` y `android/app/build/outputs/androidTest-results`.

## Límites de la evidencia
La imagen AOSP usada no ofrece una voz española TTS local utilizable; la app mostró correctamente la respuesta
visual y «Voz no disponible». STT/TTS acústicos no se han validado físicamente; el flujo de voz y sus errores sí
se probaron con fakes. No se probaron un coche, Bluetooth OBD, Maps/Waze instalados, agenda física ni cuentas
de streaming. No se midieron consumo ni latencia real de voz. No declarar esos criterios completos.

## Sprint 3 — evidencia local nueva

Tras rebase sobre `origin/main` `74507bb` y corrección de frescura del simulador, se ejecutaron:

- `python -m unittest discover -s tests -v`: **12/12**.
- `scripts/check_repository.py` y `git diff --check`: **correctos**.
- `:core:test :testing:test :app:testDebugUnitTest`: **57/57** (core 13, testing 42, app 2). `BluetoothObdTest`: **12/12** con fake serial.
- `:app:connectedDebugAndroidTest` en emulador API 35: **12/12**, incluidos onboarding, herramientas locales, Keystore, HTTP loopback, MediaSession y la pantalla OBD sin adaptador seleccionado.
- `:app:lintDebug`: **0 errores, 10 avisos** (8 `GradleDependency`, 2 `UseKtx`). `:app:assembleDebug` y `:app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**.

**Total Sprint 3: 81/81** (12 Python + 57 JVM + 12 instrumentadas). APK debug: 11.762.683 bytes, SHA-256 `6f09ee0dca6d667eec93c81ef15f573817a2fbd00542fc54ef76d876d78f322c`; firma debug v2 verificada. `adb install -r` y arranque de MainActivity funcionaron. La revisión manual confirmó el simulador después de 15 s de onboarding, respuesta escrita local ante TTS no disponible, permiso Bluetooth y mensaje de ausencia de adaptadores emparejados. No se conectó un ELM327 físico. Evidencia ampliada: `SPRINT3_REPORT.md` y archivos ignorados en `artifacts/`.

## Sprint 5 — 2026-10-20, rama local `feat/sprint5-places-navigation`

Baseline remoto `main` `4fea84d`: workflows [Tests](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39365833566) y [Android](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39365833567) terminaron `success`. No hay workflow remoto para esta rama local sin push.

Validación final en Windows/JDK 17/SDK 35: `python -m unittest discover -s tests -v` **12/12**; Gradle `:core:test` **17/17**, `:testing:test` **56/56**, `:app:testDebugUnitTest` **2/2**, `:app:connectedDebugAndroidTest` en `CopilotDrive_API35` Android 15/API 35 **15/15**; total **102/102**. El comando conjunto incluyó `:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` y terminó `BUILD SUCCESSFUL`. Lint: **0 errores, 12 avisos**. `python scripts/check_repository.py` aprobó; `git diff --check` sin errores de contenido (avisos CRLF de Git en Windows).

Las 15 instrumentadas incluyeron Lugares → demo etiquetada → búsqueda fake → selección → apertura simulada → dashboard, además de flujos anteriores de OBD, voz, IA loopback y Keystore. Smoke separado **1/1**: se instaló APK, se lanzó MainActivity (`pidof` confirmó proceso), se concedió y revocó `ACCESS_COARSE_LOCATION`, se desactivaron Wi-Fi y datos en el emulador y se ejecutó la consulta local de combustible de Jarvis. `BUILD SUCCESSFUL`; Wi-Fi y datos se restauraron a su estado previo. No se conectó Google Places, GPS físico, Maps/Waze real ni un vehículo.

APK: `artifacts/CopilotDrive-AI-sprint5-debug.apk`, **19.825.143 bytes**, SHA-256 `49125CDEBFD8B1E8D8DA116A2F99180A988209EEF9C77E9A85C86C0669FF3291`. Hash idéntico al APK generado. `apksigner verify --verbose --print-certs`: `Verifies`, firma v2 `true`, un firmante debug; certificado SHA-256 `AD561B54E005E27E4CA2873963EE7F6C34C4E246FEB9EA76AA4CC2E2EB476A8A`. No es firma de producción.

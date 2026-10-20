# Resultados ejecutados — 2026-10-20

Código Android validado: commit d849c3e (rama feat/android-mvp). No resultados de CI remota: no se ha hecho push.
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

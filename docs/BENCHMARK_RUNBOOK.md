# Benchmarks locales — Sprint 8

No ejecutar dos consumidores de ADB, instrumentación, compilación o matriz al mismo
tiempo. Mantener API/modelo/resolución/densidad/fontScale y compilación constantes,
host sin trabajo nuevo, simulador onboarded y sin proveedores externos. Sin borrar
datos. Guardar intentos fallidos y muestras originales en artifacts/sprint8 (ignorado).

## Variante y herramientas

Macrobenchmark 1.3.4 estable fijado para la base SDK35/AGP8.9.2 existente; no se
afirma que sea la versión más reciente. ProfileInstaller1.4.1 solo en benchmark.
Módulo com.android.test opt-in mediante `-PenableMacrobenchmark`, minSdk26; app
derivada de release configurada actualmente sin minificación, no debuggable,
profileable solo en benchmark, firma debug, applicationId `.benchmark` separado.
No es una release de producción ni se ha generado firma de producción. La clave
Places de benchmark se fuerza vacía y usa la política HTTPS de producción.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/android.ps1 :app:assembleBenchmark :macrobenchmark:assembleBenchmark -PenableMacrobenchmark
python scripts/benchmark_android.py --serial emulator-5554 --allow-active --allow-install --dry-run --method coldStartup --iterations 20 --out artifacts/sprint8/macro-dry-run.json
python scripts/benchmark_android.py --serial emulator-5554 --allow-active --allow-install --method coldStartup --iterations 20 --out artifacts/sprint8/macro-cold.json
```

Repetir por separado warmStartup, hotStartup, dashboardScroll, settingsAndLab,
presetDraft y backgroundRestore. El runner no selecciona otros dispositivos ni
actúa sobre teléfonos. EMULATOR es la única advertencia de calidad de medición
suprimida explícitamente; no se suprimen errores debuggable/profileable/seguridad.
Los benchmarks no se suman a tests funcionales y no bloquean el CI principal.

Los runners comprueban ActivityManager: ADB y boot_completed no bastan si falla el
framework. No reinician el emulador automáticamente. Con CompilationMode.None se
exige API34+ para evitar la reinstalación implícita de datos que la herramienta
puede necesitar en APIs anteriores. Para API26/30 debe prepararse y revisarse una
estrategia CompilationMode.Ignore; la ejecución sigue PENDING, no se fuerza None.
El scroll usa margen relativo a ambas dimensiones, velocidad800px/s y una assertion
de desplazamiento; dos gestos rápidos que no producen frames no son una muestra válida.

JSON nativo y Perfetto se guardan bajo Android/media del paquete del test. Copiarlos
a artifacts/sprint8/macro solo para revisión local; revisar/redactar antes de
compartir: las trazas pueden contener metadatos del sistema. Con ejecución Gradle
manual también aparecen en build/outputs/connected_android_test_additional_output.

StartupTimingMetric mide TTID y, cuando Android emite el evento, TTFD. El criterio
de ReportDrawnWhen requiere preferencias+onboarding+layout restaurado+primer snapshot,
sin esperar TTS o proveedores opcionales. Se excluye onboarding. HOT puede no
emitir un TTFD nuevo al conservar Activity: no rellenar el dato ausente.
FrameTimingMetric mide CPU por frame y overrun solo desde API31; no convertir una
duración global del test en frame timing. p95 por frame no es p95 entre arranques.

## Comparación histórica y estabilidad

```powershell
$apk = 'android/app/build/outputs/apk/debug/app-debug.apk'
$apkHash = (Get-FileHash $apk -Algorithm SHA256).Hash
python scripts/qualify_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --samples 20 --allow-active --out artifacts/sprint8/after.json
python scripts/performance_report.py --before artifacts/sprint8/before-valid.json --after artifacts/sprint8/after.json --out artifacts/sprint8/comparison.json
python scripts/stability_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --cycles 20 --allow-active --dry-run --out artifacts/sprint8/stability-plan.json
python scripts/stability_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --cycles 20 --allow-active --out artifacts/sprint8/stability.json
```

Instalar también app-debug-androidTest.apk con adb install -r. La instalación
incompatible se detiene; no desinstalar ni borrar datos. Usar am instrument directo
para conservar los APKs; connectedDebugAndroidTest puede desinstalarlos al finalizar.
El runner hace ciclos en un proceso instrumentado (PSS Debug.getPss), luego reinicios
y HOME/foreground (PSS dumpsys). Son poblaciones distintas, sin prueba de fuga por
sí solas. Los tiempos de presets/Ajustes/Lab/retorno incluyen sincronización/scroll
del harness y son cotas externas, no tiempos puros de la app ni TTFD.

Fuentes oficiales consultadas:
[configuración](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview),
[métricas y ReportDrawnWhen](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics),
[versiones](https://developer.android.com/jetpack/androidx/releases/benchmark),
[ProfileInstaller](https://developer.android.com/jetpack/androidx/releases/profileinstaller).

## Sprint 9 — forense y preservación

`python scripts/host_health.py` captura tres muestras CIM de memoria disponible,
headroom de commit, paginación y CPU. El runner Macrobenchmark lo exige antes y
después: bloquea memoria física <512 MiB, commit libre <1024 MiB o las tres muestras
con >1000 páginas entrantes/s. Son guardas conservadoras del host local, no un
presupuesto de rendimiento de la app. Las muestras de extremos no excluyen picos
intermedios; registrar ruido y no cerrar aplicaciones ajenas para conseguir verde.
PagesInput/sec incluye fallos duros de ejecutables/DLL/ficheros mapeados, no solo
lecturas del pagefile: el contador aislado no demuestra intercambio extremo.
La guarda es conservadora y no atribuye causa de jank. Véase
[contadores de memoria de Windows](https://learn.microsoft.com/en-us/troubleshoot/windows-client/performance/how-to-determine-the-appropriate-page-file-size-for-64-bit-versions-of-windows).
También comprueba ActivityManager y los hashes instalados después, y conserva la
identidad del paquete debug y hash de layout. No compilar/instalar/descargar SDK ni
analizar masivamente trazas mientras se mide. Un fallo detiene la secuencia.

```powershell
python scripts/benchmark_android.py --serial emulator-5554 --allow-active --method dashboardScroll --iterations 20 --out artifacts/sprint9/repeat-dashboardScroll.json
python scripts/collect_macro_evidence.py --serial emulator-5554 --allow-active --out artifacts/sprint9/macro/dashboardScroll
python scripts/jank_evidence.py --trace-root artifacts/sprint9/macro --processor .tools/trace_processor.py --code-commit COMMIT_REAL_DEL_APK --apk artifacts/sprint9/baseline-benchmark.apk --run-root artifacts/sprint9 --out artifacts/sprint9/repeat-forensics.json
```

Repetir los cuatro escenarios secuencialmente. El collector crea una carpeta nueva
y solo copia JSON nativo y sus trazas referenciadas; se niega a sobrescribir intentos.
Descargar el launcher oficial de https://get.perfetto.dev/trace_processor a
`.tools/trace_processor.py`; ejecutarlo con Python descarga el binario oficial
verificado por el launcher. Esta sesión usó v58.2. Raw traces permanecen locales.
Los errores de parseo se marcan REVIEW_REQUIRED y no se eliminan del informe.

API26/30: compilar el nuevo runner y usar `--compilation Ignore`; la instrumentación
también valida el modo y rechaza None antes de API34 aunque se invoque sin Python.
Ignore conserva compilación existente: no comparar contra None como condiciones
equivalentes. API26/30 no ofrecen frameOverrunMs: ausencia no es cero jank.

La suite completa incluye tests que reemplazan la credencial fixture, guardan layout
y conceden/revocan listener multimedia. **am instrument no desinstala**, pero eso
no hace que la suite sea segura para preferencias personales. Ejecutarla mediante
`compatibility_android.py --allow-disposable-install-and-tests` únicamente en AVDs
dedicados `CopilotDrive_Sprint9_API26/30/35`. El runner rechaza teléfonos y otros AVDs,
verifica firma/hash antes/después y registra errores de suite por separado.
`connectedDebugAndroidTest` continúa reservado a CI/AVDs desechables.

Un ensayo de renderer debe mantener APK/display/iteraciones/compilación, cerrar
limpiamente el AVD y arrancar temporalmente con `-gpu host`, sin editar config.ini
ni cargar/guardar snapshots. Verificar GLES efectivo, paquete y fingerprints de
DataStore, repetir y restaurar el arranque original en finally. Un cambio de
renderer es un experimento del entorno, no una optimización del código Android.
Detenerse si falla restauración. No aplicar este procedimiento a un teléfono.

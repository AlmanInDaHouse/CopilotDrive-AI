# Sprint10 — sin cohorte nueva de rendimiento

No se ejecutaron benchmarks ni se atribuyeron optimizaciones. Observación inicial
Windows:326/367/574MiB disponibles, headroom2683–3030MiB; guarda
HOST_PHYSICAL_MEMORY_PRESSURE. Otra observación:122–141MiB libres y529–538MiB
headroom, también paging sostenido. Se detuvo un daemon Gradle propio inactivo;
no se cerraron aplicaciones ajenas para fabricar condiciones válidas.

El contexto inicial se recogió durante trabajo de baseline: es una precondición
fallida, no medición de rendimiento de app. API26 guest y host son memorias distintas.
Las duraciones de suites/smokes no son TTID/TTFD/FrameTiming ni comparación PSS.
No nuevas trazas ni extrapolación SwiftShader/Radeon a teléfono. Sin heap/dominadores
ni atribución nueva. Rendimiento físico PENDING, campaña host BLOCKED. Se conservan
los resultados históricos, incluidos los inválidos de Sprint9, a continuación.

## Histórico hasta Sprint9

# Comparación de rendimiento — Sprints 8 y 9

## Sprint 9 — repetición forense, mismo binario

Sprint 8 ya está fusionado por PR #9 (`504e9a7`). Nueva tanda de 20 iteraciones
por escenario, mismo AVD/API35/1280×800/160dpi/fontScale 1, CompilationMode.None,
APK benchmark `168ebeeb…d3ef9` y runner original instalado `c43d08d8…87849`.
GLES efectivo: Google SwiftShader4.0.0.1. No hay cambio de código de producto.
Gradle/instalaciones/descargas SDK no se ejecutaron durante estas tandas.
ActivityManager, memoria/paginación del host y preservación se comprobaron en
los extremos; no es monitorización continua ni prueba de CPU/temperatura fijadas.

| Escenario | CPU ms p50/p95 Sprint9 | Overrun ms p50/p95 Sprint9 | Frames | Overrun positivo S8 → S9 |
| --- | ---: | ---: | ---: | ---: |
| dashboardScroll | 31.81 / 52.71 | 25.51 / 52.62 | 1901 | 97.04% → 98.00% |
| settingsAndLab | 27.13 / 76.29 | 14.55 / 126.57 | 407 | 84.26% → 77.15% |
| presetDraft | 24.86 / 57.80 | 11.82 / 68.95 | 628 | 65.70% → 70.70% |
| backgroundRestore | 16.32 / 45.39 | 2.90 / 35.21 | 53 | 61.82% → 60.38% |

Percentiles nearest-rank de la población de frames, no de 20 tiempos de arranque.
Las regresiones y diferencias favorables se conservan. Misma metodología/display/
binario, pero el estado del host histórico no está controlado: comparación
exploratoria, sin significación estadística ni causalidad del código.

Se analizaron 80 trazas antiguas y 80 nuevas. En scroll nuevo, la mediana por ventana
de Main postAndWait es1218.30 ms, RenderThread Running del guest1239.21 ms y
Compose:recompose4.53 ms: persiste una ruta gráfica costosa. Las veinte trazas de
retorno de cada cohorte tienen systrace_parse_failure; se marcan REVIEW_REQUIRED.
Los frames nativos se conservan, con limitación explícita de interpretación causal.
Detalles, hipótesis y auditoría en JANK_INVESTIGATION; datos crudos en
artifacts/sprint9/macro y resúmenes en repeat-forensics.json. No se suman tiempos
inclusivos ni se presenta Running del guest como CPU exclusiva física.

El crecimiento PSS y la compatibilidad se evalúan separadamente en
MEMORY_INVESTIGATION y ANDROID_API_COMPATIBILITY. Ningún nuevo presupuesto de CI.

## Evidencia histórica Sprint 8

API35/Android 15 x86_64; 1280×800px/160dpi/fontScale1, simulador onboarded, sin
proveedores externos. Baseline APK Sprint 7 F85A…F0D01 y APK Sprint 8 C23F86E1B7DC7F3DEA05D9AF6E41374EF2B79A58D1227B302B7CA1AB12400EA2.
20 observaciones secuenciales por cohorte, con force-stop y cachés calientes;
ninguna compilación, suite o matriz simultánea durante la pareja final tras recuperar
el host. APK Sprint 7 archivado y reinstalado con -r, seguido de Sprint 8 con -r en
finally: misma firma/versionCode/esquemas, sin desinstalación ni data clear. Layout
conservado por hash en ambas cohortes. artifactCodeCommit separa origen del binario
del commit del runner. before-valid/after-final conservan las primeras cohortes.
Percentiles nearest-rank. Android/modelo/display/método/condiciones coinciden en
performance_report.py. Host Windows compartido, con presión severa de memoria/paginación observada;
sin fijar CPU/temperatura: comparación exploratoria, sin atribución causal.
Durante las tandas hubo os error1455 del host; se detuvo un daemon Gradle propio
inactivo. El emulador perdió ActivityManager y se reinició el AVD existente sin
wipe/snapshots, preservando hash de layout/APK. Arranques y Ajustes/Lab preceden
a la recuperación; scroll/preset/retorno se repitieron después. No se equiparan
las condiciones del host ni se atribuyen cambios numéricos a una optimización.

| Métrica externa debug | Sprint 7 repetido p50/p95 | Sprint 8 p50/p95 | Cambio p50 |
| --- | --- | --- | --- |
| Activity am start -W (ms) | 2536/2899 | 2561/2917 | +0.99% |
| Dashboard observado/cota UI (ms) | 5639/6180 | 5714/6190 | +1.33% |
| PSS inicial dumpsys (KiB) | 85943/86351 | 87001/87319 | +1.23% |

El baseline histórico publicado de Sprint 7 fue 2732/3417 ms Activity,
5995/6788 ms cota UI y 84848/85369 KiB PSS. La tabla principal usa la pareja
before-controlled/after-controlled, ejecutada tras recuperar el host; se conservan
también before-valid/after-final y el baseline histórico. No se equipara
am-start a TTFD ni se resta la cota UI para inventar tiempo de render.

La omisión de MediaSession en demo elimina la llamada periódica por construcción
(antes una por tick); el IO de secretos ya no bloquea Main y el formato del reloj
se reutiliza. La comparación agregada incluye instrumentación adicional de
disponibilidad: no mide aisladamente esas correcciones. Cualquier cambio numérico
de la tabla se conserva, incluidas regresiones; no se afirma mejora porcentual
por optimización ni se extrapola a teléfono. No se fijó un presupuesto de CI.

## Macrobenchmark instrumentado

Paquete aislado .benchmark, release derivada sin minificación, no debuggable,
firma debug, profileable, CompilationMode.None, 20 iteraciones por escenario.
Advertencia EMULATOR suprimida explícitamente; el resultado no representa un OEM.
StartupTimingMetric: TTID y TTFD; ReportDrawnWhen espera el dashboard visible con
preferencias/onboarding/layout/snapshot. Onboarding queda fuera de la medida.
HOT sin nuevo evento fully-drawn conserva TTFD ausente. FrameTimingMetric produce
CPU y overrun (API31+); overrun positivo indica incumplimiento de deadline.
Los frameCount por iteración permanecen en JSON nativo; la tabla contiene solo
duraciones en milisegundos.

| Escenario | Métrica (ms) | n valores | p50/p95 nearest-rank | Overrun positivo |
| --- | --- | --- | --- | --- |
| backgroundRestore | frameDurationCpuMs | 55 | 17.98/85.42 | — |
| backgroundRestore | frameOverrunMs | 55 | 4.31/82.77 | 61.82% |
| coldStartup | timeToFullDisplayMs | 20 | 1819.07/2420.35 | — |
| coldStartup | timeToInitialDisplayMs | 20 | 1819.07/2420.35 | — |
| dashboardScroll | frameDurationCpuMs | 1855 | 32.48/55.03 | — |
| dashboardScroll | frameOverrunMs | 1855 | 25.69/57.06 | 97.04% |
| hotStartup | timeToInitialDisplayMs | 20 | 153.11/249.08 | — |
| presetDraft | frameDurationCpuMs | 618 | 23.53/67.86 | — |
| presetDraft | frameOverrunMs | 618 | 11.26/91.78 | 65.70% |
| settingsAndLab | frameDurationCpuMs | 394 | 29.30/83.73 | — |
| settingsAndLab | frameOverrunMs | 394 | 18.62/140.71 | 84.26% |
| warmStartup | timeToFullDisplayMs | 20 | 978.52/1519.04 | — |
| warmStartup | timeToInitialDisplayMs | 20 | 606.75/881.45 | — |

Para startup, n=20 arranques. Para frames, n es el total de frames de las 20
iteraciones, no 20 arranques; sus percentiles no se comparan entre sí.
Overrun positivo expresa frames con valor>0 divididos por frames observados;
son jank observado en este entorno, sin presupuesto aprobado ni extrapolación OEM.
Los datos sin métrica no se sustituyen por cero. Métricas nativas y trazas por iteración se
conservan en artifacts/sprint8/macro; resumen numérico en metrics-summary.json.
No había Macrobenchmark en Sprint 7: no se inventa una comparación TTID/TTFD previa.
Fallos conservados: scroll sin frames (gesto demasiado acotado en paisaje),
confirmación de lanzamiento fallida en presets y framework ausente en retorno.
Se corrigió el gesto (margen por dimensión, velocidad800px/s, espera y assertion de
movimiento), se corrigió el selector del diálogo separado del editor, se añadió
guard ActivityManager y se repitieron las tandas fallidas.
Los fallos no entran en las muestras válidas ni se convierten en ceros.
No se han contado recomposiciones ni capturado un heap profile con Android Profiler.

Las cotas de cambio de preset/Ajustes/Lab/retorno, medidas por el harness con
sincronización y scroll, figuran en STABILITY_TEST_REPORT. No son duración pura de
la app ni métricas de frame. Método, comandos, fuentes oficiales y extracción de
trazas en BENCHMARK_RUNBOOK. Raw logs/Perfetto requieren revisión antes de compartir.

## Sprint 9 — binario final y experimento de renderer

Target benchmark SHA-256:
`e2f19020b3c754ea80b520f98169648436069f4eb8d0fddc773ea6180b2a0388`.
Código de producto `5790f71`; variante benchmark independiente, no debug APK 501B.
Mismo AVD API35, 1280×800/160dpi/fontScale 1, None y 20 iteraciones solicitadas.
GLES efectivo Radeon: Android Emulator OpenGL ES Translator (AMD Radeon RX 6600),
OpenGL ES 3.1; al terminar se volvió a Google SwiftShader 4.0.0.1, OpenGL ES 3.0.
Fingerprints RESTORED.

| Observación del binario final | Estado | Frames | CPU p50/p95 ms | Overrun p50/p95 ms | Overrun positivo |
| --- | --- | ---: | ---: | ---: | ---: |
| Radeon, runner anterior al ajuste de nodo | EMULATOR_BENCHMARKED | 1834 | 22.70 /44.50 | 8.24 /42.84 | 84.46% |
| SwiftShader, runner con reacquisición de nodo | INVALID_HOST | 1956 | 30.33 /48.84 | 23.80 /48.76 | 94.38% |

La fila inválida conserva datos, **no entra en una comparación de mejora**. El
host de Radeon pasó las guardas (RAM disponible876–1026 MiB antes, 1046–1070 después).
SwiftShader terminó con 278–369 MiB y además cambió el APK del runner:15850fc3…
frente a 6217590f…; el APK de la app sí coincide. No existe una pareja válida con
idéntico runner/host que permita atribuir una mejora porcentual al renderer ni al
código. La variación observada es consistente con contribución del entorno gráfico;
84.46% de overrun positivo sigue siendo jank alto y queda sin explicación completa.

En Radeon, medianas por ventana: Main Running 267.68 ms, postAndWait 606.16 ms,
Compose:recompose3.40 ms, RenderThread Running 979.76 ms y eglSwapBuffers 595.50 ms.
La ruta de dibujo/buffers sigue siendo visible. No se suman tiempos inclusivos ni
se convierte Running del guest en CPU física exclusiva. La comparación con la
serie inicial de otro APK tampoco es una optimización de producto.

Intentos conservados:

- Primera tanda final SwiftShader bloqueada antes de medir por RAM<512 MiB.
- Otro intento produjo10 trazas y falló en StaleObjectException al salir el header
  de LazyColumn. Se conservó en invalid/final-scroll-stale-object; no se calculan
  percentiles de un recorrido incompleto. La referencia se reacquiere y repetidos
  stale provocan fallo explícito, no una assertion de movimiento aprobada.
- Repetición corregida:20 iteraciones/JSON nativo, pero host final bajo el mínimo;
  raw en invalid/final-scroll-host-pressure, no promoción a muestra válida.
- Nueva campaña Radeon con el runner corregido: BLOCKED antes de medir por RAM.
  coldStartup/warmStartup/hotStartup/settingsAndLab/presetDraft/backgroundRestore
  de esa campaña quedaron NOT_RUN por la misma precondición, con métricas ausentes.

No hay TTID/TTFD nuevos válidos del binario final ni una comparación causal cerrada.
Se conserva la evidencia original de Sprint8 y las 80 trazas iniciales Sprint9 del
mismo APK 168e… para caracterizar los cuatro escenarios. Se analizaron 200 trazas
con SQL; otras10 del intento abortado permanecen como evidencia fallida. Hay130
trazas nuevas Sprint9:100 de tandas que completaron las guardas y 30 de intentos
inválidos/fallidos. El parseo de las 40 trazas de retorno de ambas series requiere
revisión. Los estados de muestra y de parseo son independientes.

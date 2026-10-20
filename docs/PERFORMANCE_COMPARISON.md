# Comparación de rendimiento — Sprint 8

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

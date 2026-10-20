# Rendimiento medido — Sprint 7, 2026-10-20

Evidencia EMULATOR_TESTED, ninguna física. Código APK:
`eb6fe096e00a959b8215b63c249e751b12682d40`.
SHA-256 `F85A4C1252A5C8A700171CBBF8CFA2BEFBCEA33CB5FAF093C1EA8D03030F0D01`.
Ejecución `artifacts/sprint7/performance.json`: commit eb6fe09; workingTreeDirty
true por documentación en preparación, sin cambios de fuentes compiladas del APK.

Entorno: Windows, emulador CopilotDrive_API35, Android 15/API35 x86_64, modelo
Android SDK built for x86_64; 1280×800 px, 160dpi, fontScale1.0; barras del sistema
presentes. APK debug con onboarding completado, preset Classic/simulador, IA/Places
comerciales sin configurar. Sin compilación/instrumentación concurrente durante
muestreo; no se fijó afinidad CPU, temperatura ni carga de otros programas del host.

Herramientas investigadas y disponibles:

- adb/am start -W y dumpsys meminfo: disponibles, utilizados; 20 reinicios de proceso
  por force-stop, cachés calientes y sin borrar datos. Método de primera referencia.
- Perfetto: `/system/bin/perfetto` presente; no se capturó una traza ni se afirma
  análisis de scheduling/energía. dumpsys gfxinfo disponible, sin campaña de frames.
- FrameMetrics: API compatible desde minSdk26, sin colector añadido en esta entrega.
- Macrobenchmark: no hay módulo/dependencia en el proyecto. Requiere módulo de tests
  y variante cercana a release; no se añadió para fingir comparabilidad de debug.
- Android Studio Profiler: no se ejecutó una sesión de profiler; no evidencia de
  recomposiciones, CPU o consumo. No se instaló ninguna herramienta adicional.

Referencias oficiales: [inicio de aplicación](https://developer.android.com/topic/performance/issues/launch-time),
[dumpsys](https://developer.android.com/tools/dumpsys),
[requisitos de Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview).

## Método y resultados

Repetible con el comando `--samples 20` de DEVICE_QUALIFICATION. Cada iteración:
force-stop del paquete, am start -W -n MainActivity, lectura de UIAutomator que
encuentra PTT en dashboard y dumpsys meminfo. Se verifica hash del APK instalado.
Sin envío de peticiones externas ni apertura de micrófono/OBD. Solo números exportados.

`activityStartMs` es TotalTime de am: no TTID aislado por Perfetto. La cota de dashboard
usa perf_counter_ns del host desde antes de lanzar hasta finalizar la sonda UI, e
incluye adb/UIAutomator y eliminación de su temporal. **No es TTFD exacto ni tiempo
de respuesta al toque**. PSS se lee después; unidad KiB, no consumo de batería.
Nearest-rank: p50 posición10 y p95 posición19 de 20 ordenadas. Sin exclusión de outliers.

| Métrica | n | p50 | p95 | Mín–máx |
|---|---:|---:|---:|---:|
| Inicio Activity (ms) | 20 | 2732 | 3417 | 2468–3507 |
| Dashboard observable, cota superior (ms) | 20 | 5995 | 6788 | 5724–6857 |
| TOTAL PSS (KiB) | 20 | 84848 | 85369 | 84588–85622 |

Muestras originales, orden de ejecución:

| Iteración | Inicio ms | Cota dashboard ms | PSS KiB |
|---:|---:|---:|---:|
| 1 | 2626 | 5934 | 84622 |
| 2 | 2732 | 5995 | 84705 |
| 3 | 2881 | 5966 | 84588 |
| 4 | 2625 | 5935 | 84649 |
| 5 | 3404 | 6713 | 85369 |
| 6 | 2576 | 6399 | 85315 |
| 7 | 2703 | 5972 | 84812 |
| 8 | 2580 | 5724 | 84948 |
| 9 | 2468 | 5744 | 84789 |
| 10 | 3507 | 6857 | 85368 |
| 11 | 3065 | 6211 | 84777 |
| 12 | 2639 | 6354 | 85076 |
| 13 | 2765 | 5794 | 84704 |
| 14 | 3177 | 6369 | 85220 |
| 15 | 2734 | 6049 | 84848 |
| 16 | 2564 | 5780 | 84827 |
| 17 | 3008 | 6195 | 85021 |
| 18 | 2620 | 5811 | 85332 |
| 19 | 2940 | 6233 | 84899 |
| 20 | 3417 | 6788 | 85622 |

## Interpretación y mediciones pendientes

Primer baseline numérico de esta entrega; Sprint 6 no midió lo mismo. No hay
evidencia comparativa para atribuir mejora/regresión ni se hizo una optimización
amplia. El arranque observado de debug/emulador requiere medir en teléfono y una
variante cercana a release antes de fijar objetivos. La cota superior sirve como
smoke reproducible, con ruido del instrumento explícito.

Sin medición fiable de FPS/frame pacing, recomposiciones, cambios de pantalla aislados,
energía, duración del editor/proyección como microbenchmark o sesión prolongada.
Los tests de widgets/editor/recreación/foreground prueban funcionalidad y recuperación,
no son benchmarks. Los tiempos virtuales del harness no son latencias físicas.
VoiceLatencyTracker usa mínimo20 por etapa; no se obtuvieron muestras reales de voz
con motor es-ES, por lo que no se publican p50/p95 acústicos ni de estados de voz.
PSS de procesos recién iniciados no demuestra ausencia de fugas tras horas de uso.
Prioridad Sprint8: mediciones equivalentes en hardware, TTFD y frames con variante
adecuada; después objetivos sustentados por esa distribución.

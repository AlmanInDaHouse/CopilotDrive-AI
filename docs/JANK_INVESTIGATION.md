# Sprint 9 — investigación del jank

Fecha: 2026-10-20, Europe/Madrid. Base fusionada: PR #9, main `504e9a7`.
Resultados exploratorios de emulador; ninguna certificación de fluidez OEM.

## Fuentes y procedimiento

Se conservaron las 80 trazas históricas de interacción (20 por escenario) de
`artifacts/sprint8/macro`, JSON nativo y APK benchmark SHA-256
`168ebeeb2c1c3b33ff050b428f92e8c137b2aadf86c036eaac822e79c29d3ef9`.
`jank_evidence.py` consulta el procesador oficial Perfetto v58.2 y
`jank_window.sql`: solo paquete `.benchmark`, ventana `measureBlock`, intersección
temporal de slices/estados de hilos. No se suman tiempos inclusivos anidados como
CPU independiente. `Running`, runnable (`R`/`R+`) y sleeping (`S`) se separan.
FrameTimeline clasifica eventos; sus categorías no son diagnósticos causales.

```powershell
$source = git rev-parse b9f3d9f
python scripts/jank_evidence.py --trace-root artifacts/sprint8/macro --processor .tools/trace_processor.py --code-commit $source --apk artifacts/sprint9/baseline-benchmark.apk --out artifacts/sprint9/historical-forensics.json
```

La procedencia del APK histórico está ligada al árbol de código, no a la fecha de
los informes posteriores. Hashes de cada traza y contexto nativo quedan en el JSON.
El host histórico tuvo error1455 y fallos de framework: no hay observaciones
continuas del host que permitan recuperar unas condiciones controladas a posteriori.

## Evidencia histórica por escenario

Medianas entre 20 trazas, tiempos de pared acumulados por ventana, en ms.
Running describe el scheduler del guest; una vCPU pausada por el host puede
seguir figurando Running y estos tiempos no equivalen a CPU física exclusiva:

| Escenario | Main Running | RenderThread Running | Main postAndWait | Compose:recompose | measureAndLayout | eglSwapBuffers |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| dashboardScroll | 270, 25 | 1259, 88 | 1118, 99 | 4, 49 | 25, 10 | 813, 22 |
| settingsAndLab | 307, 35 | 843, 06 | 162, 98 | 33, 00 | 1, 27 | 414, 16 |
| presetDraft | 267, 18 | 1152, 71 | 213, 26 | 26, 27 | 2, 37 | 656, 36 |
| backgroundRestore | 59, 09 | 38, 89 | 4, 67 | 3, 05 | 0, 94 | 17, 74 |

Las ventanas incluyen sincronización y esperas del harness: aproximadamente
2 s scroll, 4–5 s Ajustes/presets y 2, 7 s retorno. No son duración de una sola
recomposición o frame. Los tiempos anteriores se solapan; no sumar columnas.

- Scroll: Main duerme una mediana de 1457, 34 ms, con mucho `postAndWait`, y
  RenderThread acumula bastante más tiempo Running del guest que Main. `eglSwapBuffers`, dequeue y
  comandos gráficos son visibles. Predominan etiquetas `Prediction Error` y
  `Buffer Stuffing`, junto a incumplimientos de app/SF en algunas muestras.
  Hipótesis apoyada por correlación: ruta de renderizado/emulación dominante.
  No demuestra que toda la demora provenga del host ni que el diseño sea óptimo.
- Ajustes/Lab: composición y transiciones tienen coste medible, con colas de Main
  y trabajo de renderizado. No hay desglose por función Compose que permita
  imputar un pico a Device Lab, a un collector o a un cálculo concreto.
- Presets: se mide entrada, diálogo, scroll, cambio de draft y salida. No es un
  benchmark aislado de `LayoutEngine.project`; no atribuir el overrun al codec o
  a una escritura DataStore que este recorrido ni siquiera ejecuta.
- Retorno: solo 55 frames nativos en 20 iteraciones; todos sus traces registran
  `systrace_parse_failure`. Se conservan los números, pero la interpretación de
  scheduling necesita revisión. No se oculta el error ni se declara raíz causal.

## Auditoría de Compose y adaptadores

DashboardWidgets/Layout/Customization corresponden a `ModularDashboard.kt`,
`DashboardLayout.kt` y su coordinador; no existen módulos independientes con
esos nombres. La proyección y agrupación usan remember; filas y widgets tienen
keys. `WidgetRow.sortedBy` y etiquetas pequeñas se calculan al componer, sin
evidencia de que expliquen el jank. No se cambia el framework ni se marcan listas
mutables como estables para forzar saltos de composición.

`CopilotApp` recibe DashboardState global y los cambios de telemetría pueden
invalidar contenedores; collectors principales y de Device Lab usan lifecycle.
Un ticker foreground de 1 s refresca capacidades/media/snapshot/reloj y cancela
en background; no hay otro poller por widget. El poller OBD permanece manual y
único. No se reduce la frecuencia de señales de seguridad por rendimiento.
Las animaciones son acotadas por estado, no un reloj gráfico infinito.

Settings mantiene borradores locales con remember; Device Lab solo consulta
diagnósticos al pulsar, con historial acotado. Places no consulta proveedor ni
ubicación al componer: el AndroidView de atribuciones solo aparece con resultados.
No hay consulta comercial en los benchmarks. MediaCard usa el Bitmap entregado
por MediaSession; no descarga ni decodifica URI remota. Tamaño/retención OEM del
bitmap siguen pendientes; no se afirma que un bitmap grande sea inocuo.

Los informes estáticos del compilador son opt-in (`-PcomposeReports`).
Skippability/estabilidad estática no es un contador dinámico de recomposiciones.
En la compilación ejecutada, DashboardState, DashboardLayout y listas de widgets
son unstable; DashboardScreen, WidgetRow, WidgetContent, DashboardEditor,
SettingsScreen, DeviceLabScreen y PlacesScreen figuran restartable skippable.
No se deduce "siempre recompone" de un parámetro unstable con strong skipping.
La traza existente da duración de `Compose:recompose`, no atribución por widget.
No se ha usado Android Studio Layout Inspector ni se inventan sus resultados.

## Decisiones e incertidumbre

El experimento final Radeon completó20 iteraciones con guardas aprobadas:
CPU por frame p50/p95 22.70/44.50 ms y overrun positivo 84.46%. Main postAndWait
acumuló una mediana606.16 ms por ventana; RenderThread Running 979.76 ms.
Sigue habiendo jank alto. El nuevo software produjo20 iteraciones pero la presión
física final del host las invalida para comparación; otra tanda falló por un nodo
UIAutomator reciclado y la campaña corregida Radeon quedó bloqueada antes de medir.
No se atribuye una mejora porcentual al cambio de renderer ni al código: falta una
pareja válida con igual runner y condiciones razonables. Ver tabla y procedencia
exactas en PERFORMANCE_COMPARISON, incluidos todos los resultados negativos.

No hay una ruta de código atribuida con precisión suficiente para una
optimización del jank. Se evitan correcciones especulativas de rendimiento.
Una regresión real API26 al revocar el listener multimedia justificó una corrección
independiente de permisos, documentada en ANDROID_API_COMPATIBILITY; no se presenta
como mejora de fluidez. Las mejoras del harness son guardas de host, preservación,
provenance/SQL y compilación Ignore segura para APIs antiguas; no se presentan
como reducción del jank de la app.

Resultados nuevos y cualquier intervención de renderer: PERFORMANCE_COMPARISON.md.
Memoria: MEMORY_INVESTIGATION.md. Raw Perfetto/JSON/logs son locales e ignorados;
revisar privacidad antes de compartir. Sin métricas ausentes convertidas en cero,
sin otros warnings suprimidos aparte de EMULATOR y sin umbrales rígidos de CI.

Fuentes oficiales: [Trace Processor](https://perfetto.dev/docs/analysis/trace-processor),
[tablas SQL](https://perfetto.dev/docs/analysis/sql-tables),
[Compose tracing](https://developer.android.com/develop/ui/compose/tooling/tracing),
[estabilidad y reports](https://developer.android.com/develop/ui/compose/performance/stability/diagnose),
[aceleración del emulador](https://developer.android.com/studio/run/emulator-acceleration).

Actualización histórica Sprint10 (2026-10-20): Sprint9 fue fusionado mediante
[PR #10](https://github.com/AlmanInDaHouse/CopilotDrive-AI/pull/10) en
`793a3535713cd76cfbe6c85f9fbfb7bff21cb4b5`. Workflows del merge comprobados:
[Android success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39454974296),
[Tests success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39454974344).
El resto conserva la entrega histórica previa al merge. Resultados nuevos, incluida
la investigación de svc y los permisos transitorios del APK, en SPRINT10_REPORT.md.

# Sprint 9 — Jank Forensics, Android Compatibility & Real-Device Validation

2026-10-20, Europe/Madrid. Rama local `feat/sprint9-jank-compatibility`.
Entrega técnica para revisión con resultados inconclusos y bloqueos explícitos;
sin push, merge ni release. Hardware físico y proveedores comerciales PENDING.

## Git, merge y baseline

Raíz: checkout opencar-copilot dentro de OpenCar-Copilot-MVP. Árbol inicial limpio;
se preservaron las ramas anteriores y el worktree sprint6-baseline. Fetch origin,
main fast-forward `328ffec` → `504e9a776177d78d70a085dcb7b4cacc914a069a` y rama
Sprint 9 creada. Sin reset, limpieza destructiva ni force push.

[PR #9 fusionada](https://github.com/AlmanInDaHouse/CopilotDrive-AI/pull/9) y ambos
workflows del merge comprobados por API:
[Android success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39423473557),
[Tests success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39423473560).
El estado anterior de Sprint 8 como rama local es histórico, anterior a ese merge.
No hay CI remoto nuevo de Sprint 9 porque no se publicó la rama.

Baseline funcional Sprint 8: 184 casos (Python 36/core 43/testing 67/app 6/Android 32).
Se ejecutó Python y el pipeline local; se forzaron después las suites JVM y se
repitieron las 32 instrumentadas del APK histórico C23F…0EA2 sobre AVD35 desechable.
Los cambios de herramientas/documentación no se atribuyen al binario anterior.

## Especificación y evidencia

specs/016-jank-compatibility contiene spec, contracts, acceptance y test-plan.
Reutiliza rendimiento/calificación de specs/014 y 015. El JSON numérico relaciona
escenario, commit de código/runner, SHA del APK, Android/API/modelo, display,
renderer, compilación, iteraciones, host antes/después, duración y trazas con hash.
Hipótesis, intervención y conclusión se documentan sin convertir correlación en
causalidad. Ausencia de muestra, métrica o TTFD no se convierte en cero.

Se analizaron 80 trazas históricas y 80 de una repetición inicial equivalente del
mismo APK benchmark `168ebeeb…d3ef9`. La consulta Perfetto restringe paquete y
measureBlock, separa Main/RenderThread y sus estados de scheduler, y conserva
errores de parseo. Versiones del análisis y SQL quedan ligadas por hash.
Raw Perfetto/JSON/logs permanecen en artifacts/sprint9 ignorados por Git.
No se exportaron secretos, conversaciones ni seriales físicos.

## A–B. Métricas originales y nuevas

Original Sprint 8: Activity p50 2536 →2561 ms en comparación Sprint7/Sprint8;
scroll 97.04%, Settings/Lab84.26%, preset 65.70%, retorno 61.82% de frames con
overrun positivo. PSS instrumentado+7007 KiB en 20 ciclos. Son métricas de emulador
y poblaciones diferentes: am-start no equivale a TTID/TTFD ni a frame timing.

Primera repetición Sprint 9, mismo AVD/API35/1280×800/160dpi/fontScale 1, None,
20 iteraciones por escenario y mismo binario previo a la corrección API26:

| Escenario | CPU p50/p95 ms | Overrun p50/p95 ms | Frames | Overrun positivo |
| --- | ---: | ---: | ---: | ---: |
| dashboardScroll | 31.81 /52.71 | 25.51 /52.62 | 1901 | 98.00% |
| settingsAndLab | 27.13 /76.29 | 14.55 /126.57 | 407 | 77.15% |
| presetDraft | 24.86 /57.80 | 11.82 /68.95 | 628 | 70.70% |
| backgroundRestore | 16.32 /45.39 | 2.90 /35.21 | 53 | 60.38% |

Las diferencias favorables y desfavorables se conservan; no existe optimización
de código entre esas dos cohortes. Percentiles nearest-rank por frame, no entre
arranques. Detalle de las tandas del binario final y experimento de renderer en
PERFORMANCE_COMPARISON.md; no se mezclan paquetes debug y benchmark.

## C–E. Entorno, trazas y diagnóstico

Host Windows compartido, 16 GB de RAM, con presión física/commit y picos de lecturas
de páginas. No se fijaron CPU, frecuencia ni temperatura. Las guardas del host
rechazan presión severa antes/después y ActivityManager ausente. No hubo Gradle,
instalación ni descarga SDK durante las ventanas de medida. Solo EMULATOR se
suprimió como advertencia de calidad; no hay umbrales rígidos nuevos de CI.

En las 20 trazas históricas de scroll, medianas por ventana: Main Running 270.25 ms,
RenderThread Running 1259.88 ms, Main postAndWait 1118.99 ms, Compose:recompose4.49 ms
y measureAndLayout25.10 ms. La repetición volvió a mostrar mucha espera de Main y
trabajo de renderizado/buffers. Estos tiempos inclusivos se solapan; Running del
guest puede incluir una vCPU pausada y no equivale a CPU física exclusiva.

FrameTimeline muestra Prediction Error/Buffer Stuffing y también incumplimientos
de app/SF. No son etiquetas de causa raíz. El renderizado/emulación es una ruta
de latencia identificada; no se atribuye todo el jank al host ni se certifica que
la UI sea óptima. Las20 trazas de retorno de cada cohorte tienen
systrace_parse_failure: REVIEW_REQUIRED, sin eliminar datos ni inventar certeza.

Compose: proyección/agrupación recordadas y keys existentes; ticker único
foreground de 1 s; collectors con lifecycle; sin proveedor por widget ni fetch de
Bitmap en composición. DashboardState/listas son unstable, pero las pantallas
figuran restartable skippable en los reports del compilador. Eso no mide conteos
dinámicos por widget. No se usó Layout Inspector ni se inventa su observación.
JANK_INVESTIGATION documenta cada escenario y lo que sigue sin atribución.

## F–G. Corrección y comparaciones

No se cambió la arquitectura ni se añadieron optimizaciones especulativas de
Compose. La corrección funcional justificada fue API26: el helper de AndroidX
conservaba paquetes autorizados cuando el ajuste de listener pasaba a null.
AndroidMediaAdapter ahora lee el estado actual del componente exacto. 27+ mantiene
la API pública. La regresión exige denegación tras revocar y restauración final;
34/34 aprobaron en 26/30/35. No se presenta este cambio como mejora del jank.

El harness añade guardas de host, hashes antes/después, captura numérica/SQL,
Ignore sin reset para APIs antiguas, onboarding visible fuera de medida y lectura
robusta de nodos reciclados. None continúa bloqueado antes de 34 en Kotlin y
Python. Ignore quedó compilado y con guardas unitarias; no se declara benchmark
de rendimiento ejecutado en 26/30.

El ensayo Radeon/SwiftShader cambia el entorno de renderizado, no el código de
producto. Se mantienen APK, display, gestos, compilación e iteraciones y se
registra cualquier cambio del runner. Resultados y límites en PERFORMANCE_COMPARISON.

Radeon completó20 iteraciones con guardas aprobadas: CPU p50/p95 22.70/44.50 ms,
overrun8.24/42.84 ms y 84.46% positivo. La repetición final software produjo20
iteraciones pero terminó con 278–369 MiB libres: INVALID_HOST. El runner difiere
entre ambas por la corrección de nodo; no se calcula una mejora causal con ellas.
La nueva campaña de siete escenarios con runner corregido se bloqueó antes de
medir por falta de RAM. No hay TTID/TTFD finales nuevos válidos; los seis métodos
restantes no se ejecutaron. Se conservan200 trazas analizadas y 10 fallidas:130
trazas nuevas Sprint9 y 80 históricas. No se convierte ausencia de medida en cero.

## H. Memoria y estabilidad

100 ciclos sostenidos y 20 reinicios, 330627 ms en AVD35 independiente. El emulador
aumentó la RAM solicitada de 1536 a 2560 MB; se conserva el log y no se confunde
configuración solicitada con memoria efectiva.
0 crash/ANR observados en historial acotado exit-info de esa ventana. Paquetes,
layout, pantalla y permiso verificados/restaurados. En100 muestras, PSS
154150→146935 KiB, Java 20936→7368, nativo 22699→19976; referencias débiles a
Activities anteriores1–3 y 10 jobs del ViewModel, sin acumulación con los ciclos.
El cierre de Activity canceló el scope en las tres APIs.

Host final 302–346 MiB disponibles: PSS INCONCLUSIVE para comparación de rendimiento.
La caída de 7215 KiB no prueba mejora, ni el incremento anterior prueba una fuga.
No se suman heap asignado y PSS ni se equiparan procesos reiniciados a retención
del mismo heap. Sin heap/dominadores ni garantías OEM. Detalle y raw numérico en
MEMORY_INVESTIGATION y STABILITY_TEST_REPORT.

## I. Compatibilidad y preservación

Imágenes oficiales Google AOSP26/30 instaladas tras autorización expresa de
descarga/licencias; 35 ya existía. AVDs independientes, un emulador a la vez.

| API / Android | Instrumentadas | Instalación/paquetes | Rotación/foreground | Jarvis con red desactivada |
| --- | ---: | --- | --- | --- |
| 26 /8.0.0 | 34/34 | PASS, mismo APK/hash | PASS | BLOCKED: svc wifi137 |
| 30 /11 | 34/34 | PASS, mismo APK/hash | PASS | PASS, red restaurada |
| 35 /15 | 34/34 | PASS, mismo APK/hash | PASS | PASS, red restaurada |

Las suites cubren onboarding, dashboard, widgets/presets/DataStore, Device Lab,
voz/capacidades, permisos, MediaSession fixture, Jarvis local, Places demo y sin
proveedor/consentimiento, recreación y cierre del ViewModel. minSdk 26 no se usa
como sustituto de ejecución. No se califican fabricantes ni APIs intermedias.

La suite completa sobrescribe credenciales fixture/layout/listener: solo AVDs
desechables con nombre validado por el runner. Se usó am instrument e install-r;
ningún connectedAndroidTest, data clear ni uninstall en Sprint 9. El AVD valioso
estuvo detenido durante tests y se verificaron fingerprints de APK y DataStore
al restaurarlo. No se cargaron/guardaron snapshots ni se tocó un teléfono.

Incidencia API26: svc wifi devuelve137; el runner conserva el fallo de restauración
de comandos, aunque los flags observados coinciden de nuevo con el original 1/1.
Se detuvieron escenarios y verificó el estado antes de continuar. El primer
intento no persistía los flags originales: no se afirma recuperación retroactiva
perfecta de ese intento. Solo afectó al AVD desechable; offline26 queda bloqueado.

## J. Tests, APK y seguridad

195 casos distintos: Python 45/core 43/testing 67/app 6/Android 34. Las34 instrumentadas
se ejecutaron por cada API (102 ejecuciones, no102 casos nuevos). Lint0 errores y
12 avisos existentes; build debug/AndroidTest/benchmark, repository check y diff
check aprobados. Fallos originales y correcciones se conservan, incluidos el opt-in
de Ignore, fixture Android 8, parser legacy, accesibilidad transitoria y nodo stale.

APK: artifacts/sprint9/CopilotDrive-AI-sprint9-debug.apk; 19736019 bytes;
minSdk 26/target 35; Android Debug firmado, no firma de producción. SHA-256:

`501b01a1a48466718a185160c9fda40ce30d80efe217c76b2ad9879e1bc08d5c`

Certificado SHA-256:
`ad561b54e005e27e4ca2873963ee7f6c34c4e246feb9ea76aa4cc2e2eb476a8a`.
Instalación/MainActivity/dashboard y consulta local observadas; offline completo
en 30/35. Firma/hash y ambos paquetes se verificaron antes/después de instrumentar.

ToolRegistry/PolicyEngine, consentimiento IA/Places, exclusión de telemetría del
LLM, Keystore, respuesta escrita, contactos confirmados, intents, media autorizada,
ELM read-only, laboratorio y simulación explícita se conservan. No ECU writes,
CAN libre, borrado de averías, micrófono permanente ni servicios comerciales.

## Commits, Sprint 10 y PR

Las instalaciones finales se hicieron en los tres AVDs desechables. El AVD original
conserva deliberadamente su APK debug histórico C23F…0EA2 y los hashes originales
de ambos DataStore; la app benchmark e2f19020…a 0388 es un paquete independiente.
Las tres restauraciones y final-preservation.json aprobaron. El inventario pasivo
final encontró un emulador autorizado y ningún teléfono; no se exportaron seriales.

Commits locales: `33bce9b` especificación, `dadd757` revocación API26,
`5790f71` forense/guardas/calificación, `3fc0151` nodos reciclados del benchmark.
El commit documental posterior registra resultados; no cambia el binario.
Estado Git final y artefactos se verifican antes de entregar; sin push/merge/release.

Sprint 10: teléfono consentido para STT/TTS es-ES, audio focus, TalkBack, ELM327 y
vehículo detenido; profiling real y atribución por composable antes de optimizar;
resolver el bloqueo de red API26 y validar proveedores opcionales con cuota.
PHYSICAL_VALIDATION_MATRIX mantiene todas las observaciones humanas PENDING.
NEXT_STEPS contiene procedimiento y enlace para abrir PR después de autorizar push.

## Límites de aceptación

La entrega permite revisar código, regresiones, APK y evidencia; no cierra una
comparación válida de los siete benchmarks finales, el smoke offline API26 ni
la validación física. acceptance.md deja esos puntos abiertos. La causa completa
del jank y la explicación del PSS histórico siguen inconclusas; no se presenta
esta candidata debug como una release de producción.

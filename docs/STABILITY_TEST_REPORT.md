# Estabilidad y recuperación — Sprint 9

APK `501b01a1…bc08d5c`, API35 AVD desechable. Se solicitaron 1536 MiB, elevados
automáticamente por el emulador a 2560 MB. 100 ciclos sostenidos
(draft, background/foreground, Settings, Device Lab, Places sin consulta y recreate)
y 20 reinicios/rotaciones:330627 ms. Resultado funcional EMULATOR_TESTED;
0 crashes y 0 ANR observados en el historial acotado exit-info. No garantía de
ausencia de fallos fuera de esa ventana. Paquetes y hashes presentes al terminar;
layout/pantalla/micrófono restaurados. Ningún data clear ni uninstall.

PSS sostenido 154150→146935 KiB; referencias débiles a Activities anteriores1–3;
10 jobs del ViewModel en las 100 muestras. La prueba de cierre canceló el scope en
API26/30/35. Host final 302–346 MiB disponibles: los valores PSS permanecen como
observación INCONCLUSIVE, sin afirmar mejora o ausencia de fuga. Datos separados
de los 20 procesos reiniciados y detalles en MEMORY_INVESTIGATION.md.

Suites34/34 por API26/30/35. Smoke de rotación/foreground ejecutado en las tres;
offline con restauración verificada en 30/35. API26 offline BLOCKED: svc wifi137;
se detuvieron los escenarios, se verificó red1/1 igual al original y se conservó
la incidencia del AVD desechable. El primer intento no persistía flags originales;
el runner ahora registra antes/después y espera transiciones. El AVD valioso
estuvo detenido durante estas pruebas; fingerprints conservados en artifacts/sprint9.

## Histórico — Sprint 8

API35 emulado, APK C23F86E1B7DC7F3DEA05D9AF6E41374EF2B79A58D1227B302B7CA1AB12400EA2, código Android 259af8d. Runner activo opt-in y hash
del APK/AndroidTest instalados comprobados.20 ciclos sostenidos (preset draft,
foreground/background, Device Lab pasivo, Places sin proveedor, Activity recreate)
y 20 reinicios con HOME/foreground y rotación alterna. Duración total
173.83 s. No prueba de varias horas ni de hardware físico.
Después de esta ventana, durante Macrobenchmark, el emulador perdió ActivityManager
y el host agotó paginación. Ese fallo de entorno se registra por separado y no
se oculta bajo el contador 0 de crashes de app observado en estabilidad. Se recuperó
el AVD sin wipe, verificando layout y APK; evidencia framework-recovery.json.

Resultado: EMULATOR_TESTED; crashes observados 0,
ANR observados 0; sin fallo de assertions/recuperación en
las fases completadas. Se observan nuevas razones 4/5/6 de ActivityManager exit-info
API30+, muestreadas tras fases/ciclos. Historial acotado: no es garantía de ausencia
de fallos fuera de la ventana o de errores internos no expuestos al harness.

Display/font/densidad/rotación: RESTORED,
sin failedSettings. Preferencias del test restauradas en finally; hash de layout
igual tras cada ciclo/reinicio. Micrófono en tanda 20: PENDING
por flags no vacíos; se evitó modificar un estado que el primer runner no reconstruía.
Se corrigió el guard para permitir la comprobación de un permiso ya denegado sin
mutarlo y se añadió regresión Python. Repetición separada 1 ciclo:
EMULATOR_TESTED, permiso RESTORED,
originalGranted=False.
No concesión nueva, ni captura STT; transición de revocación con permiso concedido
no ejecutada en este entorno. Flags concedidos/fijos que no se puedan restaurar
siguen PENDING antes de la mutación.

## Memoria

Proceso instrumentado sostenido (Debug.getPss),20 observaciones: primera 142575,
última 149582, mínimo 142575, máximo 150973 KiB; delta+7007 KiB.
Incluye runtime de tests y recreaciones. No se compara con el PSS de APK sin
instrumentación, no se fuerza GC y no se declara ausencia de fugas. Incremento
observable requiere seguimiento con heap profiler y ejecución larga en teléfono.

Reinicios (dumpsys),20 observaciones: PSS previo 79003 KiB,
delta final+4 KiB. Cada reinicio cambia proceso;
esta serie prueba recuperación, no retención prolongada del mismo heap.
Muestras y tiempo monotónico completos: artifacts/sprint8/stability.json.

## Cotas de operación del harness

Incluyen scroll, confirmación automatizada, espera de Compose y sincronización.
No representan reacción humana ni duración pura de la app; no son TTFD.

| Operación | n | p50/p95 cota (ms) |
| --- | --- | --- |
| backgroundRestore | 20 | 961.55/1250.04 |
| deviceLabWithScroll | 20 | 133.13/206.10 |
| presetDraft | 20 | 242.85/399.56 |
| settingsWithConfirmation | 20 | 390.26/781.62 |

## Fallos simulados y recuperación adicional

StabilityScenarioTest:100 consultas/fallos TTS/cancelaciones/focus/media fake y 100
ciclos OBD fake con NO DATA/pérdida/reconexión limitada/retorno al simulador/cierre.
Los64 casos históricos de harness y las regresiones nuevas siguen aprobados.
No son fallos de audio/ELM observados en teléfono;100 repeticiones no son100 casos.
Solo cierre fake verificado; no socket físico abierto. Voz conserva respuesta textual.

Matriz emulada: 11 configuraciones; restauración
RESTORED. Smoke de red/rotación/consulta local
offline: EMULATOR_TESTED, red RESTORED.
El runner nuevo no ejecuta data clear ni desinstalación; incidencia del baseline
Gradle documentada en SPRINT8_REPORT.A. Sin llamadas, contactos personales, queries comerciales o audio
automático. Sin logs/UI privados exportados. PHYSICALLY_VERIFIED permanece PENDING.

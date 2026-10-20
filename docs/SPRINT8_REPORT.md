# Sprint 8 — Android performance, interoperability and test readiness

Actualización Sprint 9: Sprint 8 ya está fusionado mediante PR #9 en `504e9a7`.
Android y Tests del merge terminaron success. El resto conserva la entrega local
histórica anterior al merge; resultados nuevos en SPRINT9_REPORT.md.

Entrega local de revisión, 2026-10-20 (Europe/Madrid). Verificación física PENDING.
No push, PR, merge ni release de Sprint 8. APK convencional para Android abierto.

## A. Git y baseline

Raíz real: checkout opencar-copilot; se conserva el worktree histórico sprint6-baseline.
Árbol inicial limpio. Fetch origin ejecutado; main actualizado por fast-forward de
1328077 a `328ffec4457d9badd3569413ceff0eb7de153954`. PR #8 merged=true comprobado
mediante GitHub API. CI final del merge:
[Android success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39408440482),
[Tests success](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39408440265).
Son CI de Sprint 7 fusionado, no de la nueva rama local.
Rama creada: feat/sprint8-performance-readiness. Sin reset/clean/force push.

Baseline ejecutado: Python 23/core 38/harness 64/app 6/instrumentación 30 = 161 casos;
build debug+AndroidTest y lint correctos. Muestras del APK histórico F85A…F0D01
ligadas al merge, con archivos originales en artifacts/sprint8/before-valid.json.
workingTreeDirty=true refleja specs/código nuevo sin compilar; el hash del APK
coincide exactamente con el artefacto histórico de Sprint 7. No se atribuye ese
árbol editado al binario previo.

Incidencia de preservación: connectedDebugAndroidTest del baseline desinstaló la
app del emulador al terminar y fue necesario reinstalar/completar onboarding.
Ese estado previo del emulador no se había respaldado y no se declara restaurado.
No se ejecutó pm clear ni desinstalación manual. Se corrigió el procedimiento:
instrumentación final mediante am instrument directo, con install-r y restauración
del estado temporal de los nuevos runners. No hubo teléfono físico afectado.

## B. Auditoría del arranque

| Etapa | Evidencia en código y decisión |
| --- | --- |
| Application/MainActivity | Application Android estándar; MainActivity crea contenido Compose y obtiene un único ViewModel. No inicializador Application propio ni red de arranque |
| DashboardViewModel | Construcción eager de coordinadores y adaptadores; sin proveedor por widget. No se introdujo contenedor DI ni refactor masivo |
| TTS/STT | Constructor TTS enlaza con el servicio desde Main, con callback y espera suspendida máxima5 s. initialize también se invoca al foreground; no se ha demostrado que sea cuello de botella. Reconocedor solo se crea al pulsar; no escucha permanente |
| Keystore | Lectura inicial tras initialize y lecturas del proveedor se ejecutaban en coroutine Main; guardado cifraba y hacía commit síncrono desde UI. Ahora lectura/guardado/eliminación en IO serializado mediante Mutex, sin caché de token; cancelación propagada |
| Proveedores | Constructores no envían peticiones. IA y Places opcionales, consentimiento y política intactos. GooglePlacesProvider se crea bajo demanda; no se optimiza sin medición de consultas autorizadas |
| DataStore | Lecturas asíncronas de preferencias/layout. Decodificación acotada del layout en flow; no se detectó acceso de red ni un bucle de lectura síncrona. Layout fallback y preferenciasLoaded conservados |
| Dashboard/Compose | Proyección/filas ya usan remember; render sin IO. StateFlow global actualiza el contenedor cada tick de telemetría. Sin profiler de recomposiciones no se cuantifica su coste ni se fragmenta toda la arquitectura |
| Device Lab | Se compone al abrir; checks pasivos solo por acción de usuario. Historial acotado, sin escaneo automático, audio o red al arrancar |
| StateFlow/timers | Un ticker foreground de1 s; cancela en background. Se conserva el poller OBD independiente y único, iniciado manualmente. No se añadió ningún timer |
| MediaSession | Antes refresh cada segundo incluso en demo, consultando autorización/posibles sesiones ajenas. Ahora no se refresca AndroidMedia mientras demo está activa y se cierran observadores al volver a demo. Reproductor real conserva callbacks y comprobación de revocación |
| Imágenes | Se usan bitmaps publicados por metadata; no descarga URI remota ni nueva decodificación/adaptador por tarjeta. No se afirma límite de tamaño o ausencia de presión de memoria OEM |
| Lifecycle/recursos | background cancela ticker/voz/probe, descarta edición, cierra media y desconecta OBD real. onCleared dispone Bluetooth/TTS y conversaciones. Pruebas de recuperación cubren estos límites; sin heap profiler no se certifica ausencia de fugas |

## C–E. Metodología, comparación y optimizaciones

Especificación y contratos ejecutables en specs/015 y PerformanceReadiness.kt;
reutilizan QualificationEnvironment/DeviceQualification/EvidenceLevel de Sprint 7.
Métricas instrumentadas, cotas externas, funcional y humano son categorías distintas.
Umbrales advisory requieren20 muestras con condiciones/escenario/métrica/unidad/método
iguales; no mezclan frío/caliente ni TTID/TTFD. Ningún contrato promueve emulador.

Macrobenchmark opt-in en módulo propio/proceso propio, paquete `.benchmark`, firma
debug, no debuggable y profileable solo allí, clave Places vacía. Deriva de release
configurada sin minificación; no representa una release optimizada de producción.
StartupTimingMetric y FrameTimingMetric oficiales; setup fuera de medida, compilación
None, arranques cold/warm/hot separados. ReportDrawnWhen espera preferencias,
onboarding, layout y snapshot, con dashboard visible y sin diálogo/editor/panel.
Un test recrea Ajustes y verifica que no declara dashboard listo. No se espera voz
o servicio opcional para considerar usable el texto. HOT puede omitir TTFD nuevo.

Correcciones mínimas: IO de credenciales fuera de Main; omitir MediaSession durante
simulación; reutilizar DateTimeFormatter del reloj. Evidencia previa: callsites
síncronos/periodicidad en el merge; regresiones Keystore/consentimiento/voz/media
aprobadas. La nueva suspensión incluye revalidación de consentimiento después del
lookup y antes del transporte. Sin medición aislada no se atribuye una mejora a
cada cambio ni se promete un porcentaje. Comparación completa, incluidas regresiones
y muestras instrumentadas, en PERFORMANCE_COMPARISON.md.

La pareja final consecutiva tras recuperar el host dio Activity p50/p95
2536/2899 ms (Sprint 7) y 2561/2917 ms (Sprint 8); cota UI 5639/6180 y 5714/6190 ms;
PSS 85943/86351 y 87001/87319 KiB. Cambios p50: +0,99%, +1,33% y +1,23%, respectivamente:
no se demuestra mejora de arranque. Se conserva la ligera diferencia desfavorable,
sin atribuir significación estadística ni causa a cada optimización.
Los 7 escenarios Macrobenchmark completaron 20 iteraciones válidas cada uno; frío
TTID/TTFD 1819/2420 ms p50/p95 en variante benchmark. No se compara directamente
esa variante con debug. Hay jank observado en los 4 recorridos de interacción;
fluidez en hardware y causa CPU/render siguen pendientes.

Incidencias de benchmark: el gesto inicial de scroll no produjo frames; se corrigió
con margen relativo a ambas dimensiones, velocidad acotada, espera y assertion de
desplazamiento. El diálogo del editor pertenece a otra ventana: se sustituyó el
selector de resource-id por descripción accesible/texto y scroll acotado. Se conservan
fallos previos y se repiten las tandas; cero frames nunca se convierte en cero jank.
Windows agotó paginación (error1455); se detuvo un daemon Gradle propio inactivo.
El emulador perdió ActivityManager durante las tandas: se recuperó el AVD existente,
sin wipe ni carga/guardado de snapshots, preservando hash de layout y APK. Un bloqueo
de snapshot de proceso ya cerrado se conservó en backup ignorado tras verificar que
no quedaba emulador activo. No se cerraron otras aplicaciones de Windows ni se
modificó el SDK global.
Los runners ahora verifican ActivityManager; no reinician automáticamente. La
estrategia None se bloquea antes de API34 para evitar reinstalación implícita de datos.
Estas incidencias limitan la comparación: host sin CPU/temperatura/memoria fijados.

## F. Estabilidad y recuperación

stability_android.py exige target emulador/opt-in y hashes del APK y AndroidTest
instalados, sin data clear. Ciclos de presets draft, background, lab pasivo, Places
sin consulta y recreate en proceso sostenido; después force-stop/HOME/rotación.
Preferencias restauradas en finally, layout comparado por hash, display y permiso
restaurados/verificados. PSS de proceso instrumentado y PSS de reinicios se separan.
No se concede un micrófono previamente denegado; flags de un permiso concedido que no se pueden reconstruir
provocan salto previo a la mutación. Observación de ANR/crash mediante exit-info API30+,
historial acotado; no se transforma ausencia observada en garantía.

Harness adicional:100 ciclos de consulta local, fallo TTS, cancelación/focus y media
fake;100 ciclos de NO DATA/pérdida OBD y retorno al simulador/cierre. Son dos casos
funcionales con repeticiones internas, no cientos de tests nuevos. Detalle de
ejecución, memoria y restauración en STABILITY_TEST_REPORT.md.
La tanda 20+20 duró 173,83 s; 0 crashes y 0 ANR observados en esa ventana. PSS del proceso
instrumentado subió 7007 KiB entre primera y última muestra; no demuestra una fuga
ni su ausencia. El fallo posterior de framework durante Macrobenchmark se registra
fuera de esa ventana. Permiso previamente denegado y display se restauraron en el
ciclo adicional; no se ejecutó revocación real de un permiso concedido.

## G–H. Android y hardware

Solo API35/Android 15 x86_64 disponible. API26/API30 sin imagen instalada ni dispositivo:
PENDING, sin descarga o cambio global del SDK. minSdk 26 no prueba compatibilidad.
Matriz exacta de pantallas en TEST_RESULTS y evidencia ignorada. No teléfono físico,
ELM327 ni coche detectados/autorizados; ninguna instalación o prueba física activa.
Voz audible/STT/TTS es-ES, TalkBack, audio focus OEM/ruido, OBD detenido, reproductores,
Maps/Waze/GPS/contactos ficticios y proveedores comerciales siguen PENDING por separado.
Recorridos y acta preparados en PHYSICAL_TEST_PROCEDURES.md, sin fabricar evidencia.

## I–J. Seguridad y regresión

184 casos funcionales: Python 36/core 43/harness 67/app 6/instrumentación 32; 0 fallos y 0
omitidas. Benchmarks, muestras y ciclos repetidos no se suman. Lint:0 errores y 12
avisos existentes. Repository check y git diff check aprobados; suites y builds en
TEST_RESULTS. Permisos/network policy/backup/Keystore/OBD whitelist/privacy/provider
consent conservados. Sin credenciales comerciales, VIN, CAN libre, llamada automática,
telemetría enviada a IA ni escucha permanente. La incidencia de desinstalación
automática del baseline se registra en A; no se repitió en la validación final.

Se conservan intentos de medición fallidos: UI_DUMP_FAILED bajo carga de compilación,
APK ausente tras connectedAndroidTest y onboarding pendiente tras reinstalación.
Se repitió secuencialmente con precondiciones válidas. Errores iniciales de compilación
del nuevo test y overlay de Manifest se corrigieron y reconstruyeron; no se desactivó
ninguna regla/test para obtener verde. No se ocultan como muestras correctas.

## K–M. APK, preparación y límites

APK final, firma, SHA-256 y resultados numéricos se registran en TEST_RESULTS,
PERFORMANCE_COMPARISON y STABILITY_TEST_REPORT. Artefactos en artifacts/sprint8,
ignorados por Git. Ver RELEASE_READINESS.md: incluye componentes debug exportados,
dependencias/secretos/network/backup, límites offline/hardware y firma de producción
PENDING. Sin auditoría CVE exhaustiva, heap profiler ni prueba acústica humana.
Una candidata técnica de revisión no es una release de producción.

APK debug: `artifacts/sprint8/CopilotDrive-AI-sprint8-debug.apk` (19825143 bytes),
paquete `ai.copilotdrive.app`, minSdk 26/target 35, versionCode 1/versionName 0.2.0-debug,
firma Android Debug verificada. SHA-256:
`C23F86E1B7DC7F3DEA05D9AF6E41374EF2B79A58D1227B302B7CA1AB12400EA2`.
El APK instalado final coincide con ese hash. Artefactos y trazas no están en Git.

## N–P. Commits, siguiente sprint y PR

Commits de implementación: `2730196` (instrumentación/contratos/runners/IO), `259af8d`
(dashboard visible y benchmark en proceso aislado) y `b9f3d9f` (guardas de framework,
preservación y gestos/selectores de benchmark). El commit documental posterior
registra evidencia; no cambia el binario. Ningún commit se ha publicado.
Sprint 9 recomendado: teléfono/voz/TalkBack, ELM327/coche detenido, API26/30 y proveedores
opcionales con cuota/consentimiento; corregir solo problemas observados.

Publicación posterior, solo con autorización: revisar `git diff main...HEAD`,
comprobar artefactos, `git push -u origin feat/sprint8-performance-readiness`, abrir
PR a main con este informe y esperar Android/Tests de la rama. No ejecutado aquí.
Merge, release y firma de producción requieren decisiones independientes posteriores.

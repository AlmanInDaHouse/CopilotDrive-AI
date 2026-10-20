# Sprint 6 — Dashboard 2.0

Actualización al iniciar Sprint 7: PR #7 fusionada en main
`1328077ff16d6487b4be995aae474dfc8452d345`; workflows Android y Tests del merge
aprobados. El texto de entrega local de este informe es histórico. La siguiente
rama de trabajo es `feat/sprint7-device-qualification` (SPRINT7_REPORT.md).

Fecha: 2026-10-20. Rama local: `feat/sprint6-custom-dashboard`.
Base: `2099b9d560b0b269f2b369ab9e8cc66fe235b837`, Sprint 5/PR #6.
Se ejecutó fetch y main avanzó por fast-forward a esa base. El cambio previo de
ROADMAP se conservó, con copia exacta fuera del repositorio. Sin push, merge remoto ni release.

## A. Arquitectura

Core Kotlin puro: contratos tipados/versionados, WidgetRegistry cerrado, validación,
codec, migración, presets, first-fit y DashboardEditSession. App: repositorio DataStore,
un coordinador de personalización separado de DashboardState y renderer Compose.
Sin ViewModel por tarjeta ni cambios en ToolRegistry/PolicyEngine. Detalles:
[arquitectura](DASHBOARD_WIDGET_ARCHITECTURE.md), [ADR-007](DECISIONS/ADR-007-dashboard-layout.md)
y [especificación ejecutable](../specs/012-widgets-future/spec.md).

## B. Widgets

| Tipo | Fuente y comportamiento |
|---|---|
| Vehicle | VehicleContextEngine: combustible, velocidad, RPM, refrigerante; origen, simulación y disponibilidad explícitos |
| Media | MediaSession/demo existentes: carátula local, título/artista/app/estado y controles soportados por ToolRegistry |
| Navigation | Destino de sesión y botones Maps/Waze con disponibilidad; herramientas originales, sin ETA ni mapa ficticio |
| Assistant | VoiceSessionCoordinator: estado, respuesta, transición ligera del orb y acceso a voz; errores visibles |
| Places | Resultados/selección/rating de la sesión; abre panel, sin búsquedas al renderizar |
| ObdStatus | Bluetooth, ELM327, PIDs y error; acceso al diagnóstico existente |
| Clock | Hora local del dispositivo en 24 horas |
| SystemStatus | Capacidades, tres diagnósticos recientes y Device Lab; disponibilidad no acredita hardware |

## C. Editor

Añadir, ocultar/mostrar, subir/bajar, cambiar tamaño, preview, guardar, cancelar,
restaurar Classic y presets. Una instancia por tipo; no se oculta la última visible.
Posiciones discretas, tamaños admitidos y cero solapes. Preview no ejecuta acciones
externas de las tarjetas. [Instrucciones](DASHBOARD_CUSTOMIZATION.md).

Laboratorio y confirmación explícita de entorno de prueba detenido, TTL monotónico
de 120 s. UNKNOWN nunca se trata como PARKED. Movimiento fresco, revocación,
desconexión OBD real o background descartan borrador/confirmación; se cancela IO en curso.
Confirmar no verifica aparcamiento. Ninguna herramienta LLM puede cambiar diseños.
Barra fija con voz, respuesta completa accesible, PTT, ajustes y origen vehicular.

## D–F. Capturas, presets y pantallas

Presets: **Classic**, **Driver**, **Media**, **Laboratory**. Son solo layouts.
Referencia anterior conservada en `LegacyDashboardScreen`; Classic deriva de ella.
Las capturas locales están en `artifacts/sprint6/` (ignoradas por Git):
`editor.png`, `preview.png`, `offline-fuel.png` y `dashboard-<resolución>-dpi<dpi>-font<escala>.png`.

| Emulador: píxeles de pantalla | Densidad | fontScale | Evidencia |
|---|---|---|---|
| 800×480 | 160 dpi | 1.0 | Captura, controles fijos, reinicio y persistencia |
| 1024×600 | 160 dpi | 1.0 | Captura, controles fijos, reinicio y persistencia |
| 1280×720 | 160 dpi | 1.0 | Captura, controles fijos, reinicio y persistencia |
| 1920×1080 | 160 dpi | 1.0 | Captura, controles fijos, reinicio y persistencia |
| 360×640 vertical | 160 dpi | 1.0 | Captura, controles fijos, reinicio y persistencia |
| 1280×800 tablet | 160 dpi | 1.0 | Captura, controles fijos, reinicio y persistencia |
| 800×480 | 160 dpi | 1.5 | Captura y controles fijos con fuente ampliada |
| 1280×720 | 240 dpi | 1.0 | Captura y controles fijos con densidad distinta |

Se cambió el display del emulador mediante wm, se arrancó MainActivity y se
comprobaron bounds de controles esenciales con UIAutomator. La barra del sistema
reduce espacio útil: los valores de pantalla no se equiparan a dp disponibles.
Compose además ejecutó 800×480, 1024×600, 1280×720, 360×640 dp y fontScale 1.5
en composiciones controladas. Son pruebas de emulador API 35, no dispositivos físicos.

La inspección inicial de 800×480 detectó combustible fuera de la vista inicial.
Se compactó la cabecera baja y se hizo proporcional la proyección de spans; se
añadió un assert de combustible visible antes de scroll y se repitieron las pruebas.
La captura vertical detectó un solape de controles fijos: se corrigió su contenedor
y se añadieron asserts de no intersección tanto en Compose como en el smoke UIAutomator.
El contenido completo sigue desplazable. No se afirma validación de fuentes extremas
ni de todas las combinaciones posibles de pantalla, densidad y barras del sistema.

## G. Persistencia

DataStore dedicado al diseño no sensible. JSON v1 estricto con límites y validación.
Migración v0 de las mismas instancias, sin preset obligatorio, a v1 en memoria;
el siguiente guardado explícito escribe v1. No hay importador de terceros.
Ausencia/corrupción/versión futura/tipo desconocido/layout inválido recuperan Classic
con aviso. Error IO de lectura usa Classic temporal; fallo de escritura mantiene
borrador y original. No se guardan confirmación, conversación, ubicación ni credenciales.
Roundtrip real DataStore, reapertura de archivo, nueva Activity y force-stop/reinicio
conservan layout. Una escritura atómica ya terminada no se revierte por background.

## H. Pruebas ejecutadas

Baseline canónico aislado: Python **12/12**, core **17/17**, testing **56/56**, app
**2/2**, lint y ambos APK: **BUILD SUCCESSFUL**. El primer intento de log con redirección
PowerShell se interrumpió por un aviso SDK en stderr; se repitió sin esa redirección.
No se contabiliza ese intento como una validación aprobada.

Validación final Windows/JDK 17/SDK 35:

- Python `unittest discover`: **12/12**.
- `:core:test`: **33/33**, incluidas 16 pruebas del motor/coordinador de layouts.
- `:testing:test`: **56/56**, incluidas regresiones OBD, voz, Places, privacidad y políticas.
- `:app:testDebugUnitTest`: **6/6**, presentación y DataStore real/IO/cancelación.
- `:app:connectedDebugAndroidTest`: **23/23**, emulador Android 15/API 35.
- Total de la suite: **130/130**, sin omitidas. Repeticiones smoke se registran aparte.
- `:app:lintDebug`: **0 errores, 12 avisos** de dependencias/UseKtx existentes.
- `:app:assembleDebug :app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**.
- `check_repository.py` y `git diff --check`: correctos; sin permisos nuevos.

Instrumentación: dashboard/simulador/ausencia de datos, MediaSession, Settings/Device
Lab, Places demo, IA loopback, Keystore, editor completo, preview, guardar/cancelar,
restauración, recreación de Activity, revocación/background y tamaños/fuente descritos.
Harness: corrupción, versiones, tipos/tamaños/posiciones inválidos, duplicados, solapes,
presets, migración, guardado/recarga, cancelación, IO, simultaneidad y seguridad temporal.

Después se instaló el APK y el APK de tests con adb, se repitió el flujo completo
del editor para obtener capturas y se ejecutó la consulta local de combustible con
Wi-Fi y datos desactivados: **1/1**, respuesta escrita simulada 42 %. No requirió IA
ni Internet. Captura de pantalla y buffer de crashes guardados; sin FATAL EXCEPTION
durante el smoke. Tamaño, densidad, fontScale, Wi-Fi y datos se restauraron al valor previo.

XML originales: `android/**/build/test-results/` y
`android/app/build/outputs/androidTest-results/connected/debug/`.
Evidencia local adicional: `artifacts/sprint6/screen-evidence.json`, `test-summary.json`,
`offline-query.txt`, `crash-log.txt`, `apk-signature.txt`, capturas y XML UIAutomator.
La matriz se puede repetir con `scripts/sprint6_emulator_smoke.py`; sus prerequisitos
están en la cabecera. Solo admite emulador y restaura display/fuente/red en finally.

## I–K. Rendimiento, APK y limitaciones

No se midieron FPS, recomposiciones, consumo ni latencia del editor. Se reutilizan
los StateFlow/proveedores originales y el polling existente; renderizar no hace IO.
No se promete un benchmark ni rendimiento físico por estas decisiones de código.

APK: `artifacts/CopilotDrive-AI-sprint6-debug.apk`, **19.825.143 bytes**.
SHA-256: `1FD9B5C3682896BA9100F70C9AAC0853063DA5BC6F8DC2CED117EECD66707F91`.
Firma debug v2 verificada por apksigner, un firmante Android Debug; sin firma de producción.
Certificado SHA-256: `AD561B54E005E27E4CA2873963EE7F6C34C4E246FEB9EA76AA4CC2E2EB476A8A`.

Pendientes: pantallas físicas, TalkBack real, voces acústicas, ELM327/coche,
reproductores comerciales, Google Places con clave/cuotas, GPS y Maps/Waze instalados.
`PHYSICAL_VALIDATION_MATRIX.md` permanece sin convertir emulador/fakes en validación física.
Sin AAOS, Android Auto ni CarPlay; es una APK convencional para Android abierto.
No drag-and-drop, widgets remotos ni edición en marcha. Véase KNOWN_LIMITATIONS.md.

## L–N. Commits y siguiente entrega

- `ce16345` — especificación, ADR y harness antes de implementar el editor.
- `c121d75` — motor versionado, contratos y repositorio DataStore.
- `a6364d4` — widgets adaptativos, editor de laboratorio e integración.
- `a2c959c` — proyección proporcional, combustible visible en pantalla baja y revocación Bluetooth robusta.
- `b941c2a` — controles de voz/ajustes separados en vertical y pruebas geométricas.
- `fc69927` — smoke reproducible de resoluciones/densidad/fuente/red, limitado a emulador.
- Commit final de documentación/evidencia: consultar `git log --oneline`.

Sprint 7 recomendado: interoperabilidad física y accesibilidad, reparar problemas
observados y medir rendimiento con dispositivos concretos. Mantener la edición
limitada a laboratorio; no ampliar permisos o herramientas del LLM de forma implícita.

La publicación de Sprint 6 ya se completó mediante PR #7; no volver a publicar esa
rama. Para la PR de Sprint 7, seguir SPRINT7_REPORT y NEXT_STEPS con autorización
separada de push. La evidencia histórica de este informe no sustituye el CI de la
nueva rama ni autoriza una release.

# Sprint 10.1 — primer piloto físico Android

Fechas: 2026-10-20 a 2026-10-21, Europe/Madrid. **Instalación y apertura autorizadas y ejecutadas;
APK instalado idéntico al original. Bienvenida y dashboard confirmados por el usuario.**
**TTS guiado es-ES PHYSICALLY_VERIFIED**: callback completado y audibilidad humana confirmada.
**STT/Jarvis de gasolina PHYSICALLY_VERIFIED en intento 2**, incluida audibilidad
y declaración hablada de simulación. Primer intento FAILED por STT_TIMEOUT conservado.
**Cancelación, retorno sin autoescucha y nueva pregunta PHYSICALLY_VERIFIED** por
observación humana de la secuencia guiada. **Silencio controlado y recuperación posterior
PHYSICALLY_VERIFIED** mediante confirmación humana separada.
Restantes escenarios sin observación permanecen **PENDING**.
Sesión `sprint10-1-honor-20261020T210205Z`; las observaciones se limitan a cada escenario.
**Sesión cerrada con objetivo de voz local cumplido y pendientes explícitos.**
TalkBack manual se omite por decisión del usuario. Ajustes de pantalla originales
restaurados y distribución que el usuario guardó conservada; APK original sin cambios.

## Base y alcance

- Checkout: `opencar-copilot`; rama inicial `feat/sprint10-physical-pilot`, árbol limpio.
- HEAD de la sesión al instalar/verificar el APK: `de7c4c8c678550ac4902d7c4768c8e4bcc56e7de`.
- Corrección del runner anfitrión guardada posteriormente en
  `94aa2be67bfa5f3bcf7f58b46200ae02b45901d8`; no cambia fuentes Android ni APK.
- Documentación de esta campaña en `feat/sprint10-1-physical-validation`, basada en ese HEAD.
  Corrección mínima del runner pasivo y regresiones Python; sin cambios Android,
  compilaciones ni publicación. Instalación y apertura físicas tras autorización explícita.
- Sprint 10 registra 215 casos distintos y 37 instrumentados por API26/30/35.
  Es evidencia histórica consultada. Esta campaña repitió únicamente Python:
  56 casos aprobados, incluidos cuatro nuevos; no se suma a los 215 como suite Android nueva.
- Procedimientos revisados: `AGENTS.md`, `PHYSICAL_PILOT_GUIDE.md`,
  `DEVICE_QUALIFICATION.md`, `PHYSICAL_VALIDATION_MATRIX.md` y runners existentes.

## A. Teléfono y descubrimiento pasivo

SDK obtenido de `android/local.properties`:
`C:/Users/manul/AppData/Local/CopilotDriveAndroidSdk`.
ADB: `platform-tools/adb.exe`; no localizado en PATH. Build-tools disponibles: 35.0.0.

`adb devices -l` mostró un único destino autorizado de modelo
`Android SDK built for x86_64`, transporte emulado. La consulta de solo lectura
`getprop ro.kernel.qemu` devolvió `1`, confirmando emulador. Ningún destino físico,
`unauthorized` u `offline` observado. No se utilizó el emulador para sustituir pruebas físicas.

`Get-PnpDevice -PresentOnly` se completó correctamente: cero candidatos
Android/ADB/MTP/WPD según clase/nombre y cero dispositivos USB presentes con estado
PnP distinto de OK. No se instalaron ni modificaron drivers. Esta consulta no prueba
que el cable transmita datos ni descarta depuración desactivada o un driver OEM ausente.
La conexión y el cable requieren comprobación humana.

### Seguimiento tras confirmar el usuario la conexión USB

2026-10-20T20:54:10Z (22:54:10 Europe/Madrid), mismo commit y APK de esta acta:
Windows detecta ahora **HONOR 400 Lite**, clase WPD, estado OK. El controlador
relacionado de MTP identifica el modelo como **ABR-NX1**, usa `wpdmtp.inf` de Microsoft,
firmado, versión `10.0.26100.9711`. Controladores relacionados USB Composite/Root Hub
también de Microsoft y firmados. Sin errores PnP USB presentes en la consulta.
No se instalaron ni cambiaron controladores. Esto acredita enumeración USB/MTP,
no una interfaz ADB funcional ni compatibilidad completa del driver de depuración.

El nuevo descubrimiento ADB sigue mostrando exclusivamente un emulador autorizado;
el teléfono no aparece como `unauthorized` ni `offline`: **no aparece en ADB**.
Se pidió al usuario habilitar Depuración USB y aceptar RSA si aparece. Esa acción
humana está pendiente de confirmación; no se asume que esté desactivada ni se atribuye
el bloqueo a una causa definitiva. No se reinició el servidor ADB ni el teléfono.

### Teléfono autorizado tras activar depuración y aceptar RSA

El usuario confirmó ambas acciones. A las 2026-10-20T20:55:16Z, ADB mostró un teléfono
autorizado y un emulador, seleccionados por separado. A las 20:55:35Z se completó
el perfil pasivo, ligado al commit y SHA-256 de esta acta:

| Propiedad ADB de solo lectura | Resultado |
| --- | --- |
| Fabricante / modelo Android | HONOR / ABR-NX1 (Windows: HONOR 400 Lite) |
| Android / API | 16 / 36 |
| ABI | arm64-v8a, armeabi-v7a, armeabi |
| Resolución física | 1080 × 2412 px; sin override informado |
| Densidad física | 480 dpi; sin override informado |
| fontScale actual | 1.0 |
| App en usuario actual | No instalada; consulta `pm list packages --user current ai.copilotdrive.app` exit 0 y vacía |
| Permisos concedidos al paquete | No aplicable: paquete ausente |

No se recopilaron seriales, IMEI, MAC ni otros identificadores personales en los informes.

## B. APK candidato y estado de instalación

APK original: `artifacts/sprint10/CopilotDrive-AI-sprint10-debug.apk`.
Existe y tiene 19.744.628 bytes. No se ha sustituido ni reconstruido.

SHA-256 calculado y coincidente con el esperado:
`fcec5256a0c0911eefe22ca2492db45a9374bcb45ace81ba9269ae664b7e643c`.

`pilot_package.py` sin destino completó el **preflight local PASS**.
La verificación directa `apksigner verify --verbose --print-certs` confirmó firma
v2 válida y un firmante Android Debug. Certificado SHA-256:
`ad561b54e005e27e4ca2873963ee7f6c34c4e246feb9ea76aa4cc2e2eb476a8a`.

`aapt dump badging` confirmó:

| Campo | Resultado |
| --- | --- |
| Paquete | `ai.copilotdrive.app` |
| Versión | versionCode 1; `0.2.0-debug` |
| minSdk / targetSdk | 26 / 35 |
| Launcher | `ai.copilotdrive.app.MainActivity` |
| ABI empaquetadas | arm64-v8a, armeabi-v7a, x86, x86_64 |

Permisos empaquetados verificados por el runner: RECORD_AUDIO, READ_CONTACTS,
INTERNET, ACCESS_COARSE_LOCATION, BLUETOOTH, BLUETOOTH_CONNECT,
ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE y permiso interno
`ai.copilotdrive.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
Sin ACCESS_FINE_LOCATION en ese listado. Declaración no implica concesión.

Procedencia: `SPRINT10_REPORT.md` vincula el APK al código congelado en
`b157bc3be9da0fd6a79ada6e92b48696441e009f`; `git diff` entre ese commit y `de7c4c8`
no muestra cambios en `android` ni `scripts`. El commit posterior `94aa2be` afecta
solo al runner anfitrión y sus regresiones. La coincidencia de hash identifica
el artefacto original; no se ha realizado una compilación reproducible nueva.

Preflight físico pasivo repetido a las **2026-10-20T20:56:35Z: PASS**.
API36 cumple minSdk26; ARM64 está empaquetada y coincide con el teléfono.
Consulta del runner `pm list packages ai.copilotdrive.app` sin coincidencias;
app previa ausente, certificado/versión previos no aplicables y signatureConflict=false.
No existe downgrade de una versión instalada en este estado observado.
Es compatibilidad de instalación por metadatos, no verificación de funcionamiento OEM.

### Instalación y apertura autorizadas

El usuario otorgó aprobación explícita después de revisar teléfono, hash y alcance:
instalar con `adb install -r`, abrir la app y realizar las pruebas locales guiadas de
TTS/STT/Jarvis/recuperación. La iniciación de audio y micrófono permanece manual.
Esta autorización no abarca rendimiento, proveedores, contactos, navegación ni OBD.

Se revalidaron modelo, hash, firma y ausencia de conflicto; se ejecutó primero
`qualify_android.py` con destino explícito, permisos de instalación/actividad física
y `--dry-run`. Después se ejecutó el mismo plan autorizado con instalación `-r`
y apertura de MainActivity. Finalizó a **2026-10-20T21:02:05Z (23:02:05 Europe/Madrid)**.
El runner comprobó por SHA-256 que el APK instalado coincide exactamente con el candidato
y que `am start -W` devuelve Status OK. No downgrade, uninstall, pm clear, grants,
instrumentación ni cambios globales. `status=PENDING` del runner físico describe
calificación pendiente, no un fallo de instalación.

Primera sonda UI: app aún no detectada en primer plano. Tras pedir al usuario revisar
la pantalla, confirmó la bienvenida «Antes de empezar». Sonda posterior a las
21:02:57Z: proceso presente, app reanudada, pantalla desbloqueada y sin registros de
salida del paquete en exit-info. La causa del estado transitorio inicial no se atribuye
a un defecto ni a un bloqueo OEM. No se exportó logcat ni contenido de pantalla.

Permisos runtime observados tras instalar: micrófono, contactos, ubicación aproximada
y Bluetooth Connect **no concedidos**. El usuario concederá micrófono al iniciar PTT;
los demás no son necesarios para esta prueba de voz local.

Bienvenida visible: **PHYSICALLY_VERIFIED**, observación humana para este escenario
concreto en `onboarding-observation.json`. Después, el usuario confirmó dashboard
visible y comprobación pasiva de voz realizada. **Dashboard visible PHYSICALLY_VERIFIED**,
acta independiente `dashboard-observation.json` recogida a las 2026-10-20T21:21:09Z
(23:21:09 Europe/Madrid). No acredita todavía ocho widgets, presets, accesibilidad
ni un recorrido de voz completo.

Sonda del laboratorio en ese momento: estado acústico CANCELLED y observación humana
PENDING, con control de reproducción visible. No se deduce causa, intento completado,
audibilidad negativa ni fallo TTS de ese estado. Se solicita iniciar la frase mediante
pulsación humana y registrar por separado callback y resultado audible.

## C–J. Resultados físicos y limitaciones

| Área solicitada | Estado | Resultado observado / pendiente |
| --- | --- | --- |
| C. STT es-ES | PHYSICALLY_VERIFIED en intento2; intento1 FAILED | Reintento manual reconoce la frase; TRANSCRIPT_OBTAINED observado. Primer STT_TIMEOUT conservado, sin causa definitiva |
| D. TTS es-ES | PHYSICALLY_VERIFIED | Frase iniciada por usuario; COMPLETED observado y audibilidad completa/clara en español confirmada por usuario |
| E. Jarvis completo | PHYSICALLY_VERIFIED en intento2; intento1 FAILED | Transcripción → orquestador → herramienta solicitada/autorizada → texto → TTS_COMPLETED; usuario confirma voz clara y aviso de simulación |
| F. Confirmación acústica humana | PHYSICALLY_VERIFIED en escenarios documentados | Frase guiada y respuesta Jarvis intento2 audibles en español; simulación explícita confirmada para Jarvis |
| G. Recuperación | PHYSICALLY_VERIFIED para escenarios documentados | Usuario confirma cancelación, background/retorno sin autoescucha, nueva pregunta y recuperación tras silencio; error de motor/interrupción pendientes |
| H. Pantalla y accesibilidad | PHYSICALLY_VERIFIED en escenarios registrados; TalkBack PENDING | Dashboard/lectura, cuatro presets y ocho widgets confirmados. Fuente máxima y ambas orientaciones revisadas manualmente; factor máximo no capturado. Restauración1.0 verificada; usuario omite TalkBack |
| I. Compatibilidad OEM | PENDING más allá de escenarios documentados | En este HONOR/API36 funcionan TTS y Jarvis intento2; timeout inicial sin causa. No se homologa familia OEM, energía ni todas las condiciones de ciclo de vida |
| J. Correcciones | AUTOMATED_TESTED | Runner pasivo corregido para paquete ausente; cuatro regresiones nuevas y suite Python 56/56 aprobada |

El fallo histórico de Pico TTS en el emulador API26 consta en Sprint 10 y no se
extrapola a un teléfono. No se observó un fallo OEM en esta campaña.
Bienvenida, dashboard visible, frase TTS guiada y STT/Jarvis de gasolina intento2 tienen
confirmación humana propia PHYSICALLY_VERIFIED. No se promueven otros escenarios por
presencia de un servicio o callback.

### TTS acústico guiado, intento registrado 1

El usuario inició la reproducción manual y confirmó haber oído la frase completa y
claramente en español. Inicialmente el panel estaba fuera de pantalla; esas sondas
sin estado visible conservan callbackCompleted=null, no un fallo ni un SUCCESS inventado.
Se navegó mediante scroll dentro del laboratorio, sin iniciar audio. A las
**2026-10-20T21:23:36Z (23:23:36 Europe/Madrid)** se observaron juntos
**Estado técnico COMPLETED** y **Observación humana CONFIRMED**.

Acta `tts-observation.json`, escenario `tts.es_es.guided_phrase`, intento 1 de esta
campaña, misma sesión/commit/APK/modelo/API. El callback y el oído humano son evidencias
separadas y ambas están presentes. Alcance: una frase guiada local es-ES, no calidad
de todos los motores/voces, STT, Jarvis E2E ni comportamiento offline. Nombre individual
de voz y motor activo no expuestos por el panel; catálogo previo: 6 voces locales.
No se grabó audio ni se conservó texto reconocido.

### Primer intento STT/Jarvis: timeout, causa pendiente

Tras la instrucción de PTT manual, el usuario informó el indicador de etapa fallida.
RECORD_AUDIO estaba concedido; contactos/ubicación/Bluetooth Connect seguían denegados.
No se habilitó fallback de reconocimiento del sistema ni IA externa por automatización.

Al inspeccionar inicialmente, otra app estaba en primer plano; no se leyó su contenido.
Se reanudó CopilotDrive sin force-stop ni nueva escucha. Diagnóstico a las
**2026-10-20T21:30:01Z (23:30:01 Europe/Madrid)**:
REQUEST_STARTED → STT_LISTENING → FAILED; tras background también figura CANCELLED.
El registro de fallo conserva **Etapa de voz STT: STT_TIMEOUT**. CANCELLED posterior
no sustituye ni oculta el timeout previo. El coordinador aplica 15 segundos de límite
a STT; STT_LISTENING describe solicitud de escucha, no acredita onReady ni captura real.

Acta `stt-attempt1-observation.json`, intento 1, mismo dispositivo/build/sesión.
No se observó transcripción ni recorrido posterior de IntentResolver/ToolRegistry/
VehicleContextEngine/respuesta escrita/TTS Jarvis. El TTS guiado anterior mantiene
su resultado positivo independiente. No se atribuye todavía el fallo a micrófono,
idioma, servicio Google, red, bug de la app o restricciones OEM.

Se solicitó un único reintento humano manteniendo la app en primer plano hasta el
resultado. No se abre el micrófono automáticamente ni se cambia configuración global.

### Segundo intento STT/Jarvis: recorrido completo confirmado

El usuario inició otro PTT, mantuvo la app durante el intento y confirmó que ahora
reconoce y responde. Traza acotada leída a las **2026-10-20T21:34:08Z
(23:34:08 Europe/Madrid)**:

`REQUEST_STARTED → STT_LISTENING → STT_TRANSCRIBING → TRANSCRIPT_OBTAINED →
ORCHESTRATOR_PROCESSING → TOOL_REQUESTED → TOOL_AUTHORIZED → TEXT_RESPONSE →
TTS_STARTED → TTS_COMPLETED`.

STT_TRANSCRIBING figura dos veces, sin convertirlo en dos intentos. CANCELLED posterior
al background no invalida la finalización observada de esta respuesta. Ante la pregunta
acústica específica, el usuario confirmó **voz clara y datos simulados indicados**.
Se conservan solo etapas/confirmaciones, sin transcripción ni contenido de respuesta.

Actas `stt-attempt2-observation.json` y `jarvis-attempt2-observation.json`, intento2,
misma sesión/commit/APK/modelo/API: **PHYSICALLY_VERIFIED para ese recorrido local**.
El primer intento FAILED permanece independiente. Se observó recuperación por reintento;
no se demuestra eliminación del timeout ni fiabilidad universal o repetición en frío.

La traza física expone orquestador/herramienta/texto, no nombres de componentes internos.
La conexión IntentResolver → ToolRegistry → VehicleContextEngine se comprobó en el
código Android/core sin cambios: resolver de gasolina, `vehicle.get_fuel`, snapshot
del contexto y prefijo de simulación antes de TTS. Es evidencia del recorrido junto
a etapas/resultados humanos; no se presenta como medición individual de cada clase.
No se activó IA externa para el recorrido ni se abrió micrófono por automatización.

### Recuperación manual confirmada

El usuario completó y confirmó las tres acciones guiadas: iniciar PTT y cancelar sin
hablar; iniciar otro PTT y salir a Inicio, volver sin escucha automática; pulsar otra
vez para la pregunta de gasolina y obtener respuesta. Actas recogidas a las
**2026-10-20T21:38:40Z (23:38:40 Europe/Madrid)**, mismo teléfono/commit/SHA-256:

- `voice.cancel.manual`: PHYSICALLY_VERIFIED.
- `voice.background.return_no_autolisten`: PHYSICALLY_VERIFIED.
- `voice.recovery.next_manual_question`: PHYSICALLY_VERIFIED.

Método: observación humana explícita, sin grabación continua ni instrumentación.
Se volvió a leer SHA-256 del APK instalado: idéntico al original. Esta secuencia no
califica background prolongado, ahorro de batería OEM, error de motor inducido o
interrupción por otras apps/llamadas. No se provocaron interrupciones personales.

El usuario completó después el silencio controlado con PTT manual y una pregunta
posterior, y confirmó que la escucha termina y la siguiente pregunta vuelve a funcionar.
Acta `silence-recovery-observation.json`, escenario
`stt.silence_then_manual_recovery`, intento1, recogida a las
**2026-10-20T21:43:18Z (23:43:18 Europe/Madrid)**: PHYSICALLY_VERIFIED para fin de
escucha y recuperación observados. No se instrumentó duración exacta ni código de
error del motor; no se inventa STT_SILENCE ni STT_TIMEOUT para esta prueba.
Es distinta del timeout inicial, en el que se había pedido hablar.

### Pantalla y disponibilidad de TalkBack

El usuario confirmó respuesta a la consulta local de estado del coche, pero inicialmente
no encontró «Leer respuesta». Se indicó que la entrada está en la franja de estado de
voz sobre PTT. Sonda a las 21:46:03Z: nodo `voice-status` habilitado y pulsable en
[48,1842][1032,1986], encima de PTT [48,1998][1032,2166]. No se exportó la respuesta.
Después el usuario confirmó que lo había encontrado y que la revisión estaba bien.

Actas `read-response-observation.json` y `dashboard-visual-observation.json`, recogidas
a las **2026-10-20T21:47:26Z (23:47:26 Europe/Madrid)**:
**PHYSICALLY_VERIFIED para diálogo de respuesta y controles/visualización del dashboard
actual a fontScale1.0**, no para todos los widgets/presets ni fuentes/orientaciones.
La dificultad inicial para descubrir la entrada se conserva como observación de
usabilidad. No se estableció un defecto reproducible de renderizado: no se cambió
Android ni se inventó una regresión aprobada para un recorte no confirmado.

Sonda pasiva a las **2026-10-20T21:45:08Z (23:45:08 Europe/Madrid)**: app visible,
área UI 1080×2412 y etiquetas de PTT/Probar consulta/Ajustes dentro de esa área.
Son límites de nodos de texto, no medición de tamaño de objetivos táctiles ni prueba
de legibilidad. El paquete TalkBack está **instalado** para el usuario actual y su
servicio **no habilitado**. No se modificó ningún servicio. Prueba manual TalkBack
PENDING, requiere intervención del usuario; presencia no equivale a funcionamiento.

El usuario eligió continuar con pantalla y accesibilidad. Baseline de solo lectura a
las **21:49:28Z**: distribución guardada SHA-256
`60e75bdea738814ef9b80ace1813759ec3525a6798703f4bbb3261b8029780f8`, fontScale1.0,
autorrotación1, user_rotation0,1080×2412/480dpi. No se exporta contenido de distribución.
Presets y ocho widgets se revisarán en borrador/vista previa y se cancelarán sin guardar.
El laboratorio caduca a los dos minutos y se abandona al salir de la app: los bloques
manuales deben completarse antes de volver al chat. No se elimina esa guarda.

El usuario confirmó vistas Classic y Driver correctas y cancelación. La comprobación
de conservación a las **21:55:56Z** detectó SHA-256 guardado
`99f920b5bbe160636537e3c21be802a09fc5a0adb3a13874b6ce47cc0da3c8de`, distinto del
baseline. Se detuvo el siguiente bloque de presets. Lectura exclusivamente del archivo
no sensible de layout a las **21:56:42Z**: preset guardado MEDIA y huella
`0ded11bda938f691bf0eb3e29ffa261e6d6beec3d585e2bcbab71c9e11369c51`.
El formato del layout no contiene fechas; causa/acción que guardó el cambio aún sin
confirmar. Se preguntó si se pulsó Guardar o solo Vista previa/Cancelar.

Estado de conservación en ese momento: **BLOCKED por aclaración de acción**, no defecto Android
demostrado ni aprobación de cancelación. La comparación de huellas fallida se conserva;
las vistas Classic/Driver fueron confirmadas visualmente por el usuario, pero no se
mezcla esa observación con preservación de datos. Ningún comando de la campaña escribió,
restauró o borró el layout. Voz/TTS/Jarvis mantienen sus resultados independientes.

Aclaración posterior: el usuario confirmó que **sí había pulsado Guardar** al probar
los presets. Esto explica una escritura solicitada manualmente; no hay defecto de
cancelación demostrado. Bloqueo de aclaración levantado. Se mantiene la distribución
actual elegida por el usuario, sin restauración automática ni sustitución del baseline
original en las evidencias.

Nuevo baseline para ensayos exclusivamente de vista previa:
**2026-10-20T22:01:33Z = 2026-10-21 00:01:33 Europe/Madrid**, SHA-256 guardado
`99f920b5bbe160636537e3c21be802a09fc5a0adb3a13874b6ce47cc0da3c8de`.
Archivo `display-baseline-after-manual-save.json`. La preservación al cancelar sigue
**PENDING de un intento nuevo sin Guardar**. Se solicitó Media/Laboratory en vista
previa y cancelación; no se confunde renderizado confirmado con conservación probada.

Ensayo nuevo completado: el usuario confirma Media y Laboratory correctos en vista
previa y cancelación sin guardar. A las **2026-10-20T22:06:18Z = 2026-10-21 00:06:18
Europe/Madrid**, la huella guardada sigue siendo exactamente `99f920b5…3c8de`.
Acta `presets-media-laboratory-observation.json`: **PHYSICALLY_VERIFIED de ambas vistas
y conservación al cancelar este borrador**. Classic/Driver conservan su confirmación
visual anterior. Los cuatro presets están revisados visualmente, sin calificar
interacciones con proveedores a partir de la vista previa.

El usuario completó un borrador Laboratory añadiendo Multimedia/Navegación/Lugares/Reloj,
revisó los **ocho widgets juntos** y confirmó que se ven bien; canceló sin guardar.
Acta `eight-widgets-observation.json`, **2026-10-20T22:08:19Z = 2026-10-21 00:08:19
Europe/Madrid**: **PHYSICALLY_VERIFIED de representación conjunta a fontScale1.0**.
VEHICLE, MEDIA, NAVIGATION, ASSISTANT, PLACES, OBD_STATUS, CLOCK y SYSTEM_STATUS.
Huella persistida idéntica `99f920b5…3c8de`; límites de seguridad sin modificar.
La vista previa no valida funciones de reproductores, Maps/Waze, Places o ELM327.

El usuario ajustó manualmente el texto al máximo disponible, revisó la distribución
actual/diálogo en vertical y horizontal y confirmó que se ve y se puede usar en ambas.
La sonda de las **2026-10-20T22:11:30Z = 2026-10-21 00:11:30 Europe/Madrid** mostró
fontScale1.0 y el layout conservado. La consulta explícita del usuario Android actual
a las 22:12:13Z también mostró1.0. Al aclararlo, el usuario confirmó que ya había
devuelto el texto al tamaño original antes de contestar.

Acta `font-orientation-human-clarification.json`: **PHYSICALLY_VERIFIED de la revisión
manual con máximo seleccionado por el usuario y ambas orientaciones**. Factor numérico
máximo **no capturado**; no se declara una medición física de1.5× o2×. La primera sonda
BLOCKED se conserva como falta de métrica en ese momento, no fallo de renderizado.
Restauración de fuente **confirmada a1.0** por humano y lectura. Sin `settings put`,
`wm` de escritura ni cambios globales automáticos. No se prueba el producto cartesiano
de todos los presets/widgets/fuentes/orientaciones.

Se solicitó TalkBack manual: activar desde Ajustes, recorrer foco/nombres/estado del
micrófono, abrir y cerrar respuesta, volver a Ajustes y desactivarlo. El usuario informa
que no lo tiene disponible/localizado y decide omitir la prueba. **TalkBack manual
PENDING**, no FAILED ni UNSUPPORTED. La detección previa de paquete no demuestra una
entrada de Ajustes disponible ni funcionamiento del lector. No se insiste, instala ni
activa automáticamente. Gestos y salida se habían preparado con las guías oficiales
de Google de [gestos TalkBack](https://support.google.com/accessibility/android/answer/6151827)
y [desactivación](https://support.google.com/accessibility/android/answer/14252950).

### Verificación final y conservación

Última verificación de dispositivo a las **2026-10-20T22:17:59Z = 2026-10-21 00:17:59
Europe/Madrid**: APK local e instalado idénticos al SHA-256 original, HONOR ABR-NX1/
Android16/API36. fontScale1.0, autorrotación1, user_rotation0,1080×2412/480dpi y
TalkBack desactivado coinciden con los valores originales medidos. La distribución
guardada permanece en la referencia elegida manualmente por el usuario `99f920b5…3c8de`.
No se restauró el layout anterior `60e75bde…780f8`: el cambio se debió a Guardar manual
y se respetó esa elección. Los ensayos posteriores con Cancelar conservaron la nueva referencia.

Micrófono conserva el permiso otorgado manualmente; contactos, ubicación aproximada y
Bluetooth Connect siguen no concedidos entre los permisos consultados. No se revocan
permisos, borran preferencias/datos ni desinstala la app. Consulta AppOps específica
de RECORD_AUDIO a las22:18:46Z sin marca running observada; no se exporta el registro
completo ni se presenta esa instantánea como garantía fuera de su ventana.

No se cambió configuración global mediante ADB, conectó OBD, activó IA externa o
instaló instrumentación/herramientas adicionales. Solo lectura de metadatos y gestos
de navegación autorizados; toda escucha/reproducción fue iniciada por el usuario.

### Corrección mínima del preflight

Escenario reproducido en HONOR/API36 sin app instalada: `pm path ai.copilotdrive.app`
retorna exit 1 sin stdout/stderr. El runner original lo interpretó como COMMAND_FAILED
y bloqueó antes de distinguir ausencia de fallo del gestor de paquetes. El intento
original se conserva en `phone-preflight-initial-blocked.json`.

`scripts/pilot_package.py` ahora determina ausencia mediante consulta de paquetes
exitosa antes de pedir la ruta. Si falla la consulta, su formato es inesperado o
el paquete existe pero no puede obtenerse el APK, conserva el bloqueo. No captura
genéricamente errores para asumir ausencia, ni relaja comparación de certificados.

Se añadieron primero cuatro regresiones en `tests/test_pilot_package.py` (dos fallaban
con el código original). Tras corregir: suite Python completa **56/56 PASS**, seguida
de preflight pasivo **PASS en el teléfono**. Rama específica preserva todos los commits
Sprint10. Durante las ejecuciones workingTreeDirty=true reflejaba runner, tests y
documentación; la corrección y sus cuatro regresiones se guardaron después en `94aa2be`.
No se recompila APK: la corrección afecta solo a la herramienta anfitriona Python,
ningún archivo Android cambió. El APK conserva procedencia, firma y SHA-256 originales.

## K. Evidencias verificables

Evidencias locales ignoradas por Git, sin seriales físicos, audio, transcripciones,
contactos, capturas ni logs generales del teléfono:

| Evidencia / método | Fecha UTC | Observación | Limitación |
| --- | --- | --- | --- |
| `artifacts/sprint10-1/discovery-initial.json`; runner pasivo y `adb devices -l` | 2026-10-20T20:41:39Z | Un destino emulado autorizado, ningún teléfono | Inventario de esa consulta; no prueba acústica |
| `artifacts/sprint10-1/discovery-final.json`; runner pasivo | 2026-10-20T20:43:13Z | Sigue un único destino emulado; ningún teléfono | workingTreeDirty=true por los dos documentos de esta campaña |
| `artifacts/sprint10-1/apk-preflight.json`; `pilot_package.py` | Misma ejecución local, antes de modificar documentación | Hash/firma/paquete/SDK/permisos conformes; activeActionsExecuted=false | Sin destino: firma instalada y compatibilidad física no comprobadas |
| `artifacts/sprint10-1/usb-discovery.json`; Get-PnpDevice | 2026-10-20T20:42:05Z | Cero candidatos Android/ADB/MTP/WPD y cero problemas USB presentes | No inspección manual de cable ni diagnóstico definitivo de drivers ausentes |
| `artifacts/sprint10-1/usb-followup.json`; PnP y metadatos de drivers relacionados | 2026-10-20T20:54:10Z | HONOR 400 Lite / ABR-NX1 por MTP, controladores Microsoft firmados | Sin consulta Android ni prueba funcional de ADB |
| `artifacts/sprint10-1/discovery-followup.json`; runner pasivo | 2026-10-20T20:54:10Z | Solo emulador autorizado; teléfono ausente de ADB | Pendiente intervención humana sobre depuración/RSA |
| `artifacts/sprint10-1/phone-profile.json`; getprop, wm, settings y pm de solo lectura | 2026-10-20T20:55:35Z | HONOR ABR-NX1, Android16/API36, ARM64, pantalla/densidad y app ausente | No voz ni prueba funcional de la app |
| `artifacts/sprint10-1/phone-preflight-initial-blocked.json`; runner original | 2026-10-20T20:55:16Z | BLOCKED / COMMAND_FAILED con app ausente | Intento fallido conservado; error de herramienta, no incompatibilidad OEM |
| `artifacts/sprint10-1/phone-preflight.json`; runner corregido | 2026-10-20T20:56:35Z | PASS, minSdkCompatible=true, signatureConflict=false, sin acciones activas | APK no instalado; actividad y acústica pendientes de autorización |
| `artifacts/sprint10-1/python-tests.log`; unittest completo | 2026-10-20, antes del preflight final | 56 casos, cero fallos/errores | Pruebas anfitrión; no ejecución acústica ni nueva suite Android |
| `artifacts/sprint10-1/install-authorized-dry-run.json` y `install-authorized.json`; runner con destino físico explícito | 2026-10-20T21:02:05Z, fin | Instalación completada, hash instalado idéntico y MainActivity Status OK | Sin audibilidad ni funcionamiento STT verificados |
| `artifacts/sprint10-1/startup-ui.json` y `startup-triage.json`; sondas UI/ActivityManager acotadas | 2026-10-20T21:02:29Z / 21:02:57Z | Primera sonda sin app visible; después app reanudada y proceso presente, sin registros exit-info | No atribución de la transición ni garantía fuera de esa ventana |
| `artifacts/sprint10-1/onboarding-observation.json`; observación humana y acta de sesión | 2026-10-20, después de instalación | Usuario confirma bienvenida visible | Solo bienvenida; no dashboard ni voz |
| `artifacts/sprint10-1/voice-passive-services.json`; servicios/configuración de voz, solo lectura | 2026-10-20T21:03:32Z | Servicio Google TTS y servicios STT declarados | No acredita voz es-ES instalada ni STT local funcional |
| `artifacts/sprint10-1/voice-ui-passive.json`; estados UI con lista permitida | 2026-10-20T21:04:55Z | La app informa 6 voces locales es-ES disponibles | Disponibilidad pasiva; no reproducción ni audibilidad; solo nodos renderizados |
| `artifacts/sprint10-1/dashboard-observation.json`; observación humana ligada a sesión/build | 2026-10-20T21:21:09Z, recogida | Dashboard visible confirmado; usuario ejecutó comprobación de voz | No califica todos los widgets, accesibilidad ni audio |
| `artifacts/sprint10-1/lab-after-human-check.json`; lectura acotada UI | 2026-10-20T21:21:09Z | Estado acústico CANCELLED, humano PENDING | No demuestra finalización TTS ni explica la cancelación |
| `artifacts/sprint10-1/tts-acoustic-result.json` y `tts-callback-followup.json`; primeras sondas tras respuesta humana | 2026-10-20T21:22:23Z / 21:23:03Z | Usuario confirma audibilidad; panel fuera de vista, callback aún null | Ausencia de nodo visible no equivale a fallo |
| `artifacts/sprint10-1/tts-callback-panel.json` y `tts-observation.json`; callback UI y observación humana | 2026-10-20T21:23:36Z | COMPLETED + CONFIRMED; PHYSICALLY_VERIFIED de la frase guiada | Una frase/intento y build exactos; no STT ni Jarvis E2E |
| `artifacts/sprint10-1/jarvis-failed-stage.json`; permisos y UI acotada | 2026-10-20T21:29:00Z | Usuario informa fallo; RECORD_AUDIO concedido, app no visible en esa sonda | Sin atribución de causa ni exportación de otra app |
| `artifacts/sprint10-1/jarvis-failure-diagnostics.json` y `stt-attempt1-observation.json` | 2026-10-20T21:30:01Z | STT_TIMEOUT y etapas previas al fallo; intento1 FAILED | Sin transcripción ni éxito E2E, causa pendiente |
| `artifacts/sprint10-1/jarvis-attempt2-stages.json`; etapas permitidas | 2026-10-20T21:34:08Z | TRANSCRIPT_OBTAINED hasta TTS_COMPLETED, con herramienta autorizada | Sin contenido ni medición individual de componentes internos |
| `artifacts/sprint10-1/stt-attempt2-observation.json` y `jarvis-attempt2-observation.json`; observación humana y traza | 2026-10-20, tras intento2 | PHYSICALLY_VERIFIED; voz clara y simulación hablada confirmadas | Un reintento exitoso; timeout del intento1 no resuelto causalmente |
| `artifacts/sprint10-1/recovery-sequence-observation.json` y tres actas `recovery-*-observation.json`; observación humana | 2026-10-20T21:38:40Z, recogida | Cancela, retorna sin autoescucha y responde a nueva pregunta; hash instalado idéntico | No grabación continua, error de motor ni interrupción personal |
| `artifacts/sprint10-1/silence-recovery-observation.json`; observación humana | 2026-10-20T21:43:18Z, recogida | Escucha en silencio finaliza y siguiente pregunta responde; PHYSICALLY_VERIFIED | Sin duración precisa ni código de motor observados |
| `artifacts/sprint10-1/display-accessibility-passive.json`; nodos y presencia exacta de TalkBack | 2026-10-20T21:45:08Z | App visible 1080×2412; TalkBack instalado, no habilitado | No prueba manual de lector ni cambios globales; límites de etiquetas, no objetivos táctiles |
| `artifacts/sprint10-1/read-response-discoverability.json`; UI acotada | 2026-10-20T21:46:03Z | Entrada de lectura habilitada/pulsable; usuario inicialmente no la localiza | No se guarda contenido de respuesta ni se infiere recorte visual de la semántica |
| `artifacts/sprint10-1/read-response-observation.json` y `dashboard-visual-observation.json`; revisión humana | 2026-10-20T21:47:26Z, recogida | Lectura de respuesta y dashboard actual confirmados | No ocho widgets/presets ni fuentes grandes/rotación |
| `artifacts/sprint10-1/validation-checkpoint.json`; metadatos/permisos/hash | 2026-10-20T21:47:26Z | APK local e instalado idénticos, fontScale1.0, solo micrófono concedido entre permisos runtime consultados | Permiso no equivale a escucha activa |
| `artifacts/sprint10-1/display-baseline.json`; solo hash/configuración | 2026-10-20T21:49:28Z | Huella de distribución y valores de pantalla anteriores a campaña visual ampliada | Sin exportar preferencias ni cambiar ajustes globales |
| `artifacts/sprint10-1/presets-classic-driver-observation.json`, `layout-change-inspection.json` y `layout-change-incident.json` | 2026-10-20T21:55:56Z / 21:56:42Z | Vistas confirmadas; comprobación de huella falla, después MEDIA guardado | Preservación BLOCKED hasta aclarar Guardar; sin atribución a app ni restauración automática |
| `artifacts/sprint10-1/display-baseline-after-manual-save.json`; aclaración humana y huella de solo lectura | 2026-10-20T22:01:33Z | Usuario confirma Guardar; nuevo baseline de distribución actual, sin restauración | Cancela sin guardar aún pendiente de un ensayo nuevo; incidente original conservado |
| `artifacts/sprint10-1/presets-media-laboratory-observation.json`; revisión humana y hash tras cancelar | 2026-10-20T22:06:18Z | Media/Laboratory correctos y layout idéntico al nuevo baseline | Solo vista previa y conservación del ensayo sin Guardar; no efectos de proveedores |
| `artifacts/sprint10-1/eight-widgets-observation.json`; revisión humana y hash | 2026-10-20T22:08:19Z | Ocho widgets juntos correctos en vista previa, fontScale1.0; cancela y conserva layout | No interacción con proveedores ni matriz completa de fuentes/orientaciones |
| `artifacts/sprint10-1/large-font-orientation-observation.json` y `font-current-user-check.json`; sondas posteriores | 2026-10-20T22:11:30Z / 22:12:13Z | FontScale1.0 y layout conservado | Usuario ya había restaurado; no máximo medido. BLOCKED inicial no es fallo visual |
| `artifacts/sprint10-1/font-orientation-human-clarification.json`; aclaración y revisión humanas | 2026-10-20, después de las sondas (21/10 local) | Ambas orientaciones y máximo elegido se ven/usables; fuente original restaurada1.0 | Multiplicador del máximo no observado ni inventado |
| `artifacts/sprint10-1/talkback-deferral.json`; decisión humana | Cierre 21/10 local | TalkBack manual PENDING por decisión del usuario | Paquete detectado no implica disponibilidad práctica del lector |
| `artifacts/sprint10-1/final-device-verification.json` y `final-audio-appop.json`; comprobación final de solo lectura | 2026-10-20T22:17:59Z / 22:18:46Z | APK exacto, ajustes de pantalla/restauración y layout elegido conservados; sin running de audio observado | Permiso micrófono permanece; no garantía continua ni restauración del layout que el usuario cambió con Guardar |
| `artifacts/sprint10-1/qualification-summary.json`; normalización final por escenario | Cierre 21/10 local | Enums existentes, build/dispositivo/sesión/observación/acústica coherentes; pendientes explícitos | Invariantes estructurales comprobadas en Python; no nueva ejecución Kotlin ni suma de registros como tests automáticos |

Todas las comprobaciones corresponden al HEAD y SHA-256 indicados arriba.
Modelo: HONOR 400 Lite / ABR-NX1; Android16/API36 según ADB.
Métodos: automatizado pasivo/local, instalación/apertura autorizadas y observación
humana de bienvenida/dashboard/TTS/Jarvis. Confirmación humana de audibilidad de frase
guiada y respuesta Jarvis intento2: **Sí**, independiente de sus callbacks observados.

Verificación: `git diff --check` sin errores. Cambios limitados al runner Python,
cuatro regresiones, informe y sección Sprint 10.1 de la matriz. Evidencias en artifacts
ignoradas por Git. SHA-256 del APK comprobado de nuevo e idéntico al esperado.
Sin cambios Android, no se repitieron suites JVM/Android ni se ejecutó Gradle.
La normalización final conserva MANUAL_OBSERVATION/ANDROID_DEVICE para calificaciones
físicas con observación humana, y separa evidencia auxiliar ADB de ese método. No
convierte callbacks o controles renderizados en observaciones humanas. Los registros
de escenarios no se cuentan como nuevas pruebas automatizadas.
Auditoría local final en `artifacts/sprint10-1/final-evidence-audit.json`: JSON válidos,
invariantes de sesión/build/observación coherentes, referencias de evidencia existentes,
sin serial físico exportado y comprobaciones finales de APK/conservación conformes.

## L. OBD, proveedores e integraciones

OBD: **PENDING**. Disponibilidad de adaptador Bluetooth Classic, vehículo detenido y
autorización específica no confirmadas. No escaneo, conexión ni comandos ejecutados.

Multimedia/MediaSession, Maps/Waze, contactos ficticios/ACTION_DIAL, IA externa y
Places: **PENDING**. Sin apertura de apps externas, acceso a contactos, llamadas ni
peticiones a proveedores. Su autorización se solicita por separado cuando proceda.
Abrir un intent no acreditará una ruta activa ni efecto en un reproductor.

Rendimiento físico: **PENDING**, sin autorización independiente ni mediciones.
Sin benchmarks, herramientas adicionales o cambios de energía/configuración global.

Propuestas manuales posteriores, con autorización independiente antes de actuar:

| Fase | Preparación y observación requerida |
| --- | --- |
| Multimedia/MediaSession | Reproductor y contenido de prueba elegidos por el usuario; acceso a notificaciones manual. Verificar play/pausa/siguiente en el reproductor y su callback, no solo envío del comando |
| Maps/Waze | Destino ficticio y apertura autorizada, retorno a CopilotDrive. Registrar apertura observada; no afirmar ruta activa ni ETA |
| Contactos/ACTION_DIAL | Contacto ficticio creado por el usuario, permiso solo para el escenario, desambiguación y confirmación nueva. Abrir marcador sin iniciar llamada |
| OBD-II | Confirmar adaptador Bluetooth Classic compatible, vehículo detenido y autorización. Solo PIDs permitidos, sin VIN/CAN libre/escrituras/borrado de DTC; NO DATA, desconexión y retorno al simulador |
| IA/Places | Proyecto/credenciales de prueba y consentimiento de cuota/coste antes de solicitudes. No necesarios para el recorrido local validado |
| Rendimiento | Consentimiento específico, APK y condiciones equivalentes documentados, primera medición sin ajustes globales. Sin Gradle/benchmarks simultáneos ni extrapolación del emulador |

Fallo de motor inducido e interrupción externa de audio: PENDING, sin deshabilitar
servicios, generar llamadas ni provocar interrupciones personales para simularlos.
La cancelación guiada ya validada se inició durante PTT; no sustituye por sí sola
a una campaña separada de pérdidas de foco durante salida TTS.

## M. Cierre y recomendaciones para el siguiente incremento

Objetivo principal cumplido en este teléfono/APK: Jarvis escucha, procesa localmente
y responde por voz anunciando simulación, con confirmación acústica humana. También
se validaron cancelación/retorno sin autoescucha, silencio/recuperación, dashboard,
diálogo, cuatro presets y ocho widgets en vista previa, más revisión manual de fuente
máxima y ambas orientaciones. La calificación es por escenario, no una aprobación
universal del dispositivo o de toda la familia OEM.

El primer intento STT_TIMEOUT continúa FAILED. El reintento funcionó, pero no se
demuestra su causa ni que nunca vuelva a ocurrir. No se amplió el timeout ni cambió
código Android sin un defecto causal reproducido. La única corrección es del runner
anfitrión para paquete ausente, con cuatro regresiones y suite Python56/56 PASS.

Siguientes pasos propuestos, sin empezar otro sprint funcional:

1. Reproducir el primer arranque STT con inicio manual y condiciones documentadas,
   distinguiendo solicitud de escucha, onReady, idioma/modelos y finalización, sin
   registrar audio/transcripciones ni atribuir el fallo a calentamiento o red sin pruebas.
2. Si se necesita una matriz numérica de fuente, capturar el factor antes de restaurarlo;
   esta sesión no acredita1.5×/2× medidos. Mantener la observación de descubrimiento
   de «Leer respuesta» para revisión de usabilidad, sin llamarla un recorte reproducido.
3. TalkBack solo si el usuario quiere retomarlo y dispone de entrada/motor accesible;
   activación y restauración manuales. Hoy queda PENDING por decisión explícita.
4. Preparar error de motor/pérdida de foco TTS de forma controlada y consentida, sin
   deshabilitar servicios, borrar datos o generar llamadas/interrupciones personales.
5. Tratar OBD, multimedia/Maps/Waze/contactos, proveedores y rendimiento como fases
   independientes de la sección L, con sus requisitos de hardware/coste/consentimiento.

Cambios en `feat/sprint10-1-physical-validation`, basada en Sprint10 completo;
corrección `94aa2be` y documentación del piloto. Sin cambios a main, push, merge,
publicación ni APK nuevo. Evidencias locales ignoradas y redactadas; cierre no elimina
datos ni revoca preferencias/permisos elegidos por el usuario.

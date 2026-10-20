# Sprint 10.1 — descubrimiento físico

2026-10-20 a 2026-10-21, Europe/Madrid; base `de7c4c8`, APK original SHA-256
`fcec5256a0c0911eefe22ca2492db45a9374bcb45ace81ba9269ae664b7e643c`.
Descubrimiento ADB pasivo: solo un emulador, confirmado por `ro.kernel.qemu=1`;
ningún teléfono en ADB. Consulta PnP inicial sin candidatos Android/ADB/MTP/WPD ni
errores USB presentes. No descarta cable inadecuado, depuración desactivada o driver ausente.
Seguimiento a las 22:54 Europe/Madrid: Windows sí detecta HONOR 400 Lite / ABR-NX1
por MTP, con controlador Microsoft firmado y sin errores PnP USB presentes.
Tras activar Depuración USB y aceptar RSA, ADB detecta HONOR ABR-NX1 autorizado:
Android16/API36, ARM64,1080×2412/480dpi/fontScale1.0. Preflight del teléfono **PASS**
a las 22:56:35 Europe/Madrid: firma v2, paquete/permisos/SDK, API/ABI compatibles,
app ausente y sin conflicto de firma. Runner corregido para `pm path` exit1 cuando
no existe la app; cuatro regresiones y suite Python56/56 PASS. APK original intacto.
El usuario autorizó instalación/apertura y voz/recuperación locales. Instalación `-r`
verificada a las 23:02:05 Europe/Madrid: APK instalado con SHA-256 idéntico y apertura
MainActivity Status OK. Usuario confirma bienvenida y dashboard visibles y comprobación
pasiva de voz ejecutada. La acústica requiere observación independiente.
Sin permisos runtime concedidos por script.

| Escenario Sprint 10.1 | Estado | Evidencia / limitación |
| --- | --- | --- |
| Instalación/apertura, identidad del APK | PHYSICALLY_VERIFIED | Autorizadas; hash instalado idéntico y MainActivity Status OK, más confirmación humana de bienvenida/dashboard |
| Bienvenida visible | PHYSICALLY_VERIFIED | Usuario confirma pantalla inicial; onboarding-observation.json ligado a sesión/build |
| Dashboard visible | PHYSICALLY_VERIFIED | Usuario confirma visibilidad; dashboard-observation.json; no califica todos los widgets ni accesibilidad |
| TTS guiado es-ES y audibilidad de esa frase | PHYSICALLY_VERIFIED | Pulsación humana, COMPLETED + CONFIRMED en panel y confirmación humana separada; tts-observation.json, intento1, 23:23:36 Europe/Madrid |
| STT es-ES y Jarvis completo, intento1 | FAILED | Usuario inicia PTT y concede micrófono; STT_TIMEOUT, sin transcripción ni etapas posteriores; causa pendiente, reintento2 exitoso registrado aparte |
| STT es-ES y Jarvis completo, intento2 | PHYSICALLY_VERIFIED | Reintento manual; TRANSCRIPT_OBTAINED → orquestador/herramienta/texto → TTS_COMPLETED; usuario confirma voz clara y simulación hablada; actas intento2 |
| Catálogo TTS local es-ES | PENDING para las demás voces | La app muestra 6 voces locales; frase guiada verificada, sin calificar individualmente las otras voces |
| Cancelación, background/retorno sin autoescucha y nueva pregunta | PHYSICALLY_VERIFIED | Usuario confirma la secuencia; tres actas y recovery-sequence-observation.json, 23:38:40 Europe/Madrid; hash instalado idéntico |
| Silencio controlado y recuperación posterior | PHYSICALLY_VERIFIED | Usuario confirma fin de escucha y siguiente pregunta exitosa; acta 23:43:18 Europe/Madrid. Sin duración precisa ni código de motor, distinto del timeout inicial |
| Dashboard actual y diálogo de lectura | PHYSICALLY_VERIFIED | Usuario confirma consulta de estado, lectura y revisión visual/táctil; fontScale1.0. Dificultad inicial para descubrir la entrada conservada |
| Vistas Classic/Driver | PHYSICALLY_VERIFIED de visualización | Usuario las confirma; después aclara que pulsó Guardar. No califica conservación al cancelar |
| Vistas Media/Laboratory | PHYSICALLY_VERIFIED de visualización | Usuario confirma vistas y cancelación sin Guardar; 21/10 00:06:18 Europe/Madrid |
| Preservación al cancelar editor, ensayo nuevo | PHYSICALLY_VERIFIED | Hash guardado idéntico `99f920b5…3c8de` tras Media/Laboratory. Primer cambio aclarado por Guardar manual; historial original conservado |
| Ocho widgets juntos, vista previa | PHYSICALLY_VERIFIED | Usuario confirma las ocho tarjetas y cancela; fontScale1.0, hash idéntico `99f920b5…3c8de`, 21/10 00:08:19 Europe/Madrid. Sin efectos de proveedores |
| Fuente máxima elegida y orientación | PHYSICALLY_VERIFIED de revisión manual | Usuario confirma vertical/horizontal usables y que restauró antes de la sonda. Factor máximo no capturado; fuente original1.0 y layout conservado, sin declarar1.5×/2× medidos |
| TalkBack manual | PENDING | Usuario informa que no lo tiene disponible/localizado y decide omitirlo. Paquete detectado no implica entrada utilizable; lector final desactivado, sin cambios por ADB |
| OBD/vehículo, rendimiento e integraciones/proveedores | PENDING | Sin hardware/autorización específica confirmados |

Evidencia y acciones propuestas en [SPRINT10_PHYSICAL_PILOT_REPORT.md](SPRINT10_PHYSICAL_PILOT_REPORT.md).
Bienvenida, dashboard visible, frase TTS guiada y Jarvis/STT intento2 se marcan
PHYSICALLY_VERIFIED, junto a la secuencia de recuperación manual confirmada.
STT/Jarvis intento1 permanece FAILED por timeout; silencio controlado y recuperación
posterior confirmados. Cuatro presets, ocho widgets y revisión manual de fuente máxima
y ambas orientaciones confirmados; multiplicador máximo no medido. Error de motor,
interrupción externa de audio, TalkBack y fases independientes permanecen PENDING.
Sin extensión a toda la familia OEM.
La evidencia histórica se conserva.

Comprobación auxiliar final ADB (21/10 00:17:59 Europe/Madrid): fuente1.0,
autorrotación1, rotación0,1080×2412/480dpi y TalkBack apagado coinciden con el baseline.
Layout elegido con Guardar manual conservado `99f920b5…3c8de` y APK original idéntico.
Las calificaciones manuales corresponden a los escenarios de la tabla; esta comprobación
aporta evidencia auxiliar de conservación. Resumen normalizado local:
`artifacts/sprint10-1/qualification-summary.json`.

# Sprint10 — matriz de evidencia física

Descubrimiento pasivo inicial/final registrado en SPRINT10_REPORT: ningún teléfono
autorizado; emulador únicamente. No instalación, voz, OBD ni accesibilidad física.
Todas las filas físicas de abajo conservan PENDING. No se detectó ni calificó un
ELM327/coche desde Windows; su disponibilidad física no se inventa.

| Escenario Sprint10 | Nivel/estado | Evidencia |
| --- | --- | --- |
| Confirmación acústica y vínculo sesión/build | AUTOMATED_TESTED | PhysicalPilotTest y PhysicalVoicePilotTest |
| Controles y permisos empaquetados | EMULATOR_TESTED cuando se ejecutan | PhysicalPilotUiTest; tabla final en informe |
| A: proveedor fake sin red | AUTOMATED_TESTED | consulta local, cero llamadas AI |
| B: Android26 sin conectividad | EMULATOR_TESTED para intento aprobado | ConnectivityService + flags antes/después + consulta local; informe API26 |
| Acústica humana, STT/TTS es-ES físico | PENDING | Sin observador/dispositivo |
| OBD, vehículo, rendimiento físico, interoperabilidad y TalkBack | PENDING | Sin hardware/autorización/observación |

Acta nueva: specs/017-physical-pilot/pilot-record-template.json. Procedimiento:
PHYSICAL_PILOT_GUIDE.md y VOICE_QUALIFICATION.md. No copiar resultados del emulador
a una fila PHYSICALLY_VERIFIED.

## Matriz histórica, todos los pendientes físicos se conservan

# Matriz de validación física — actualizada en Sprint 9

Estado de todas las integraciones físicas: `PENDING`. Sprint 9 repitió descubrimiento ADB pasivo:
solo emulador Android 15/API35, modelo Android SDK built for x86_64. No teléfono,
adaptador o coche conectado/autorizado. `IMPLEMENTED` y `AUTOMATED_TESTED` describen
software; solo observación manual documentada puede marcar `PHYSICALLY_VERIFIED`.
Evidencia de emulador, commit y SHA-256 en SPRINT9_REPORT; no se promueve esta matriz.
Recorridos preparados por integración y requisitos de consentimiento en
PHYSICAL_TEST_PROCEDURES.md. API26/30/35 ejecutaron34 pruebas por versión en AVDs
oficiales independientes; API26 offline permanece bloqueado por el comando Wi-Fi
del sistema. Las observaciones humanas y toda verificación física siguen PENDING.

| Integración | Intento y requisitos | Evidencia actual | Resultado observado | Pendiente |
|---|---|---|---|---|
| Voz Android es-ES | Teléfono, micrófono, voz local instalada; STT push to talk y frase TTS | Fakes/contratos automatizados históricos | Sin ejecución física | Captura, silencio, cancelación, llamada/interrupción y calidad audible |
| ELM327 Classic | Adaptador emparejado y alimentado; vehículo detenido | Fake serial automatizado | Sin ejecución física | RFCOMM, init, PIDs, lecturas, NO DATA, timeout, revocación, desconexión y reconexión |
| Vehículo real detenido | Banco controlado, conductor fuera de marcha | Ninguna | Sin ejecución física | Comparar lecturas permitidas, fuente y retorno al simulador |
| Chat Completions real | Endpoint HTTPS, token de prueba, consentimiento, cuota | Fake HTTP/loopback histórico | Sin ejecución externa | Texto, modelo, JSON, tool calling opcional, 401/429/5xx y latencia |
| MediaSession | Reproductor instalado y autorizado | Emulador histórico | Sin ejecución física | Selección, pausa y callback del reproductor |
| Maps/Waze | Apps instaladas, vehículo detenido | Intents/unit tests históricos | Sin ejecución física | Resolver y abrir destino sin afirmar ruta iniciada |
| Contactos de laboratorio | Contacto ficticio, permiso, marcador | Fakes/unit tests históricos | Sin ejecución física | Desambiguación, confirmación nueva y ACTION_DIAL |
| Pantalla, insets y fuente OEM | Teléfono/pantalla con APK exacto; consentimiento antes de instalar | Matriz de emulador Sprint 7 y tests Compose | PENDING físico | Controles esenciales, rotación, fuentes 1.5×/2×, editor, driver/laboratory |
| TalkBack manual | Lector instalado y activado por usuario autorizado | Semántica Compose Sprint 7; TalkBack ausente en emulador | PENDING físico | Foco, headings, nombres, deshabilitados, diálogos y respuesta larga |
| Google Places / GPS | Clave restringida, cuota y consentimiento separado de coste | Fakes/demo; carrera de revocación corregida | PENDING externo/físico | Ubicación coarse, revocación, categorías, errores y atribuciones reales |

Para registrar una ejecución, copia una fila en un acta con: fecha, commit, APK SHA-256,
Android/API y modelo, precondiciones, pasos, método, observación humana, nivel de
evidencia, error/duración y lo no verificado. Identifica adaptador/firmware sin MAC;
vehículo anónimo sin VIN/matrícula. Usa `FAILED` si se intentó y falló, `UNSUPPORTED`
si falta capacidad, `BLOCKED` por requisito externo y `PENDING` sin ejecución.
No convertir una fila PENDING en verificada por detectar hardware o recibir un callback.
Ver DEVICE_QUALIFICATION para invariantes de contratos y confirmaciones antes de actividad.

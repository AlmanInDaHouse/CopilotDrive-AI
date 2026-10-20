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

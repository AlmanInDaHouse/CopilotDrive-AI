# Recorridos físicos preparados — Sprint 8

Estado inicial: PENDING. El descubrimiento pasivo de esta sesión solo encontró un
emulador API35. Ninguna fila siguiente acredita una ejecución física o comercial.
Antes de instalar: presentar modelo/API, SHA-256 del APK, permisos solicitados y
escenarios concretos; obtener consentimiento humano explícito. No reutilizar la
autorización del emulador como autorización para un teléfono. Revisar el dry-run de
qualify_android y usar sus flags independientes únicamente tras ese consentimiento.

Acta mínima por integración: fecha UTC, commit, SHA-256, API/modelo, escenario,
precondiciones, método, observación/código de error, duración monotónica, estado de
restauración y confirmación humana cuando corresponda. Sin serial/MAC completa,
VIN/matrícula, contactos, audio, texto privado, token o coordenadas precisas.

| Integración | Recorrido y criterios observables | Consentimiento/requisitos específicos |
| --- | --- | --- |
| Voz | Micrófono/permiso; STT local es-ES; consulta local → Jarvis → ToolRegistry → texto → TTS es-ES; cancelar en STT y TTS, interrumpir audio focus, volver de segundo plano, recuperar tras fallo y comprobar texto; repetir con ruido ambiental controlado | Voz local instalada; fallback STT del sistema solo con consentimiento separado; usuario confirma sonido audible. Callback TTS no acredita acústica |
| OBD solo lectura | Emparejamiento Classic, permiso por API, selección e inicio manual RFCOMM, init ELM327, capacidades, PIDs permitidos/latencia/NO DATA, desconexión, reconexión limitada, cierre de socket y retorno al simulador | Teléfono+ELM327 compatible+coche detenido+autorización. Solo enum ObdWireRequest. Sin VIN, CAN libre, limpieza de averías ni inicio automático |
| IA externa | HTTPS y credencial de prueba; texto, HTTP401/429/5xx, timeout, latencia, cancelar, propuesta de tool autorizada, revocar consentimiento durante petición; verificar que cuerpo solo contiene texto/historial permitido | Endpoint, entorno, cuota/coste y consentimiento explícitos. Nunca credenciales comerciales en CI ni cuerpos de petición en logs |
| Places/GPS | Clave Android restringida por paquete/certificado, permiso coarse al buscar, categoría, resultados/atribuciones, denegar y revocar, límites/errores, selección caducable y Maps/Waze | Proyecto de pruebas, cuota/coste y consentimiento de ubicación/proveedor independientes; sin exportar búsquedas ni coordenadas |
| MediaSession | Autorizar notificaciones, seleccionar reproductor instalado, reproducción/pausa/siguiente, observar callback, perder sesión/permiso y volver a demo | Reproductor y contenido de prueba autorizados; estado solicitado no equivale a reproducción confirmada; no exportar títulos/carátulas |
| Maps/Waze | Destino ficticio, resolver intent, abrir y volver al dashboard | Apps instaladas, entorno detenido, autorización; intent abierto no significa ruta iniciada ni ETA |
| Contactos | Contacto ficticio preparado por el usuario, desambiguación, confirmación nueva y ACTION_DIAL | No leer contactos personales para crear fixtures; nunca iniciar llamada |
| TalkBack/layout | Foco, headings, nombres, estado deshabilitado, diálogo/respuesta larga, PTT/Ajustes/fuente/insets, rotación y editor/presets | Usuario habilita TalkBack; no modificar ajustes físicos sin consentimiento; restaurar cambios autorizados |

Si falta hardware o permiso: PENDING; si un requisito impide una prueba solicitada:
BLOCKED; si se intentó y falla: FAILED. Registrar capacidades ausentes como UNSUPPORTED
sin generalizar al modelo Android. Una observación solo califica ese escenario y APK.

API26: probar permisos clásicos Bluetooth y ausencia de STT on-device moderno.
API30: comprobar MediaSession y permisos/lifecycle sin BLUETOOTH_CONNECT obligatorio.
API35: comprobar dispositivos cercanos, coarse, audio focus, insets y recuperación.
No instalar imágenes SDK26/30 globalmente como parte de un runner. Preparar AVDs
oficiales independientes tras acordar descarga/espacio; nunca borrar AVDs existentes.

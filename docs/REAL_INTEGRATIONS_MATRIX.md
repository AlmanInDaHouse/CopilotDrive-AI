# Integraciones — Sprint 11

| Integración | REAL implementado | Requisito / límite | Evidencia |
|---|---|---|---|
| OBD | RFCOMM ELM327, selección/confirmación, inicialización y descubrimiento mode 01; desconexión/cierre y errores. | Adaptador emparejado, permiso Nearby en API31+, Bluetooth encendido, vehículo compatible. Sin auto conexión. Sin garantía Juke/PIDs. | Protocolos y fallos en JVM/fakes; hardware pendiente. |
| STT | SpeechRecognizer Android, preferencia local y fallback sistema opt-in, PTT. | Motor/idioma y permiso de micrófono. Fallback puede usar red. | Sprint10.1 tuvo recorrido acústico en HONOR con APK anterior; APK Sprint11 requiere nueva prueba física. |
| TTS | Voces instaladas es-ES, foco, cancelación y respuesta escrita. | Motor/datos instalados; Pico API26 tiene limitación histórica. | Callback/máquina de estados automatizados; audibilidad Sprint11 pendiente. |
| Jarvis local | Herramientas allowlisted, sin clave. Combustible ausente/obsoleto/no compatible explícitos. | Datos reales disponibles y permisos por herramienta. | JVM + flujo textual en emulador. |
| Multimedia | Sesión Android seleccionada, metadatos y controles soportados; vacío sin sesión. | Consentimiento de acceso a sesiones mediante notification listener. No lectura del contenido de notificaciones. | MediaSession fixture solo en instrumentación; reproductor físico pendiente. |
| Maps/Waze | Disponibilidad validada, destino real vía intent. | App instalada, confirmación/contexto seguro cuando corresponde. Sin ETA interna ni afirmación de ruta iniciada. | URI/política en tests; efecto físico pendiente. |
| Google Places | SDK actual, ubicación aproximada y consentimiento independiente por sesión, búsqueda solo manual. | Clave restringida en build y proyecto/cuota; sin clave NOT_CONFIGURED. No solicitudes comerciales en CI. | Controlador probado con fakes; proveedor facturable no ejecutado en esta campaña. |
| IA externa | Endpoint/modelo configurables, HTTPS, credencial en Android Keystore, opt-in y prueba manual. | Proveedor real y cuota. Sin clave funcionan las herramientas locales. Contexto vehicular externo deshabilitado. | Contratos/HTTP y seguridad automatizados; credenciales reales no proporcionadas. |
| Contactos | ContentResolver tras permiso y ACTION_DIAL con confirmación fresca. | Permiso READ_CONTACTS y app marcador. Nunca llamada automática. | Fakes/política; agenda/teléfono físicos pendientes. |
| Hora / sistema | Hora del dispositivo, capacidades disponibles y diagnóstico con evidencia independiente. | Una capacidad AVAILABLE no acredita prueba física. | Emulador / tests; no promoción a PHYSICALLY_VERIFIED. |

No se han añadido servicios, dependencias comerciales, telemetría remota ni claves. El APK debug está preparado para configuración por el usuario; la ausencia de integración nunca se presenta como éxito simulado en REAL.

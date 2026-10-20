# Sprint 4 — validación reproducible

El Laboratorio del dispositivo, accesible desde Ajustes Compose, muestra comprobaciones pasivas y un historial temporal. Cada acción activa exige pulsación específica; la prueba IA avisa del posible consumo de cuota y la prueba acústica pide confirmar la escucha humana. La conexión OBD conserva el diálogo de vehículo detenido.

Las comprobaciones de disponibilidad solo indican presencia, permisos o configuración. `PASS` no significa compatibilidad física: el nivel de evidencia se registra por separado. Un fake, unit test o emulador jamás asciende una integración a `ANDROID_DEVICE`, `OBD_ADAPTER` o `PARKED_VEHICLE`. Si falta hardware, se entrega APK y matriz `PENDING`.

OBD usa únicamente RFCOMM Classic y las solicitudes cerradas del Sprint 3: ATZ/ATE0/ATL0/ATS1/ATSP0, 0100/0120, 010C/010D/0105/012F. Prohibidos ECU write, reset de averías, consola ELM, CAN arbitrario y comandos del usuario o modelo. No se inicia acceso al vehículo en segundo plano. Tiempo de duración monotónico; fecha de observación de pared.

STT sigue siendo push to talk: preferencia local, fallback del sistema con consentimiento, límite de tiempo y cancelación sin bucle. TTS solo usa voces locales es-ES. Un callback técnico no acredita calidad audible. La respuesta escrita permanece cuando falla voz. La IA externa requiere consentimiento de texto, endpoint remoto HTTPS y credencial; la prueba opcional usa una frase fija y no envía telemetría, contactos ni ubicación.

La voz mide por separado inicialización STT, reconocimiento, respuesta del orquestador, inicialización TTS y tiempo hasta callback TTS. Cada etapa muestra número de muestras; p50/p95 solo desde 20 observaciones de esa etapa. Un tiempo técnico no acredita calidad acústica.

El contrato de contexto vehicular futuro exige fuente, timestamp, TTL, calidad, categoría, consentimiento específico, límite de bytes y revocación. No existe emisor activo de ese contrato en Sprint 4. El historial diagnóstico es temporal y acotado; el texto se redacta antes de mostrarse. No hay exportación automática ni persistencia.

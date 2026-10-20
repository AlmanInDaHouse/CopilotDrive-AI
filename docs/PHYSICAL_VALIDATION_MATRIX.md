# Matriz de validación física — Sprint 4

Estado inicial de todas las filas: `PENDING`. No hay teléfono, adaptador, coche ni cuenta aportados en esta sesión. `IMPLEMENTED` y `AUTOMATED_TESTED` describen software; solo una observación manual documentada puede marcar `PHYSICALLY_VERIFIED`.

| Integración | Intento y requisitos | Evidencia actual | Resultado observado | Pendiente |
|---|---|---|---|---|
| Voz Android es-ES | Teléfono, micrófono, voz local instalada; STT push to talk y frase TTS | Fakes/contratos automatizados históricos | Sin ejecución física | Captura, silencio, cancelación, llamada/interrupción y calidad audible |
| ELM327 Classic | Adaptador emparejado y alimentado; vehículo detenido | Fake serial automatizado | Sin ejecución física | RFCOMM, init, PIDs, lecturas, NO DATA, timeout, revocación, desconexión y reconexión |
| Vehículo real detenido | Banco controlado, conductor fuera de marcha | Ninguna | Sin ejecución física | Comparar lecturas permitidas, fuente y retorno al simulador |
| Chat Completions real | Endpoint HTTPS, token de prueba, consentimiento, cuota | Fake HTTP/loopback histórico | Sin ejecución externa | Texto, modelo, JSON, tool calling opcional, 401/429/5xx y latencia |
| MediaSession | Reproductor instalado y autorizado | Emulador histórico | Sin ejecución física | Selección, pausa y callback del reproductor |
| Maps/Waze | Apps instaladas, vehículo detenido | Intents/unit tests históricos | Sin ejecución física | Resolver y abrir destino sin afirmar ruta iniciada |
| Contactos de laboratorio | Contacto ficticio, permiso, marcador | Fakes/unit tests históricos | Sin ejecución física | Desambiguación, confirmación nueva y ACTION_DIAL |

Para registrar una ejecución, copia una fila en un acta con: fecha, commit, APK SHA-256, Android/API y modelo, precondiciones, pasos, observación, nivel de evidencia, error/duración y lo no verificado. Identifica adaptador y firmware sin MAC completa; vehículo de forma anónima sin VIN ni matrícula. Usa `FAIL` solo si se intentó y falló; `UNSUPPORTED` si el entorno carece de capacidad. No conviertas una fila `PENDING` en `PASS` por la existencia de hardware.

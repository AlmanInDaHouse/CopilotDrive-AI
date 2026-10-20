# Plan de pruebas (borrador)

- Preparar banco con vehículo inmóvil y adaptador emparejado. Capturar tiempos y respuestas normalizadas sin identificadores. Repetir en Android 8, 12 y 15 cuando haya dispositivos disponibles; registrar explícitamente niveles no probados.
- Probar PIDs soportados y ausentes, eco, `NO DATA`, respuesta parcial, pérdida de enlace, apagado de Bluetooth, revocación de permiso, cancelación, reconexión limitada y cambio al simulador.
- Probar STT/TTS es-ES con y sin voz instalada, pérdida de audio focus y fallo TTS; comprobar que queda texto. No activar escucha permanente.
- Probar proveedor local y remoto consentido con 200, 401, 429, timeout y TLS; inspeccionar solicitudes para excluir telemetría y secretos en logs.
- Mantener las pruebas fake/JVM, instrumentadas y Python como gate previo. Publicar evidencia física solo después de ejecutarla.

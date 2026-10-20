# Aceptación

1. Device Lab abre desde Ajustes, ejecuta pruebas pasivas sin hardware, muestra resultados tipados con fecha/duración y permite borrar historial.
2. Voz, Bluetooth, OBD e IA generan observaciones distinguibles sin afirmar verificación física ni calidad acústica por disponibilidad o callback.
3. La prueba de red y la acústica requieren acción explícita. El vehículo solo se conecta después del diálogo existente de entorno detenido.
4. El harness offline cubre fallos de permiso, transporte, parser, timeout, reconexión, simulador y redacción. CI no requiere token ni vehículo.
5. Builds y lint pasan; la APK debug y su SHA-256 se registran junto a los resultados ejecutados.
6. Las filas físicas no ejecutadas conservan `PENDING`, incluidas voz es-ES, ELM327 Classic, vehículo detenido, proveedor real, MediaSession, Maps/Waze y contactos de laboratorio. No bloquean la entrega de infraestructura.
7. No hay escritura vehicular, telemetría externa, escucha continua ni secretos en registros.

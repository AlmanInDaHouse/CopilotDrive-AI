# Conectar un adaptador OBD-II Bluetooth Classic

1. Con el vehículo detenido, inserta un adaptador ELM327 compatible en el puerto OBD-II. Empareja el adaptador desde Ajustes Bluetooth de Android. Esta versión no descubre dispositivos nuevos.
2. Abre CopilotDrive, entra en Ajustes y toca «Mostrar dispositivos emparejados». En Android 12–15 acepta «Dispositivos cercanos» para consultar y conectar; en Android 8–11 se usa el permiso Bluetooth heredado del manifest.
3. Selecciona el adaptador y pulsa «Conectar». Lee y acepta el aviso experimental. Espera a `CONNECTED_REAL` y comprueba los PIDs disponibles y la última lectura.
4. Usa «Desconectar» para cerrar la sesión o «Volver al simulador» para regresar a datos ficticios. La conexión se cierra cuando la app pasa a segundo plano.

El protocolo usa Bluetooth Classic SPP/RFCOMM, no BLE. Solo consulta RPM (010C), velocidad (010D), temperatura de refrigerante (0105) y nivel de combustible (012F) si el vehículo lo soporta. El porcentaje de combustible puede faltar. `NO DATA`, tramas incompletas y PIDs no anunciados dan «No disponible». No se calcula autonomía. La lectura OBD no es un diagnóstico certificado. No se solicita VIN, no se guardan identificadores del vehículo y la telemetría no sale del teléfono.

En emulador API 35 se comprobó el permiso Nearby devices y la indicación «No hay adaptadores emparejados». Esa prueba verifica la pantalla y el manejo sin adaptador; la compatibilidad RFCOMM real sigue pendiente de hardware.

## Validación física pendiente

Registrar modelo de teléfono y Android, fabricante y firmware del adaptador, vehículo, emparejamiento, permiso, conexión, latencia, PIDs anunciados, desconexión y reconexión. En el mismo dispositivo comprobar STT/TTS, audio focus, proveedor de voz real, consultas locales sin Internet, fallo de TTS con respuesta escrita y confirmación del marcador. No ejecutar estas pruebas automáticamente ni considerar los fakes como prueba de interoperabilidad física.

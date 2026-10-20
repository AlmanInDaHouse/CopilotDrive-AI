# Procedimiento del Laboratorio del dispositivo

1. Instala el APK debug cuyo SHA-256 figure en `SPRINT4_REPORT.md`. En un entorno detenido, completa onboarding, abre Ajustes y pulsa «Abrir laboratorio».
2. Ejecuta diagnóstico local. Registra Android/API, modelo, permisos, STT/TTS, configuración IA y fuente SIMULATED/REAL. `PASS` en disponibilidad no acredita funcionamiento.
3. Comprueba Bluetooth y emparejamiento sin conectar. Si hay adaptador, vuelve a Ajustes y usa la confirmación de conexión OBD solo con vehículo detenido. Observa RFCOMM, ELM, PIDs, lecturas y retorno al simulador. No envíes comandos fuera de la app.
4. Comprueba voz instalada. Para la prueba acústica acepta el diálogo y anota si se oyó la frase. Para STT usa el botón normal de micrófono, concede permiso y ensaya silencio, cancelación e interrupción. Verifica que el texto permanece ante fallo TTS.
5. Si deseas comprobar IA real, configura endpoint HTTPS, modelo, token y consentimiento de texto. Acepta el diálogo de cuota del laboratorio. Registra código de resultado y duración sin copiar token, prompt, respuesta personal ni trazas completas.
6. Ensaya por separado MediaSession, Maps/Waze y contactos ficticios. Registra lo solicitado y el callback observado; un intent abierto no demuestra que una ruta, llamada o reproducción haya empezado.
7. Completa `PHYSICAL_VALIDATION_MATRIX.md` para cada entorno probado, con APK SHA-256, requisitos, acción, resultado, evidencia y pendientes. Borra el historial de sesión al terminar. No incluyas VIN, MAC completa, audio, contactos ni conversaciones.

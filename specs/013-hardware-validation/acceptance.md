# Aceptación (borrador)

1. Al menos un adaptador ELM327 Classic y un vehículo detenidos completan conexión, descubrimiento, lectura, `NO DATA` si procede, desconexión y retorno al simulador; se registra el modelo exacto. Si no hay hardware, el criterio queda `NOT_RUN`.
2. Se documentan permisos revocados, Bluetooth apagado, timeout y reconexión en al menos un teléfono físico compatible.
3. Las pruebas de voz miden respuesta escrita ante fallo TTS y comportamiento de audio focus/STT con motor real, indicando idioma y motor instalado.
4. El proveedor LLM real se ensaya con consentimiento de texto, credencial de prueba y sin telemetría; los resultados de herramientas no aparecen en solicitudes remotas.
5. CI sigue sin requerir vehículo, adaptador, voz ni token.

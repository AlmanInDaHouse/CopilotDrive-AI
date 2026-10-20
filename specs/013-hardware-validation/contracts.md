# Contratos de evidencia (borrador)

Cada ensayo físico registra fecha, versión de app, nivel Android, modelo de teléfono, modelo/firmware de adaptador, vehículo anonimizado, PIDs anunciados, resultados, latencia, errores y pasos de reproducción. No registrar VIN, matrícula, dirección MAC completa, audio bruto, contactos ni token. Resultado `NOT_RUN` es distinto de `PASS` y `FAIL`. El consentimiento de telemetría externa permanece desactivado.

Las observaciones físicas se enlazan con una versión exacta del APK y su SHA-256. Los reportes de fake y emulador se mantienen separados de la matriz física.

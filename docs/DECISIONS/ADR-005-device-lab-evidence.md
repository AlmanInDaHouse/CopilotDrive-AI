# ADR-005 — Evidencia y Laboratorio del dispositivo

Estado: aceptado, Sprint 4.

El core JVM define `DiagnosticCheck`, `DiagnosticHistory` y niveles de evidencia; Android ejecuta sondas pasivas y presenta StateFlow en Compose. La duración usa reloj monotónico y el timestamp usa tiempo de pared. El historial no persiste y admite borrado manual.

El estado `PASS` significa que la comprobación concreta produjo el resultado esperado. El campo `evidence` indica dónde ocurrió. Las sondas de disponibilidad y los callbacks activos de esta versión conservan `PENDING` para verificación física; el acta manual es el lugar donde se promueve evidencia tras observar el dispositivo. Esto evita que CI o el emulador acrediten un coche real.

La prueba de red y la acústica exigen pulsación y aviso. La conexión OBD conserva su confirmación de vehículo detenido. No hay exportación en esta versión; toda descripción pasa por redacción defensiva de MAC, VIN y tokens. El historial está limitado a 100 observaciones.

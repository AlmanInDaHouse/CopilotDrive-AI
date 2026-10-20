# Sprint 8 priorizado — pruebas físicas consentidas

Sprint 6 se fusionó por PR #7 en main `1328077ff16d6487b4be995aae474dfc8452d345`.
Android y Tests del merge: success. Sprint 7 está en rama local
`feat/sprint7-device-qualification`; revisar SPRINT7_REPORT, TEST_RESULTS,
ACCESSIBILITY_AUDIT, PERFORMANCE_BASELINE y DEVICE_QUALIFICATION antes de publicar.
No se ha ejecutado push, merge ni release de Sprint 7.

1. **P0, teléfono autorizado:** descubrimiento pasivo, confirmación humana antes de
   instalar/hacer pruebas activas; API/modelo y APK exactos. TalkBack manual con
   fuente ampliada, permisos, respuesta larga y editor; voz es-ES audible con
   confirmación del usuario. Interrupción/retorno de audio focus y segundo plano.
2. **P0, ELM327 y coche detenido autorizados:** emparejamiento, RFCOMM, inicialización,
   PIDs, NO DATA, respuesta parcial, timeout, permiso revocado, reconexión y simulador.
   Comparar solo lecturas permitidas, sin VIN, MAC completa ni comandos libres.
3. **P1, rendimiento en teléfono:** repetir muestras bajo condiciones estables;
   preparar variante benchmark cercana a release y Macrobenchmark si se necesita
   TTFD/frame timing comparable. Medir recomposiciones, ciclos de navegación,
   memoria sostenida y energía antes de fijar presupuestos. No extrapolar emulador.
4. **P1, servicios opcionales:** configurar proyecto/cuota/consentimiento separado
   de costes para IA y Google Places. Probar HTTPS/401/429/5xx/cancelación/red y
   ubicación coarse/revocación. Fakes/loopback no prueban producción comercial.
5. **P1, interoperabilidad:** reproductores autorizados y callbacks MediaSession,
   GPS, Maps/Waze instalados y contactos ficticios. Un intent no acredita ruta/ETA.
6. **P2, recuperación OEM:** LMK real, permisos revocados por Android, varias APIs
   desde minSdk26 y densidades/insets físicos. Corregir solo fallos reproducidos.

Mantener UNKNOWN como desconocido, edición solo de laboratorio y contexto vehicular
excluido de IA. No añadir escucha permanente, servicio de micrófono ni tools de layout.

Publicar PR solo después de autorización explícita: revisar diff y commits contra
main, ejecutar suites/firma/hash, push de la rama, abrir PR a main con SPRINT7_REPORT
y esperar ambos workflows de la nueva rama. El CI del merge de Sprint 6 no es CI
de Sprint 7. Merge y release requieren decisiones independientes posteriores.

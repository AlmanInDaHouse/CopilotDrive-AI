# Sprint 10 recomendado — dispositivo real y atribución de rendimiento

Sprint 8 está fusionado por PR #9 en main `504e9a776177d78d70a085dcb7b4cacc914a069a`.
Android y Tests del merge terminaron success. Sprint 9 se entrega en la rama local
`feat/sprint9-jank-compatibility`, sin push, merge ni release. Consultar SPRINT9_REPORT,
JANK_INVESTIGATION, ANDROID_API_COMPATIBILITY y MEMORY_INVESTIGATION para resultados.

1. Teléfono autorizado: descubrimiento pasivo, presentar modelo/API, APK/hash y pasos;
   obtener consentimiento antes de instalar o ejecutar. STT es-ES, TTS audible,
   cancelación, audio focus, ruido y recuperación, con observación humana.
2. Perfetto en dispositivo estable: contrastar Main/RenderThread/FrameTimeline con el
   experimento de renderer del Sprint 9. Añadir trazas por composable y conteo dinámico
   solo cuando ayuden a atribuir una ruta. Corregir un cuello medido y repetir condiciones;
   no extrapolar SwiftShader ni presentar cambios del host como optimizaciones de app.
3. Memoria: seguir el ensayo sostenido y separar heap Java/nativo/gráficos, PSS,
   instrumentación, cachés y lifecycle. Heap/dominadores sobre datos sintéticos si hay
   retención reproducible; un delta PSS no demuestra una fuga.
4. API26/30/35: mantener las 34 pruebas por versión ya ejecutadas. Resolver el bloqueo
   offline de la imagen API26 (`svc wifi` termina 137); preservar el estado y los intentos.
   El permiso multimedia en 26 ya se lee sin la caché obsoleta. No declarar todos los
   fabricantes/versiones intermedias certificados por tres AVDs.
5. ELM327 y vehículo detenido: consentimiento específico, selección manual, RFCOMM,
   init y PIDs allowlisted, NO DATA, timeout, revocación, desconexión y vuelta a simulador.
   Sin VIN, CAN libre, escritura ECU ni borrado de averías.
6. TalkBack/insets y apps reales: Maps/Waze con destino ficticio, MediaSession de un
   reproductor autorizado y contacto ficticio con confirmación ACTION_DIAL. Registrar
   observaciones por APK; no iniciar llamadas ni confundir intent con ruta activa.
7. IA/Places opcionales: endpoint/proyecto, credencial de prueba, consentimiento y
   cuota/coste independientes. Revocar durante suspensión, cancelar y observar errores;
   sin telemetría/contactos/ubicación en prompts ni cuerpos privados en logs.

Procedimientos y acta en PHYSICAL_TEST_PROCEDURES.md; PHYSICALLY_VERIFIED permanece
PENDING hasta observación humana. Mantener editor solo en laboratorio y micrófono PTT.

Para abrir una PR posteriormente: revisar `git diff main...HEAD` y los informes,
publicar la rama únicamente cuando se autorice con
`git push -u origin feat/sprint9-jank-compatibility` y abrir la comparación
https://github.com/AlmanInDaHouse/CopilotDrive-AI/compare/main...feat/sprint9-jank-compatibility?expand=1
con SPRINT9_REPORT, tests, APK/hash, resultados negativos y limitaciones. Esperar Android
y Tests de esa rama. Los workflows aprobados de main no son CI de Sprint 9.
Firma de producción, merge y release son decisiones posteriores independientes.

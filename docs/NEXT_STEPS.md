# Siguiente sprint recomendado: 9 — validación física consentida

Sprint 7 está fusionado por PR #8 en main `328ffec4457d9badd3569413ceff0eb7de153954`.
Tests y Android del merge terminaron en success. Sprint 8 permanece en la rama
local `feat/sprint8-performance-readiness`, sin push, PR, merge ni release.
Consultar SPRINT8_REPORT, PERFORMANCE_COMPARISON, STABILITY_TEST_REPORT y
RELEASE_READINESS para la evidencia local y sus límites.

1. Teléfono autorizado: descubrimiento pasivo, presentar APK/API/modelo y operaciones,
   obtener consentimiento antes de instalar o ejecutar. STT/TTS es-ES audible con
   confirmación humana, cancelar, audio focus, ruido y recuperación. TalkBack/insets.
2. ELM327 Classic y coche detenido autorizados: RFCOMM, init, capacidades/PIDs de
   lectura, latencia/NO DATA, timeout, permiso revocado, desconexión, cierre y vuelta
   al simulador. Sin VIN/CAN libre/limpieza de averías ni conexión automática.
3. Rendimiento: repetir Macrobenchmark en teléfono bajo condiciones estables.
   No extrapolar el emulador ni convertir cotas del harness en tiempos de app.
   Medir con profiler recomposiciones, memoria/heap y consumo antes de nuevos cambios.
4. Cobertura API26/30: preparar AVDs oficiales independientes cuando se acuerden
   descarga y espacio. Repetir permisos/Compose/DataStore/voz/Bluetooth/MediaSession,
   lifecycle/rotación y recuperación; minSdk26 no basta como evidencia.
5. Proveedores opcionales: endpoint/credencial/proyecto de prueba, cuota y
   consentimiento de coste independientes para IA y Places. Revocación, cancelación,
   errores HTTP, atribuciones y coarse; nada comercial en CI o logs.
6. Interoperabilidad: reproductores autorizados/callbacks, destinos ficticios
   Maps/Waze y contactos ficticios con confirmación ACTION_DIAL. Sin llamada
   automática ni afirmación de ruta/ETA por abrir un intent.

Procedimientos y acta: PHYSICAL_TEST_PROCEDURES.md. Mantener PHYSICALLY_VERIFIED=PENDING
hasta observación humana ligada al APK. No ampliar contexto IA, edición en marcha,
escucha permanente ni servicios prolongados de micrófono.

Para publicar una PR posteriormente y con autorización: revisar `git diff main...HEAD`,
repetir checks si cambia código, ejecutar `git push -u origin feat/sprint8-performance-readiness`
y abrir PR a main con SPRINT8_REPORT y evidencia. Esperar ambos workflows de esa rama:
el CI del merge de Sprint 7 no es CI de Sprint 8. Merge y release son pasos separados.

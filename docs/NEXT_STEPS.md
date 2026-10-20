# Sprint11 — campaña física consentida

Sprint9 fusionado por PR#10 en main `793a3535713cd76cfbe6c85f9fbfb7bff21cb4b5`;
Android/Tests del merge success verificados. Sprint10 permanece en la rama local
`feat/sprint10-physical-pilot`, sin push/merge/release. Ver SPRINT10_REPORT.md para
regresiones, APK, offline API26 y límites; PHYSICAL_PILOT_GUIDE.md para la instalación.

1. Conectar teléfono y realizar descubrimiento/preflight pasivo. Presentar API/modelo,
   APK/SHA/permisos/firma previa y pruebas concretas. Autorizar explícitamente antes
   de instalar o actuar. No desinstalar ni borrar datos ante conflicto de firma.
2. Voz prioritaria: STT es-ES local si existe, fallback del sistema consentido, permiso,
   silencio/timeout/cancelar/dos solicitudes/foco/background. TTS motor/voz local,
   frase guiada y confirmación humana completa. Copiar acta exacta; UI temporal no
   verifica por sí sola. Registrar motores incompatibles, incluido Pico de API26.
3. TalkBack manual y foco/headings/respuesta larga/controles/editor/Ajustes/diálogos,
   fuente grande/horizontal/insets OEM; no habilitar servicios sin consentimiento.
4. Multimedia, Maps y Waze, contactos ficticios: registrar efecto observado, revocación
   y retorno a demo. ACTION_DIAL con confirmación; ninguna llamada automática o ETA
   inferida. Las tres APIs emuladas no certifican fabricantes ni hardware real.
5. ELM327+coche detenido y autorización específicos: seleccionar y confirmar conexión,
   capacidades/PIDs read-only/NO DATA/latencias/desconexión. Volver al simulador.
   Sin VIN, CAN libre, ECU writes ni borrar DTC.
6. Rendimiento con autorización independiente y recursos válidos: StartupTimingMetric,
   FrameTimingMetric/Perfetto,20 iteraciones viables, condiciones/cohortes iguales.
   No compilar durante medidas ni extrapolar el host/emulador. Corregir solo cuellos
   atribuidos. Host Sprint10 falla guardas de RAM/commit; no hubo campaña adicional.
7. IA/Places solo con endpoint/proyecto/clave restringida y credencial de pruebas,
   consentimiento/cuota/costes. Sin telemetría/contactos/coordenadas en IA ni llamadas
   comerciales en CI. Coarse-only también en el APK fusionado.

API26 offline: conservar SVC Wi-Fi como no soportado por permiso shell. Usar runner
Settings exclusivamente en AVD desechable con opt-in y estado restaurable; consultar
API26_OFFLINE_INVESTIGATION.md. Prueba A fake no sustituye B de conectividad Android.
No elevar privilegios ni repetir comandos137 sin motivo nuevo.

## Abrir PR posteriormente

Revisar estado, commits, informes y `git diff main...HEAD`. El árbol debe conservar
fuera de Git APKs/logs/trazas y secretos. Después de autorización de publicación:

```powershell
git push -u origin feat/sprint10-physical-pilot
```

Abrir comparación:
https://github.com/AlmanInDaHouse/CopilotDrive-AI/compare/main...feat/sprint10-physical-pilot?expand=1
Título sugerido: `Sprint10: guided voice pilot, API26 offline and APK evidence gates`.
Descripción: problema/resultados/cambios,215 casos distintos aprobados según SPRINT10_REPORT, hash APK, seguridad coarse-only,
intentos fallidos, hardware PENDING y performance BLOCKED. Esperar Android y Tests
de la rama; los workflows del merge Sprint9 no son CI de Sprint10. Push, PR remota,
merge, firma de producción y release no se han ejecutado en esta entrega.

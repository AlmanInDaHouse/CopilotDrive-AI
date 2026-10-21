# Siguiente paso — validar Sprint 11 en HONOR

La candidata 0.11.0-debug/versionCode 2 implementa REAL y la nueva cabina Compose.
Base: `origin/main` `be3344c` contiene Sprint10.1, incluido el acta física previa.
Rama local: `feat/sprint11-real-mode-premium-ui`. Sin push/merge/release.
Informe y APK exacto: [SPRINT11_REPORT.md](SPRINT11_REPORT.md).

## Antes de instalar

1. Conectar HONOR 400 Lite y ejecutar **solo** preflight pasivo:
   `python scripts/pilot_package.py --apk artifacts/sprint11/CopilotDrive-AI-sprint11-debug.apk --serial SERIAL --out artifacts/sprint11/honor-preflight.json`.
2. Comparar paquete, minSdk, firma y versión de la app instalada con la candidata.
   La candidata se compara además con el APK Sprint10 archivado: mismo paquete y
   certificado, versionCode 1 → 2. La comprobación live del teléfono sigue pendiente.
3. Presentar el SHA-256 del informe y solicitar autorización explícita antes de
   instalar. No ejecutar `install`, `am start`, permisos, audio ni capturas físicas
   por el mero hecho de conectar el teléfono.
4. Solo tras autorización: actualización compatible `adb -s SERIAL install -r APK`.
   No desinstalar, borrar datos ni usar downgrade ante conflicto. DataStore conserva
   nombre/ruta y layout v1; verificar widgets y voz guardados después de actualizar.

## Prueba humana propuesta

- Leer aviso de migración a REAL; comprobar widgets anteriores y ajustes de voz.
- REAL sin OBD: «No disponible», sin 42 %, canción ficticia o ETA inventada.
- Pulsar micrófono y preguntar «Jarvis, ¿cuánta gasolina queda?». Escuchar explicación
  de falta de conexión, comprobar respuesta escrita, cancelación y siguiente consulta.
- Entrar en Ajustes en entorno detenido, activar DEMO explícitamente y comprobar
  etiquetas visuales/habladas. Reiniciar app, verificar persistencia y volver a REAL.
- Fondo/retorno: no autoescucha ni micrófono de fondo. Vertical/horizontal y fuente
  ampliada: PTT/cancelación/Ajustes accesibles, sin recortes de insets OEM.
- Registrar APK exacto, intento y observación humana; callbacks no prueban audibilidad.

OBD requiere autorización separada, ELM327 emparejado y coche detenido. Consultar
solo PIDs mode 01 y registrar soporte real Juke sin suponer combustible/puertas/clima.
Google Places requiere clave restringida y cuota de pruebas, coarse location y
consentimiento propio. IA externa requiere endpoint/modelo/credencial opt-in; no es
requisito para Jarvis local. Multimedia/Maps/Waze/contactos necesitan efecto observado.

## Abrir PR después de autorización de publicación

Revisar `git status`, commits, informes, capturas y `git diff origin/main...HEAD`.
Los APKs y logs permanecen fuera de Git. Con autorización de push:

```powershell
git push -u origin feat/sprint11-real-mode-premium-ui
```

Comparación: https://github.com/AlmanInDaHouse/CopilotDrive-AI/compare/main...feat/sprint11-real-mode-premium-ui?expand=1

Título: `Sprint 11: real sources by default and native automotive cockpit`.
Descripción: REAL/DEMO y migración, respuestas sin datos inventados, design system y
pantallas Compose, ocho widgets preservados, pruebas y evidencia exactas del informe,
firma/SHA de la candidata, hardware/credenciales/performance pendientes. Esperar CI
Android/Tests de la rama. No ejecutar push, PR remota, merge ni release automáticamente.

Sprint 12 propuesto: piloto físico de este APK, hardware OBD, interoperabilidad y
proveedores reales consentidos; rendimiento emparejado en un host con recursos estables.

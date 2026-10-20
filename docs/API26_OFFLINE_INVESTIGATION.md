# API26 offline — investigación Sprint 10

2026-10-20. Solo `CopilotDrive_Sprint9_API26`, AOSP Android8.0.0 x86_64,
OSR1.180418.004/4931640; un AVD activo. No teléfono, root, grants privilegiados,
wipe, reboot ni modificación del AVD valioso. No se descarga ninguna imagen nueva.

## Hallazgo reproducido

El baseline de Sprint9 instalado (`501b01a1…bc08d5c`) pasó34/34 pruebas tras un
arranque en frío sin snapshots. `sys.boot_completed=1` y `service check activity`
confirmaron framework disponible. El guest tenía inicialmente MemAvailable1024840
KiB y1006484 KiB después del intento. El host tenía presión severa; se documenta
independientemente, no se identifica como causa por el código137.

`svc help` funciona. `/system/bin/svc` exporta CLASSPATH=/system/framework/svc.jar
y ejecuta app_process; app_process enlaza con app_process64. UID shell2000 sin
privilegios adicionales. `cmd connectivity help` devuelve «No shell command
implementation»; `cmd wifi help` no proporciona una alternativa útil en esta imagen.
`pm check-permission` tampoco existe aquí. Se inspeccionó dumpsys package de
com.android.shell: no declara CHANGE_WIFI_STATE. dmesg está protegido; no se elevó acceso.

Un único intento SVC guardó antes los flags1/1 y el estado conectado. `svc wifi
disable` y la restauración `svc wifi enable` devolvieron137; `svc data enable`
devolvió0. El buffer crash asocia esos procesos con:

`SecurityException: WifiService: Neither user 2000 nor current process has android.permission.CHANGE_WIFI_STATE.`

La traza pasa por IWifiManager.setWifiEnabled y WifiCommand.run, seguida por otro
fallo «Bad file descriptor» en el manejo de excepción. ActivityManager seguía activo.
Esto fundamenta la limitación de permiso de la ruta SVC en esta imagen; no demuestra
un OOM ni identifica retrospectivamente todos los137 históricos. No es un crash del
proceso de CopilotDrive ni una caída demostrada de system_server: el marcador genérico
«SYSTEM PROCESS» de app_process debe leerse con el stack/proceso correspondiente.

La implementación [AOSP WifiCommand Android8](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-8.0.0_r1/cmds/svc/src/com/android/commands/svc/WifiCommand.java)
llama al servicio Wi-Fi como com.android.shell y captura RemoteException, sin manejar
SecurityException. La revisión exacta instalada no se equipara byte a byte a ese tag;
la traza y el script leídos del guest son la evidencia primaria de esta ejecución.
Las URLs de Googlesource no estuvieron disponibles en la herramienta web; se consultó
el espejo AOSP. No se parcheó Android, SELinux ni permisos para evitar el fallo.

El primer JSON conserva restoration=FAILED por los comandos fallidos, aun cuando
flags1/1, red predeterminada y dos redes conectadas coincidían con el original. Una
lectura pasiva posterior confirmó ese estado. No se reescribe ese intento como PASS.

## Alternativa reversible y prueba B

`scripts/api26_offline.py --wifi-settings-ui` abre WIFI_SETTINGS y localiza el
Switch real por resource-id/clase/bounds observados. No usa coordenadas fijas,
binder calls numéricas ni permisos especiales. Datos móviles usa la ruta svc data
disponible. El runner exige opt-in, emulador positivo, API26, nombre del AVD
desechable y APK instalado con hash exacto; persiste el estado antes de mutar.

Un primer intento Settings quedó bloqueado por un diálogo de crash de Pico TTS
procedente de la suite. No se cambió la red y se conservaron los fallos de verificación
UI/restauración. Se cerró explícitamente el diálogo observado del motor en el AVD
desechable, sin deshabilitar/desinstalar ni cambiar su configuración, y se verificó
el interruptor disponible antes de repetir una sola vez.

La repetición con APK intermedio `e5555bad…65251d46` aprobó la consulta local:

| Momento | Wi-Fi/datos | Red predeterminada | Redes conectadas |
| --- | --- | --- | ---: |
| Antes | 1/1 | Sí | 2 |
| Antes de consulta | 0/0 | No | 0 |
| Después de consulta | 0/0 | No | 0 |
| Restauración inicial | 1/1 | Sí | 1, asociación Wi-Fi aún pendiente |

1 test,2909ms; el callback/acústica no forman parte del criterio. Se reforzó después
el runner para esperar también el número de redes previo, además de flags y default;
la validación del APK final se registra separadamente en SPRINT10_REPORT y TEST_RESULTS.
La repetición final del APK `fcec5256…b7e643c` aprobó1/1 en2280ms, con0/0,
sin default y0 redes conectadas antes/después. Restauración final1/1, default activo
y2 redes conectadas: el mismo snapshot agregado que al inicio. Preflight pasivo de
la instalación confirmó firma compatible y API26>=minSdk26. Se cerraron dos diálogos
Pico observados antes del intento; no se habilitó ni deshabilitó el motor.
Nunca confundir esta primera observación parcial con restauración instantánea de todas
las asociaciones. Los fallos se conservan en artifacts/sprint10/api26-offline-*.json.

## A y B son evidencias distintas

- A: PhysicalVoicePilotTest.localJarvisDoesNotInvokeUnavailableRemoteProviderTestA:
  proveedor AI fake indisponible, consulta de gasolina local, cero invocaciones de AI.
  AUTOMATED_TESTED. No prueba nada sobre la conectividad del sistema Android.
- B: query instrumentada del ViewModel con radios desactivadas y ConnectivityService
  sin default ni redes conectadas antes/después. EMULATOR_TESTED cuando ese intento
  y su restauración aprueban; fallo de formato/conexión/restore conserva BLOCKED.
- SVC Wi-Fi en esta imagen: UNSUPPORTED por permiso del shell; su intento queda
  BLOCKED y sus códigos137 permanecen como evidencia. No se repite indefinidamente.
- STT local, voces offline audibles y teléfono físico: PENDING. La consulta local
  escrita no prueba reconocimiento ni síntesis offline. Pico TTS tuvo SIGSEGV en
  libttspico/SynthThread; se registra como limitación del motor de esta imagen.

Reproducir solo tras instalar los dos APK en este AVD desechable y cerrar overlays
conocidos, nunca sobre un teléfono ni sobre `CopilotDrive_API35`:

```powershell
python scripts/api26_offline.py --serial emulator-5554 --allow-disposable-network-test --wifi-settings-ui --apk artifacts/sprint10/CopilotDrive-AI-sprint10-debug.apk --out artifacts/sprint10/api26-offline-final.json
```

La lectura de ConnectivityService no hace ping ni consulta un proveedor comercial.
La restauración compara flags/default/número de redes, no identidades/IP ni un servidor
externo. Un cierre abrupto del host requiere recuperar manualmente los flags guardados
por la UI; no hay garantía de finally ante pérdida de energía. Sin borrado de datos.

# Piloto físico controlado — Sprint 10

2026-10-20, Europe/Madrid. No se ha detectado un teléfono físico autorizado.
La candidata debug y el recorrido están preparados; calificación física PENDING.
APK convencional para Android abierto, mínimo Android 8/API26; no Android Auto/AAOS.
Usar exclusivamente en un entorno detenido. Nunca probar mientras se conduce.

## Identificar el dispositivo sin modificarlo

En el teléfono, habilita Opciones de desarrollador y Depuración USB tú mismo.
Conecta un cable de datos, desbloquea y revisa el diálogo de confianza RSA del PC.
No hace falta root ni desbloquear bootloader. En PowerShell, desde el checkout:

```powershell
$adb = 'C:/Users/manul/AppData/Local/CopilotDriveAndroidSdk/platform-tools/adb.exe'
& $adb devices -l
python scripts/qualify_android.py --out artifacts/sprint10/phone-discovery.json
```

`unauthorized` requiere aceptar en el teléfono; `offline` exige revisar conexión.
Un emulador no es un teléfono. Anota localmente el serial para seleccionar el destino;
en actas utiliza un alias. No publiques seriales, MAC, VIN o capturas privadas.

```powershell
$serial = 'SERIAL_DEL_TELEFONO'
$apk = 'artifacts/sprint10/CopilotDrive-AI-sprint10-debug.apk'
python scripts/pilot_package.py --apk $apk --serial $serial --out artifacts/sprint10/phone-preflight.json
```

El preflight es pasivo: inspecciona firma/hash/permisos/API/modelo y, si existe la app,
lee una copia temporal del APK instalado para comparar certificados. No abre la app,
instala, concede permisos ni toca datos. Un conflicto de firma o API<26 bloquea el
procedimiento. La copia temporal se elimina. No sustituir esa comprobación por asumir
que todas las builds debug usan la misma clave. Historial de rotación de firmas no
se califica automáticamente; el chequeo conservador puede bloquear una actualización.

## Autorización e instalación

Antes de actuar en el teléfono, presentar al usuario: modelo/API, paquete
`ai.copilotdrive.app`, ruta/hash/certificado y permisos del informe APK; app previa y
compatibilidad de firma; y estos pasos: instalar -r, abrir, completar onboarding,
conceder micrófono al pulsar, prueba local hablada, frase TTS, cancelar y volver de
segundo plano. Obtener autorización explícita para instalación y esas pruebas.
Rendimiento, OBD, acceso a notificaciones/contactos y proveedores requieren acuerdos
independientes. Los comandos siguientes solo se ejecutan tras esa autorización:

```powershell
$expected = (Get-Content artifacts/sprint10/apk-metadata.json -Raw | ConvertFrom-Json).candidate.sha256
if ((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLower() -ne $expected) { throw 'APK diferente' }
& $adb -s $serial install -r $apk
& $adb -s $serial shell am start -W -n ai.copilotdrive.app/.MainActivity
```

Si la firma no coincide, detenerse. Nunca desinstalar automáticamente, usar downgrade,
`pm clear` o restablecer permisos/datos globalmente. `install -r` conserva datos, pero
sí reemplaza el APK; no supone una copia de seguridad ni garantiza rollback a otra
versión. Guardar el APK previo compatible cuando sea necesario antes de autorizar.
El APK AndroidTest es para AVDs desechables, no instalarlo en el teléfono personal.

Permisos declarados: micrófono, contactos, Internet, ubicación aproximada,
Bluetooth clásico hasta API30 y Bluetooth Connect. Las dependencias añaden
ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE y un permiso interno de receptor; el listado exacto del APK prevalece. Conceder solo lo
necesario al comenzar cada prueba; no habilitar servicios de accesibilidad por script.

## Preparar español y hablar con Jarvis

1. En los ajustes Android de texto a voz, comprobar motor instalado y descargar una
   voz española es-ES local si el proveedor la ofrece. Descargas/costes se aceptan
   manualmente. Volver a Ajustes de CopilotDrive, comprobar catálogo, elegir voz.
   «Instalar datos de voz» abre un instalador disponible, no garantiza que exista.
2. Comprobar reconocimiento local. La API on-device se consulta solo desde API31;
   versiones anteriores pueden necesitar el servicio del sistema. Instalar/configurar
   ese servicio manualmente si se desea. Sin motor disponible, conservar uso textual.
3. El fallback «Permitir motor STT del sistema; puede enviar audio a su proveedor»
   está desactivado inicialmente. Activarlo solo con consentimiento separado.
4. En Device Lab, «Comprobar voz instalada» es pasivo. «Reproducir frase guiada»
   pide confirmación antes de reproducir. Tras callback, indicar si se oyó completa
   y correctamente. Registrar esa respuesta en acta; nunca deducirla del callback.
5. Pulsar «Pulsar para hablar con Jarvis», conceder micrófono y decir «cuánta gasolina
   queda». Ver LISTENING/transcripción/orquestador/política/texto/TTS. El diagnóstico
   oculta contenido; la respuesta escrita se consulta en el dashboard. Los datos del
   simulador deben anunciarse como simulados. «Probar texto local sin micrófono» no
   valida STT. Consultar VOICE_QUALIFICATION.md para la matriz completa.
6. Repetir dos solicitudes, silencio, cancelar, background/foreground e interrupción
   consentida. Recuperar foco nunca debe reiniciar micrófono automáticamente. No
   grabar audio, conversación o pantalla con información privada.

En esta campaña, Pico TTS de la imagen emulada API26 produjo SIGSEGV en libttspico.
No es evidencia sobre un motor de un teléfono. Si el motor real falla, conservar el
texto, registrar versión/motor/código y elegir manualmente otro motor local compatible.

## Integraciones separadas

| Prueba | Acción y observación requerida |
| --- | --- |
| Multimedia | Reproductor y contenido ficticio autorizados; iniciar reproducción en él, conceder acceso a notificaciones desde Android, seleccionar sesión en Ajustes. Play/pause/siguiente/anterior y cambio de sesión: confirmar efecto en reproductor. Revocar acceso y comprobar denegación; volver a demo. Un comando enviado no demuestra efecto |
| Maps y Waze | Instalar manualmente si se desea, destino ficticio, abrir intent y volver. Registrar apertura observada; no ruta activa ni ETA sin datos verificables |
| Contactos | Crear contacto ficticio sin datos personales, permitir lectura al probar, desambiguar y confirmar número. Solo ACTION_DIAL; no pulsar llamar. Revocar y retirar solo el fixture creado |
| OBD | Teléfono+ELM327 emparejado+vehículo detenido y autorización específica antes de conectar. Seleccionar manualmente, confirmar, leer capacidades/PIDs permitidos, registrar válidos/NO DATA/latencias; desconectar y volver al simulador. No VIN, CAN libre, ECU writes, borrar DTC o diagnóstico intrusivo |
| IA/Places | Endpoint/clave restringida y proyecto/credencial de pruebas, consentimiento y cuota/costes independientes. IA: solo texto sintético, nunca telemetría/contactos/coordenadas. Places: coarse, resultados y atribuciones, cancelar/revocar. Servicios comerciales excluidos de CI |
| Accesibilidad | TalkBack instalado y activado por el usuario tras consentimiento. Orden/foco/headings, respuesta extensa, voz, editor, Ajustes/diálogos, fuente1.5×/2×, horizontal e insets OEM. Si no existe lector: observación humana PENDING |

## Rendimiento con autorización independiente

Registrar APK benchmark y runner, firma/hash, API/modelo, compilación, temperatura,
energía, orientación/fuente/display y permisos. Preparar onboarding fuera de medida.
Usar módulo macrobenchmark opt-in y Android Studio/runner oficial con selección explícita
del teléfono; StartupTimingMetric, FrameTimingMetric y Perfetto, según BENCHMARK_RUNBOOK.
Los runners Python actuales rechazan teléfonos deliberadamente; no retirar esa guarda.
La ejecución manual oficial solo se prepara después del consentimiento independiente.
No usar `CompilationMode.None` antes de API34; en26/30, Ignore conserva compilación
existente. Mantener cohortes idénticas y 20 iteraciones cuando sea viable; no comparar
Ignore con None ni debug con benchmark. No instalar/compilar durante ventanas de medida.
Host sin RAM suficiente, paging intenso, calentamiento o interrupciones: BLOCKED;
no rellenar métricas con cero ni extrapolar los resultados del emulador.

## Acta, errores y restauración

Copiar `specs/017-physical-pilot/pilot-record-template.json` y completar solo lo observado:
sessionId, intento, fecha UTC, commit/hash, API/modelo anónimo, escenario/precondiciones,
método, callback, resultado humano, error fijo y referencia redactada. STT: registrar
coincidencia sí/no con la frase sintética; no guardar transcripción. Mantener estados
AUTOMATED_TESTED/EMULATOR_TESTED separados de PHYSICALLY_VERIFIED. Un sí en Device Lab
es temporal y pierde validez al repetir/cancelar; requiere acta exacta para calificación.

Antes de probar, anotar permisos, voz/motor, notificaciones, accesibilidad y configuración
propios. Al acabar: cancelar, desconectar OBD, volver al simulador, borrar etapas/historial
de diagnóstico, retirar fixtures, revocar solo permisos/accesos concedidos para el piloto
y restaurar motor/voz/volumen/foco/rotación/fontScale originales desde Android. Restaurar
TalkBack solo con el usuario. Si falla restauración, registrar FAILED y detenerse.
No exportar logcat general de un teléfono: puede contener datos ajenos. Revocar confianza
USB desde el teléfono si se desea. No desinstalar ni borrar datos sin nueva decisión.

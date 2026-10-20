# InstalaciÃ³n Android de revisiÃ³n

## Para probar el APK
En esta sesiÃ³n: `artifacts/CopilotDrive-AI-debug.apk`. Copiar al dispositivo Android 8.0+ que admita
APKs de terceros, abrirlo y autorizar la instalaciÃ³n desde esa fuente. Abrir CopilotDrive AI.
No instalar como app de sistema. No requiere root, Python ni cuenta de IA.

Con USB debugging autorizado en un dispositivo de prueba:

```powershell
adb install -r artifacts/CopilotDrive-AI-debug.apk
adb shell am start -n ai.copilotdrive.app/.MainActivity
```

Usar solo detenido. Completar onboarding. Â«Probar consultaÂ» funciona sin micrÃ³fono; la respuesta visual
identifica simulaciÃ³n y 42 %. Para escucharla hay que tener un motor TTS con voz local instalada.
El APK no descarga motores ni modelos de voz.

## Compilar desde fuente
Instalar JDK 17 y Android SDK con Android Studio o command-line tools oficiales.
Configurar JAVA_HOME. Instalar `platforms;android-35` y `build-tools;35.0.0` con sdkmanager.
Configurar ANDROID_HOME o `android/local.properties` con `sdk.dir=C:/ruta/al/sdk`.
No versionar local.properties ni SDKs. La primera compilaciÃ³n necesita red para dependencias.

```powershell
.\scripts\android.ps1
.\scripts\android.ps1 :app:connectedDebugAndroidTest
```

En Linux/macOS, ejecutar los comandos gradlew del README. La distribuciÃ³n del wrapper usa checksum fijo.
El APK debug se genera en `android/app/build/outputs/apk/debug/app-debug.apk`; se firma con la clave debug
del entorno. No es la identidad de firma final de producciÃ³n y no es reproducible byte a byte entre claves.

## DiagnÃ³stico
- Sin STT local: autorizar motor del sistema en Ajustes solo si se acepta su tratamiento de audio.
- Sin voz TTS espaÃ±ola local: instalarla en ajustes de idioma/voz del dispositivo o seleccionar otra voz local.
- Sin sesiones: abrir reproductor compatible y autorizar acceso a notificaciones. No se leen mensajes.
- Varias sesiones sin favorita inequÃ­voca: elegir explÃ­citamente en Ajustes.
- Sin Maps/Waze: se informa ausencia; no se muestra ETA inventada.
- Sin contactos: activar permiso al usar la bÃºsqueda. Denegarlo no bloquea el dashboard.
- RevocaciÃ³n: capacidades y controles se reevalÃºan al volver a la app y durante foreground.

## Comandos
Â«Jarvis, cuÃ¡nta gasolina quedaÂ», Â«temperaturaÂ», Â«estado del cocheÂ», Â«pon mÃºsicaÂ», Â«pausa SpotifyÂ»,
Â«siguiente canciÃ³nÂ», Â«llÃ©vame a Gran VÃ­a, MadridÂ», Â«Waze MadridÂ», Â«llama a MercedesÂ»,
Â«unoÂ»/Â«dosÂ» para desambiguar, Â«confirmarÂ» o Â«cancelarÂ». ConfirmaciÃ³n caduca a los 30 segundos.
Â«Pausa SpotifyÂ» opera sobre la sesiÃ³n seleccionada, que puede ser de otra aplicaciÃ³n: revisar el proveedor
mostrado en el dashboard. No hay integraciÃ³n Spotify App Remote especÃ­fica todavÃ­a.

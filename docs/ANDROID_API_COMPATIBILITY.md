# Sprint 9 — compatibilidad Android

APK convencional, minSdk 26/targetSdk 35. El mínimo declarado no acredita una
ejecución en Android 8. Solo se califican escenarios realmente observados.
Descubrimiento pasivo inicial: un emulador API35, ningún teléfono físico autorizado.

## Imágenes y aislamiento

Catálogo oficial [Google AOSP system images](https://dl.google.com/android/repository/sys-img/android/sys-img2-3.xml):

| API | Imagen oficial | Descarga comprimida | Estado inicial |
| --- | --- | ---: | --- |
| 26 / Android 8 | system-images;android-26;default;x86_64, r1 | 474178332 bytes | No instalada |
| 30 / Android 11 | system-images;android-30;default;x86_64, r11 | 676379777 bytes | No instalada |
| 35 / Android 15 | system-images;android-35;default;x86_64, r2 | Ya instalada | Disponible |

El usuario autorizó expresamente ambas descargas y licencias necesarias durante
esta sesión. Estimación presentada: 8–12 GB de espacio de trabajo, unos22 GB libres
antes de instalar. Solo repositorio oficial de Google, SDK de herramientas local.
AVDs independientes `CopilotDrive_Sprint9_API26/30/35`, sin userdata copiada del
entorno valioso y sin wipe/snapshots. Un solo emulador activo; builds y medidas
secuenciales. Las APIs antiguas no se simulan cambiando una propiedad de API35.

## Cobertura y límites

La suite directa recorre onboarding/MainActivity, dashboard, layouts/presets,
persistencia DataStore/recreate, Device Lab pasivo, ausencia/catálogo TTS, permisos
y presentación Bluetooth, MediaSession fixture autorizada/revocada, Jarvis local,
Places demo y Places sin consentimiento/proveedor. Rotación/offline se registran
como smoke separado; no deducirlos de compilar el APK.

`compatibility_android.py` exige el nombre de AVD desechable, opt-in y propiedades
positivas de emulador. Instala con -r, verifica firma/hash antes y después y ejecuta
am instrument. El test de Keystore sustituye el fixture y luego lo borra; MediaSession
cambia listener; editor guarda layout. No ejecutar esta suite sobre datos valiosos.
No se utiliza connectedDebugAndroidTest sobre el AVD original.

```powershell
python scripts/compatibility_android.py --serial emulator-5554 --allow-disposable-install-and-tests --apk android/app/build/outputs/apk/debug/app-debug.apk --test-apk android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk --out artifacts/sprint9/compatibility-api26.json
```

Repetir tras arrancar cada AVD específico. La API se lee del sistema, no del nombre.
Resultados ejecutados se mantienen en SPRINT9_REPORT y TEST_RESULTS, con JSON local
por versión. Ninguna prueba automática acredita audio audible, radio ELM327, coche,
TalkBack humano, Maps/Waze físico, GPS ni consultas a proveedores comerciales.

## Guardas revisadas

| Ruta | Guarda existente/requisito |
| --- | --- |
| SpeechRecognizer on-device | SDK>=31 antes de disponibilidad/creación moderna; fallback requiere consentimiento |
| Bluetooth | Permiso clásico declarado hasta 30; CONNECT y solicitud runtime solo>=31; sin SCAN ni escrituras |
| Notification listener | isNotificationListenerAccessGranted solo>=27; lectura actual del componente exacto en 26, sin caché de permisos |
| Audio focus | AudioFocusRequest/USAGE_ASSISTANT disponibles desde26, mínimo real del APK |
| java.time | LocalTime/DateTimeFormatter disponibles desde26 |
| Memoria | MemoryInfo summary desde23; PSS/heap instrumentado no certifica una fuga |
| ApplicationExitInfo | Solo>=30; contadores null/UNSUPPORTED en 26, nunca cero inventado |
| Macrobenchmark | None solo>=34; Ignore explícito en 26/30 sin reset/reinstalación; overrun ausente antes31 |

No se ha rebajado ninguna política de permisos, consentimiento o validación para
hacer pasar una API. Las limitaciones observadas por versión deben conservarse.

## Hallazgo API26 — revocación multimedia

La primera suite detectó un timeout en MediaSession: Android 8.0 responde
`No shell command implementation` a `cmd notification`. Se adaptó la fixture
de API26 al ajuste secure del listener, con restauración del valor original.
Un primer intento de adaptación agregó comillas literales porque UiAutomation
usa Runtime.exec, no un shell; se corrigió y se conservaron ambos intentos.

La fixture corregida expuso otro problema: al eliminar el ajuste, el helper
NotificationManagerCompat no recalcula su conjunto de paquetes si el valor es
null. `AndroidMediaAdapter.authorized()` podía seguir devolviendo true en 26.
Ahora esa rama lee el estado actual y comprueba el ComponentName exacto; 27+
conserva la API pública de NotificationManager. La prueba exige permiso falso tras
revocación y restauración real al terminar. No concede acceso ni ignora errores
para hacer pasar el test. No se afirma que esta corrección reduzca el jank.

Fuentes primarias:
[UiAutomationConnection y Runtime.exec](https://android.googlesource.com/platform/frameworks/base/+/android-8.1.0_r27/core/java/android/app/UiAutomationConnection.java),
[caché de NotificationManagerCompat](https://android.googlesource.com/platform/frameworks/support/+/65a3eefbbf4cb18106821def86e7466be194e0b5/core/core/src/main/java/androidx/core/app/NotificationManagerCompat.java).
Los fallos originales se conservan en artifacts/sprint9/compatibility-api26-attempt*.
La revisión r11 del paquete API30 tiene Pkg.Revision=10 en source.properties y 11
en package.xml del SDK Manager; se conserva esa discrepancia del archivo oficial.

# Sprint10 — piloto y acta exacta

Reutiliza DeviceQualification/EvidenceLevel/CompatibilityStatus. Nuevos contratos en
specs/017-physical-pilot: PhysicalPilotSession, TestDeviceProfile, HumanObservation,
VoiceQualificationResult, AcousticConfirmation, IntegrationQualification,
PerformanceEnvironment y PilotReadinessResult. PHYSICALLY_VERIFIED exige entorno
físico/método manual/evidencia y observación de sesión/build/escenario/fecha/intento
coincidentes. Callback o estado UI no crean una calificación física.

PHYSICAL_PILOT_GUIDE.md prepara conexión, autorización, firma/permisos/SDK, instalación,
voz española, integraciones y restauración. `pilot_package.py` solo inspecciona: si se
indica un serial autorizado, lee modelo/API y firma del APK previo mediante copia
local temporal; no instala, inicia Activity ni concede permisos. Firma conflictiva
bloquea, sin desinstalar. Listado exacto de permisos del APK, no solo manifest fuente.

API26 offline: api26_offline.py solo admite el AVD desechable explícito; SVC Wi-Fi
no soportado por permiso shell, alternativa Settings. Ningún runner automático
produce PHYSICALLY_VERIFIED. Ver API26_OFFLINE_INVESTIGATION.md para intentos y límites.

## Contratos y procedimientos anteriores conservados

# Calificación de dispositivos — Sprint 8

Sprint 7 está fusionado por PR #8 (328ffec), CI Android/Tests success. Sprint 8 es
local sin publicación. Se reutilizan los contratos de calificación y el runner
pasivo; se añaden contratos PerformanceReadiness, benchmark_android y stability_android.
Estos dos runners solo actúan sobre emuladores explícitos y APKs comprobados por hash.
Ver BENCHMARK_RUNBOOK para comandos y PHYSICAL_TEST_PROCEDURES para los recorridos
físicos pendientes. TTID/TTFD/cota UI, funcional/emulado/humano y servicio comercial
son categorías distintas. Raw Perfetto/logs requieren revisión antes de compartir.

Una calificación describe un escenario en un APK exacto. No homologa una familia
de teléfonos ni autoriza uso en conducción. Contratos ejecutables:
[DeviceQualification.kt](../android/core/src/main/kotlin/ai/copilotdrive/core/DeviceQualification.kt);
especificación `specs/014-device-qualification/`.

| Estado | Significado |
|---|---|
| AUTOMATED_TESTED | Contrato ejercitado con pruebas JVM/fakes |
| EMULATOR_TESTED | Escenario observado en emulador identificado |
| PHYSICALLY_VERIFIED | Observación humana física, con acta y build exactos |
| FAILED | Escenario intentado, resultado negativo |
| UNSUPPORTED | Capacidad ausente en ese entorno |
| BLOCKED | No puede ejecutarse por un requisito o error de herramientas |
| PENDING | Aún no ejecutado; no equivale a fallo |

EvidenceLevel conserva los valores existentes de diagnóstico: UNIT_TEST, FAKE,
EMULATOR, ANDROID_DEVICE, OBD_ADAPTER, PARKED_VEHICLE, PENDING. No es un estado
de compatibilidad. Un callback en un teléfono no es una observación acústica.
El constructor rechaza PHYSICALLY_VERIFIED para emulador o método automatizado.

## Procedimiento reproducible en Windows

Desde la raíz del repositorio, con JDK 17 y SDK 35 disponibles:

```powershell
python scripts/qualify_android.py --out artifacts/sprint8/discovery.json
python scripts/qualify_android.py --serial emulator-5554 --out artifacts/sprint8/environment.json
$apk = 'android/app/build/outputs/apk/debug/app-debug.apk'
$apkHash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
python scripts/qualify_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --install --launch --allow-install --allow-active --dry-run --out artifacts/sprint8/dry-run.json
# Solo después de revisar el dry-run; este ejemplo actúa sobre el emulador.
python scripts/qualify_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --install --launch --allow-install --allow-active --out artifacts/sprint8/install.json
```

Completar onboarding desde la app antes de medir. Los tests conectados de Gradle
desinstalan al finalizar: instalar de nuevo ambos APK si se quiere repetir un test
con `adb am instrument`. La instalación -r no borra datos ni permite downgrade;
si una firma no coincide, detenerse sin desinstalar/borrar preferencias.

```powershell
python scripts/qualify_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --matrix --allow-active --out artifacts/sprint8/screens.json
python scripts/qualify_android.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --samples 20 --allow-active --out artifacts/sprint8/performance.json
python scripts/sprint7_recovery_smoke.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --allow-active --dry-run --out artifacts/sprint8/recovery-dry-run.json
python scripts/sprint7_recovery_smoke.py --serial emulator-5554 --apk $apk --expected-sha256 $apkHash --allow-active --out artifacts/sprint8/recovery.json
```

El runner descubre adb por parámetro/PATH/SDK/local.properties/.tools; solo consulta
dispositivos autorizados. Verifica paquete, min/target SDK, firma debug y SHA-256 antes
de instalar; compara también el hash del APK instalado antes de medir. El informe
incluye commit y workingTreeDirty; no se debe ocultar un árbol modificado.
No instala SDKs, acepta licencias ni habilita servicios. El inventario exporta alias,
estado y transporte; no serial/MAC/VIN. UIAutomator se analiza en memoria y su archivo
temporal se elimina; no se exportan texto de pantalla ni capturas.

La matriz modifica tamaño, densidad, fuente y rotación exclusivamente en emulador.
Restaura overrides originales (incluida ausencia de font_scale), intenta cada ajuste
aunque otro falle y comprueba el resultado. Un fallo de restauración produce BLOCKED.
No cambia red, permisos, preset ni preferencias. El hash de layout verifica persistencia
tras cierre/reinicio de proceso; esto no certifica restauración de memoria tras LMK.

El smoke de recuperación adicional exige emulador, onboarding y layout guardado;
prueba rotación real 90°/0° sin force-stop, HOME/foreground y consulta local Jarvis
mediante el test instalado con Wi-Fi/datos deshabilitados temporalmente. Compara hash
de layout y restaura rotación/red, incluso ante fallo; rechaza estados de red no
restaurables antes de modificar. Exporta solo resultado del test y ausencia de crash
observado para el proceso actual; no texto de respuesta ni buffer de logcat. Requiere
los dos APKs instalados y --allow-active; no tiene vía de actividad en teléfono físico.

## Hardware y servicios externos

Con teléfono físico: primero ejecutar solo descubrimiento. Identificar API/modelo,
revisar minSdk y explicar instalación/escenarios concretos. Solicitar confirmación
humana antes de instalar o hacer pruebas activas; solo entonces usar --allow-physical
junto a --allow-active y, para instalar, --allow-install. La matriz de display se
rechaza incluso con esos flags. El runner nunca genera PHYSICALLY_VERIFIED.

OBD, micrófono, TTS, contactos, IA y Places se prueban desde la UI con sus propios
consentimientos. Para proveedores comerciales, confirmar credenciales de prueba,
cuota y costes antes de cualquier solicitud. No incluir esos datos en la evidencia.
No conectar al coche automáticamente. Vehículo detenido y solo PIDs permitidos.

Acta física: fecha, commit completo, SHA-256, Android/API, modelo, escenario,
precondiciones, pasos, método, observación/código, duración monotónica, nivel de
evidencia, resultado, observador y limitaciones. Usar identificadores anónimos para
adaptador/vehículo, sin VIN, matrícula, MAC, ubicación precisa o conversación.
La confirmación audible es una observación humana separada del callback TTS.

## Verificación y límites de medición

`am start -W` mide inicio de Activity tras force-stop (proceso frío, cachés calientes).
La sonda UI aporta una cota superior de dashboard observable, incluyendo adb y
UIAutomator; no es TTFD ni latencia de interacción del usuario. Memoria: TOTAL PSS
de dumpsys, KiB. Reloj de la sonda: perf_counter_ns; muestras crudas numéricas y
percentiles nearest-rank desde 20. No extrapolar debug/emulador a hardware/release.
Ver PERFORMANCE_BASELINE y SPRINT7_REPORT para ejecuciones reales.

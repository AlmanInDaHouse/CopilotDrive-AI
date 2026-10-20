# Sprint 3 — informe técnico

## Base de trabajo

`origin/main` avanzó a `74507bb` (PR #3, Sprint 2 fusionado). Se creó `backup/sprint3-before-main-rebase-20261020` antes del rebase. `git range-diff` confirmó equivalencia de los cuatro commits de Sprint 3; `f3d2ba2` añadió una corrección y pruebas después. No se hizo push ni merge.

`sdkmanager` no estaba en PATH, pero Command-line Tools existía en `%LOCALAPPDATA%\CopilotDriveAndroidSdk\cmdline-tools\latest\bin`. Tras autorización explícita del usuario, `sdkmanager --licenses` informó «All SDK package licenses accepted» y SDK Manager instaló Platform 35, Build-Tools 35.0.0 y Platform-Tools 37.0.1. JDK Microsoft 17.0.10.7 está en `%LOCALAPPDATA%\Programs\Microsoft`. `android/local.properties` ignorado por Git apunta al SDK externo; `JAVA_HOME` y `ANDROID_HOME` se fijaron solo para validar. Gradle Wrapper, AGP y Kotlin no se cambiaron.

## Implementación

`BluetoothObdProvider` conserva el contrato `VehicleDataProvider`. `Elm327Protocol` inicia el adaptador con secuencia fija; `VehicleCapabilityDiscovery` consulta 0100 y, si procede, 0120; `TelemetryPollingCoordinator` lee exclusivamente 010C, 010D, 0105 y 012F soportados. `ObdResponseParser` acepta eco y formatos con o sin espacios y rechaza `NO DATA` o tramas incompletas. El adaptador Android usa RFCOMM SPP público, cierre de socket en cancelación y un worker privado para evitar bloquear el hilo principal. El dashboard y Jarvis comparten `VehicleContextEngine`; el simulador permanece disponible.

La interfaz exige seleccionar un dispositivo ya emparejado y confirmar un aviso experimental antes de conectar. En Android 12+ solicita BLUETOOTH_CONNECT; en Android 8–11 usa BLUETOOTH heredado. No solicita escaneo ni ubicación. La telemetría no se serializa hacia IA externa, no se consulta VIN y no hay envío libre de comandos. El simulador se renueva al consultarlo para no caducar tras un onboarding largo.

## Evidencia

- Python: 12/12.
- Kotlin/JVM: core 13/13, testing 42/42, app 2/2; 57/57. Bluetooth fake: 12/12.
- Android instrumentado, emulador API 35: 12/12.
- Android lint: 0 errores y 10 avisos (8 de versiones de dependencias, 2 `UseKtx`). `assembleDebug` y `assembleDebugAndroidTest`: BUILD SUCCESSFUL.
- `scripts/check_repository.py` y `git diff --check`: aprobados.
- APK: `artifacts/CopilotDrive-AI-sprint3-debug.apk`, 11.762.683 bytes, SHA-256 `6f09ee0dca6d667eec93c81ef15f573817a2fbd00542fc54ef76d876d78f322c`. Firma debug v2 y un firmante verificados con `apksigner`.
- `aapt` confirmó minSdk 26 y target/compile SDK 35. El APK no contiene entradas `.env`, `.jks`, `.keystore`, `local.properties` ni el token de prueba. El manifest fusionado añade el permiso interno `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` de AndroidX.
- `adb install -r` y lanzamiento de MainActivity: correctos y sin errores `AndroidRuntime`. Tras 15 s en onboarding, el dashboard mostró 42 % simulado, velocidad 0 y refrigerante 88 °C. La consulta local respondió «Simulación. Combustible: 42 %» en texto aunque TTS no estaba disponible. Ajustes pidió Nearby devices y mostró que no hay adaptadores emparejados. Capturas y logs quedan en `artifacts/`, ignorado por Git.
- Dispositivo físico/OBD: no disponible ni probado. Ver `CONNECT_OBD.md` para la matriz manual.

## Limitaciones

La interoperabilidad ELM327, audio físico y proveedor comercial de IA necesitan pruebas en dispositivo real. BLE queda fuera del alcance. Combustible 012F puede no existir; autonomía y voltaje no se infieren. La reconexión se limita a dos intentos por sesión y las lecturas dejan de ser actuales en cuanto falla el transporte. Este documento no atribuye validación física a un fake.

El siguiente incremento recomendado es validación física de OBD y voz, antes de ampliar funciones. Borrador separado: `specs/013-hardware-validation/`.

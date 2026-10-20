# Sprint 3 — informe técnico

## Base de trabajo

El 20 de octubre de 2026, `origin/main` seguía en `b3646df` y no contenía Sprint 2 (`c92e6e8`). La rama `feat/sprint3-bluetooth-obd` se creó desde ese commit de Sprint 2 para conservar su arquitectura. No se hizo push ni merge. Antes de fusionar Sprint 3 habrá que integrar Sprint 2 en `main` y comprobar el historial.

## Implementación

`BluetoothObdProvider` conserva el contrato `VehicleDataProvider`. `Elm327Protocol` inicia el adaptador con secuencia fija; `VehicleCapabilityDiscovery` consulta 0100 y, si procede, 0120; `TelemetryPollingCoordinator` lee exclusivamente 010C, 010D, 0105 y 012F soportados. `ObdResponseParser` acepta eco y formatos con o sin espacios y rechaza `NO DATA` o tramas incompletas. El adaptador Android usa RFCOMM SPP público, cierre de socket en cancelación y un worker privado para evitar bloquear el hilo principal. El dashboard y Jarvis comparten `VehicleContextEngine`; el simulador permanece disponible.

La interfaz exige seleccionar un dispositivo ya emparejado y confirmar un aviso experimental antes de conectar. En Android 12+ solicita BLUETOOTH_CONNECT; en Android 8–11 usa BLUETOOTH heredado. No solicita escaneo ni ubicación. La telemetría no se serializa hacia IA externa, no se consulta VIN y no hay envío libre de comandos.

## Evidencia

- Python: 12 pruebas, todas aprobadas.
- `:core:test :testing:test`: 52 aprobadas, 0 fallos, en JDK 17; 9 cubren el flujo serial fake de Sprint 3.
- `scripts/check_repository.py`: aprobado tras actualizar la allowlist de permisos.
- Pruebas de Android app, lint, APK y firma: registrar resultado final después de instalar SDK Android 35.
- Dispositivo físico/OBD: no disponible ni probado. Ver `CONNECT_OBD.md` para la matriz manual.

## Limitaciones

La interoperabilidad ELM327, audio físico y proveedor comercial de IA necesitan pruebas en dispositivo real. BLE queda fuera del alcance. Combustible 012F puede no existir; autonomía y voltaje no se infieren. La reconexión se limita a dos intentos por sesión y las lecturas dejan de ser actuales en cuanto falla el transporte. Este documento no atribuye validación física a un fake.

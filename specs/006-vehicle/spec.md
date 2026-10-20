# Contexto vehículo

Cada métrica: nullable, unidad, fuente, timestamp, estado y calidad. TTL=10s. Autonomía no derivada. OBD Android transporte pendiente.

Alcance por etapa: consultar docs/IMPLEMENTATION_STATUS.md. Una especificación no implica implementación completa.

## Sprint 3: Bluetooth Classic OBD-II

En Android 8–15 se selecciona un adaptador previamente emparejado y se usa RFCOMM SPP. Sin Bluetooth, permiso, emparejamiento o respuesta válida, la aplicación sigue ofreciendo el simulador y la voz. La conexión exige un aviso experimental y consentimiento local. No hay lectura automática al iniciar.

`BluetoothObdProvider` implementa `VehicleDataProvider`; `VehicleContextEngine` conserva el TTL de 10 s. `BluetoothTransport` encapsula el socket y solo acepta un conjunto cerrado de operaciones de lectura; `Elm327Protocol` ejecuta una secuencia fija de inicio; `ObdPidRegistry`, `ObdResponseParser` y `VehicleCapabilityDiscovery` consultan 0100 y 0120 cuando procede. `TelemetryPollingCoordinator` intercala 010C, 010D, 0105 y 012F si está soportado. `VehicleConnectionState` es observable por StateFlow. Timeouts, cancelación, reconexión limitada y cierre del socket no bloquean la voz.

Estados: SIMULATED, CONNECTED_REAL, DISCONNECTED, UNSUPPORTED, STALE y ERROR; CONNECTING es transitorio. Los valores reales conservan unidad, fuente OBD, instante, disponibilidad y calidad SENSOR_REPORTED. Un PID no soportado o NO DATA produce valor nulo y «No disponible». La autonomía y tensión de batería quedan nulas; no se infieren. Al desconectar, valores previos nunca se presentan como actuales.

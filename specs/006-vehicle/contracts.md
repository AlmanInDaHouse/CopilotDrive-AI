# Contratos — Contexto vehículo

VehicleDataProvider.snapshot; VehicleContextEngine.read; AVAILABLE UNAVAILABLE STALE DISCONNECTED SIMULATED.

Fuentes de verdad implementadas: android/core/src/main/kotlin/ai/copilotdrive/core/. Sin interfaz de comandos arbitrarios.

Sprint 3 conserva `VehicleDataProvider.snapshot()` y `VehicleContextEngine.snapshot()`. `BluetoothObdProvider` expone StateFlow de `VehicleConnectionState`, `VehicleSnapshot`, capacidades y error. `BluetoothTransport.exchange()` acepta exclusivamente `ObdWireRequest`, un enum cerrado. La lista de solicitudes queda limitada a ATZ, ATE0, ATL0, ATS1, ATSP0, 0100, 0120, 010C, 010D, 0105 y 012F. El esquema JSON v1 no cambia. Valor nulo con `UNAVAILABLE` o `DISCONNECTED` es obligatorio para PIDs ausentes y lecturas fallidas.

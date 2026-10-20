# ADR-004 — Bluetooth Classic OBD-II de solo lectura

Estado: aceptado para Sprint 3.

Se usan adaptadores ELM327 Bluetooth Classic previamente emparejados mediante RFCOMM SPP y APIs Android públicas. BLE requiere un perfil de adaptador diferente y queda fuera de este incremento. La app pide BLUETOOTH_CONNECT en Android 12+; en Android 8–11 declara permisos Bluetooth heredados. No descubre dispositivos ni pide ubicación.

El núcleo conoce solo operaciones enumeradas. El adaptador Android traduce esas operaciones a bytes; no ofrece una función pública de comandos libres. La inicialización es fija y los únicos modos OBD son 01 para capacidades y cuatro métricas. Los PIDs no soportados no se consultan. La telemetría permanece en memoria local y el contrato de IA no la serializa. Cancelar o superar un timeout cierra el socket. Se limitan los reintentos y se deja el simulador disponible.

Riesgos: clones ELM327 pueden variar en eco, latencia y formato; el parser tolera eco y espacios pero rechaza tramas incompletas o malformadas. Las pruebas de fake no certifican interoperabilidad física.

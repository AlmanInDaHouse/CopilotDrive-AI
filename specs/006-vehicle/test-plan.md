# Plan de pruebas — Contexto vehículo

Fixtures JSON/roundtrip, nulls, unsupported schema, desconexión, fuente contradictoria, timestamps futuros, PID whitelist y equivalencia Python.

Tests sin API keys ni hardware por defecto. Integración física y latencia no se presuponen. Objetivo de resolución local p95 <100 ms; medir antes de afirmar. Consumo y latencia STT/TTS pendientes de dispositivo.
## Sprint 3

Fake serial ELM327: inicio fijo, descubrimiento 0100/0120, eco, respuesta compacta, NO DATA, PID ausente, trama parcial o malformada, retraso variable, timeout, desconexión, reconexión, revocación de permiso, radio ausente y cancelación. JVM no requiere coche. En Android físico, validar emparejamiento, permiso, RFCOMM y métricas sin atribuir resultados del fake al hardware.

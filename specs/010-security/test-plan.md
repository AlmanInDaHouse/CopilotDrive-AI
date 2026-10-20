# Plan de pruebas — Seguridad y privacidad

Tests hostiles de herramientas, contrato, permisos, confirmación y manifest; inspección Git de secretos.

Probar inyección de prompt, ecu.write, contactos propuestos, JSON extra o corrupto, redirect 302, TLS inválido, revocación, exclusión de token en body/logs y cancelación. Revisar backup=false y data extraction rules.

Tests sin API keys ni hardware por defecto. Integración física y latencia no se presuponen. Objetivo de resolución local p95 <100 ms; medir antes de afirmar. Consumo y latencia STT/TTS pendientes de dispositivo.

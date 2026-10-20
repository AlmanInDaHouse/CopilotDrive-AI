# Plan de pruebas — Orquestador

Tests deny-by-default, inyección, campos extra, timeouts, cancelación y resultados verificados.

FakeHTTPTransport y FakeAIProvider comprueban texto, tool call, JSON malformado/truncado, 401/429/500, TLS, cambio de configuración y ausencia de consentimiento. FakeConversationStore cubre continuidad, límites, limpieza y aislamiento. Ejecución sin claves.

Tests sin API keys ni hardware por defecto. Integración física y latencia no se presuponen. Objetivo de resolución local p95 <100 ms; medir antes de afirmar. Consumo y latencia STT/TTS pendientes de dispositivo.

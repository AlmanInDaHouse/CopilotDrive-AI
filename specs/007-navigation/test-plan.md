# Plan de pruebas — Navegación y POI

URI tests de inyección y FakeNavigationProvider; intents reales en emulador/dispositivo con apps.

Tests sin API keys ni hardware por defecto. Integración física y latencia no se presuponen. Objetivo de resolución local p95 <100 ms; medir antes de afirmar. Consumo y latencia STT/TTS pendientes de dispositivo.

## Sprint 5

Harness: denegación, consentimiento, aproximada, obsolescencia, duplicados, coordenadas inválidas, ratings ausentes, error 429, selección caducada, cambio de categoría y navegador ausente. Instrumentación: pantalla Lugares, búsqueda fake, selección y destino simulado; permiso real y API comercial quedan para laboratorio separado.

# Plan de pruebas — Voz

TestCoroutineScheduler/reloj determinista, FakeSpeechRecognizer/FakeTextToSpeech; silencio, timeout, doble inicio, cancelación, TTS fallido.

Tests sin API keys ni hardware por defecto. Integración física y latencia no se presuponen. Objetivo de resolución local p95 <100 ms; medir antes de afirmar. Consumo y latencia STT/TTS pendientes de dispositivo.

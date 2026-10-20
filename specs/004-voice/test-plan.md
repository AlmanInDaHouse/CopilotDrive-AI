# Plan de pruebas — Voz

TestCoroutineScheduler/reloj determinista, FakeSpeechRecognizer/FakeTextToSpeech; silencio, timeout, doble inicio, cancelación, TTS fallido.

Sprint 2: repetir estados TTS sin motor, idioma ausente y voz local instalada en dispositivos de laboratorio; comprobar prueba de voz, instalación oficial, cambio de voz y pantalla visual tras error. Los fakes cubren fallo y cancelación sin micrófono.

Tests sin API keys ni hardware por defecto. Integración física y latencia no se presuponen. Objetivo de resolución local p95 <100 ms; medir antes de afirmar. Consumo y latencia STT/TTS pendientes de dispositivo.

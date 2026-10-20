# ADR-003: Voz local verificable

Estado: implementado parcialmente en Sprint 2 (2026-10-20).

## Decisión

Push-to-talk sigue siendo la única activación. Se prefiere SpeechRecognizer local; el motor general requiere consentimiento independiente porque puede transmitir audio. La sesión usa timeout, cancelación, audio focus y cierre en background. No se reinicia automáticamente el reconocimiento.

TTS comprueba inicialización, disponibilidad de `es-ES` y voces locales instaladas. El selector muestra solo esas voces. Una selección que desapareció produce error visible en lugar de cambiar silenciosamente de voz. La app permite prueba de síntesis y abre la acción oficial de instalación de datos TTS. Nunca descarga una voz por sí sola. La respuesta completa se conserva en UI cuando la síntesis falla.

## Consecuencias

Un emulador sin voz `es-ES` puede mostrar respuestas sin reproducirlas. Los fakes prueban estados y fallos, pero la voz acústica, llamadas entrantes y motores OEM necesitan una matriz física. El diagnóstico actual identifica ausencia de motor/idioma/voz; no mide calidad acústica.

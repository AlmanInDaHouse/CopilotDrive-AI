# Calificación de voz Jarvis — Sprint 10

Estado físico y acústico humano: **PENDING**. Ningún teléfono disponible/autorizado.
Los resultados de tests y emulador están en SPRINT10_REPORT.md; no acreditan audibilidad.

El flujo reutiliza AndroidSpeechRecognizer → VoiceSessionCoordinator →
AssistantOrchestrator → ToolRegistry/PolicyEngine o AIProvider opcional → texto →
AndroidTextToSpeech. No se ha creado un segundo sistema de conversación. El nuevo
AcousticProbe solo reproduce una frase fija por acción explícita, usa foco, timeout
de20s y cancelación; no captura micrófono ni conserva audio. Timeout se captura antes
de CancellationException para no dejar la prueba sin resultado. Salir/background/PTT
cancela el probe; la limpieza de un intento antiguo no detiene su reemplazo.

Device Lab muestra etapas cerradas VoiceJourneyStage, emitidas por el coordinador y
ToolRegistry. Policy-authorized indica que la política permitió intentar la herramienta;
no certifica resultado de reproductor, navegación o marcador. El historial es acotado a32
etapas en RAM y borrable. No texto del proveedor, argumentos, transcripción o razonamiento
privado en esa vista. La respuesta normal sigue accesible en dashboard ante fallo TTS.

## Matriz preparada para un teléfono consentido

| Área | Procedimiento y registro | Estado físico |
| --- | --- | --- |
| STT es-ES | Permiso al pulsar; motor/localidad y coincidencia de frase sintética; jamás guardar audio/transcripción | PENDING |
| STT local y fallback | Ver capacidad real; sistema solo con consentimiento. Ausencia = UNSUPPORTED en ese dispositivo, no fallo general | PENDING |
| Silencio/vacío/timeout | Esperar respuesta recuperable, micrófono liberado, reintentar explícitamente | PENDING |
| Cancelación/doble petición | Cancelar escuchando; dos solicitudes consecutivas sin duplicar sesión ni tool | PENDING |
| Foco/background | Interrupción autorizada y HOME/retorno: cancelar, no reescuchar automáticamente; solicitar de nuevo | PENDING |
| TTS catálogo | Motor instalado, voz local es-ES disponible/seleccionada; guardar identificador del motor/versión, no identificador personal | PENDING |
| Síntesis | Frase fija escrita y estado técnico SPEAKING/COMPLETED/FAILED/CANCELLED | PENDING |
| Acústica | Después del callback, respuesta humana independiente: completa/correcta sí o no. Acta de sesión/build/intento | PENDING |
| Fallo/interrupción TTS | Cancelar o interrumpir; comprobar texto conservado y siguiente intento. Sin voz local = respuesta textual utilizable | PENDING |

Frase guiada: «Prueba de voz en español. Jarvis está listo para ayudarte.»
La confirmación solo se acepta para el intento actual completado y una vez; reintento
invalida la anterior. Callback sin observador deja HumanOutcome.PENDING. «No» registra
NOT_OBSERVED, nunca una verificación positiva. El UI no emite DeviceQualification;
VoiceQualificationResult exige acta/manual/entorno físico, commit/hash y referencia
coincidentes, timestamp válido y observación positiva del mismo intento para verificar.
La lista de preparación no admite calificaciones físicas sin sus registros observados.

Regresiones deterministas: callback independiente de humano, confirmación obsoleta,
timeout, permiso TTS, cancelación/cleanup tardío, foco y reintento, privacidad de etapas,
rechazo/autorización de política y consulta local con proveedor remoto fake indisponible.
PhysicalPilotUiTest usa estado sintético; no abre micrófono ni confirma acústica real.

En la imagen AOSP API26 se observaron crashes de `com.svox.pico`/libttspico durante
consultas del baseline y de regresión. Texto local y suite funcional siguieron pasando;
no se declara TTS real aprobado en esa imagen. Sin evidencia para reparar una librería
del sistema desde la app; registrar la limitación y probar un motor local real consentido.
En las imágenes sin servicio de voz, catálogo ausente no se convierte en prueba acústica.

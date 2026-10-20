package ai.copilotdrive.app.platform

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import ai.copilotdrive.core.AudioFocusCoordinator
import ai.copilotdrive.core.SpeechToTextProvider
import ai.copilotdrive.core.TextToSpeechProvider
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AndroidSpeechRecognizer(private val context: Context, private val allowSystem: () -> Boolean,
    private val onReady: (Long) -> Unit = {}) : SpeechToTextProvider {
    private var recognizer: SpeechRecognizer? = null
    fun localAvailable() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    fun available() = localAvailable() || SpeechRecognizer.isRecognitionAvailable(context)

    override suspend fun recognize(onTranscribing: () -> Unit): String = withContext(Dispatchers.Main.immediate) {
        val startedNs = System.nanoTime()
        val local = localAvailable()
        if (!local && !allowSystem()) throw SecurityException("System recognition requires consent")
        check(available())
        cancel()
        try { suspendCancellableCoroutine { continuation ->
            val active = if (local) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = active
            continuation.invokeOnCancellation { active.cancel(); active.destroy(); if (recognizer === active) recognizer = null }
            active.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (continuation.isActive) onReady(((System.nanoTime() - startedNs) / 1_000_000).coerceAtLeast(0))
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (continuation.isActive) onTranscribing() }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onError(error: Int) {
                    if (continuation.isActive) {
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) continuation.resumeWithException(SecurityException("microphone"))
                        else continuation.resumeWithException(IllegalStateException(when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "STT_NO_MATCH"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "STT_SILENCE"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "STT_NETWORK_TIMEOUT"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "STT_BUSY"
                            SpeechRecognizer.ERROR_CLIENT -> "STT_INTERRUPTED"
                            else -> "STT_ERROR_$error"
                        }))
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (continuation.isActive) continuation.resume(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
                }
            })
            active.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            })
        } } finally { cancel() }
    }
    override fun cancel() { recognizer?.cancel(); recognizer?.destroy(); recognizer = null }
}

data class InstalledVoice(val name: String, val label: String)

class AndroidTextToSpeech(context: Context) : TextToSpeechProvider {
    private val ready = CompletableDeferred<Boolean>()
    private val engine = TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }
    val voices = MutableStateFlow<List<InstalledVoice>>(emptyList())
    val status = MutableStateFlow("Inicializando motor TTS…")
    private var pending: Pair<String, CancellableContinuation<Unit>>? = null
    var selectedVoice: String = ""
    var rate = 1f
    var pitch = 1f

    suspend fun initialize() = withContext(Dispatchers.Main.immediate) {
        if (!ready.await()) { status.value = "Motor TTS ausente o falló la inicialización."; return@withContext }
        val language = engine.isLanguageAvailable(Locale.forLanguageTag("es-ES"))
        if (language < TextToSpeech.LANG_AVAILABLE) { status.value = "No hay datos de voz para es-ES. Instálalos desde Android."; return@withContext }
        voices.value = engine.voices.orEmpty().filter { it.locale.toLanguageTag().equals("es-ES", true) && !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
            .sortedBy { it.name }.map { InstalledVoice(it.name, "${it.locale.displayName} · ${it.name}") }
        status.value = if (voices.value.isEmpty()) "No hay voz local es-ES instalada." else "${voices.value.size} voz(es) local(es) es-ES disponible(s)."
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { finish(utteranceId, null) }
            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) { finish(utteranceId, IllegalStateException("TTS unavailable")) }
            override fun onError(utteranceId: String?, errorCode: Int) { finish(utteranceId, IllegalStateException("TTS_ERROR_$errorCode")) }
        })
    }

    @Synchronized private fun finish(id: String?, error: Exception?) {
        val pair = pending ?: return
        if (pair.first != id) return
        pending = null
        if (pair.second.isActive) {
            if (error == null) pair.second.resume(Unit) else pair.second.resumeWithException(error)
        }
    }

    override suspend fun speak(text: String) = withContext(Dispatchers.Main.immediate) {
        check(ready.await())
        val localVoices = engine.voices.orEmpty().filter { it.locale.toLanguageTag().equals("es-ES", true) && !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
        val voice = (if (selectedVoice.isBlank()) localVoices.firstOrNull() else localVoices.firstOrNull { it.name == selectedVoice })
            ?: error("No hay voz local es-ES seleccionada")
        check(engine.setVoice(voice) == TextToSpeech.SUCCESS)
        engine.setSpeechRate(rate.coerceIn(0.7f, 1.3f)); engine.setPitch(pitch.coerceIn(0.7f, 1.3f))
        suspendCancellableCoroutine<Unit> { continuation ->
            val id = UUID.randomUUID().toString()
            synchronized(this@AndroidTextToSpeech) { pending?.second?.cancel(); pending = id to continuation }
            continuation.invokeOnCancellation { engine.stop(); synchronized(this@AndroidTextToSpeech) { if (pending?.first == id) pending = null } }
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) finish(id, IllegalStateException("TTS failed"))
        }
    }

    override fun stop() {
        engine.stop()
        val previous = synchronized(this) { pending.also { pending = null } }
        previous?.second?.cancel()
    }
    fun close() { stop(); engine.shutdown() }
}

class AndroidAudioFocus(context: Context, private val onLoss: () -> Unit) : AudioFocusCoordinator {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setOnAudioFocusChangeListener { change -> if (change < 0) onLoss() }
        .build()
    override fun acquire() = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    override fun release() { manager.abandonAudioFocusRequest(request) }
}

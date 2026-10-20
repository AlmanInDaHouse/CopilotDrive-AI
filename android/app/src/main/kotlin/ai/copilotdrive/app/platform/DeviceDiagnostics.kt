package ai.copilotdrive.app.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import ai.copilotdrive.core.DiagnosticCheck
import ai.copilotdrive.core.DiagnosticStatus
import ai.copilotdrive.core.EvidenceLevel
import ai.copilotdrive.core.ProviderConfiguration

/** Passive probes only. They do not open a microphone, socket, or network connection. */
class DeviceDiagnostics(
    private val context: Context,
    private val bluetooth: AndroidBluetoothObd,
    private val stt: AndroidSpeechRecognizer,
    private val tts: AndroidTextToSpeech,
) {
    private fun check(id: String, component: String, ok: Boolean, description: String,
        correction: String? = null, unsupported: Boolean = false, started: Long = SystemClock.elapsedRealtime()): DiagnosticCheck =
        DiagnosticCheck(id, component, if (ok) DiagnosticStatus.PASS else if (unsupported) DiagnosticStatus.UNSUPPORTED else DiagnosticStatus.PENDING,
            EvidenceLevel.PENDING, System.currentTimeMillis(),
            (SystemClock.elapsedRealtime() - started).coerceAtLeast(0), if (ok) null else id.uppercase().replace('.', '_'),
            description, if (ok) null else correction)

    fun local(config: ProviderConfiguration, credentialSaved: Boolean, source: String, capabilities: Set<String>): List<DiagnosticCheck> {
        val model = Build.MODEL.orEmpty().take(80)
        val mic = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val micPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val bluetoothPresent = bluetooth.present()
        val bluetoothPermission = bluetooth.permitted()
        val ttsEngine = context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).isNotEmpty()
        return listOf(
            check("device.identity", "android", true, "Android ${Build.VERSION.RELEASE}; API ${Build.VERSION.SDK_INT}; modelo $model. Identificación informativa."),
            check("bluetooth.hardware", "bluetooth", bluetoothPresent, if (bluetoothPresent) "Radio Bluetooth detectada; funcionamiento sin verificar." else "Sin radio Bluetooth.", unsupported = true),
            check("bluetooth.permission", "bluetooth", bluetoothPermission, if (bluetoothPermission) "Permiso Bluetooth concedido o no requerido por esta API." else "Falta permiso de dispositivos cercanos.", "Concede permiso Bluetooth en Ajustes."),
            check("bluetooth.enabled", "bluetooth", bluetooth.available(), if (bluetooth.available()) "Bluetooth encendido; enlace sin verificar." else "Bluetooth apagado, sin permiso o no disponible.", "Activa Bluetooth y concede permiso."),
            check("microphone.hardware", "voice", mic, if (mic) "Micrófono declarado; captura sin verificar." else "Sin micrófono declarado.", unsupported = true),
            check("microphone.permission", "voice", micPermission, if (micPermission) "Permiso de micrófono concedido; captura sin verificar." else "Permiso de micrófono denegado.", "Concede permiso solo para una prueba iniciada por ti."),
            check("stt.local", "voice", stt.localAvailable(), "Reconocimiento local ${if (stt.localAvailable()) "disponible" else "no disponible"}; precisión sin verificar."),
            check("stt.system", "voice", SpeechRecognizer.isRecognitionAvailable(context), "Servicio STT del sistema ${if (SpeechRecognizer.isRecognitionAvailable(context)) "disponible" else "ausente"}; captura sin verificar."),
            check("tts.engine", "voice", ttsEngine, if (ttsEngine) "Motor TTS instalado; salida acústica sin verificar." else "No se encontró motor TTS.", "Instala o configura un motor TTS.", unsupported = true),
            check("tts.es_es", "voice", tts.voices.value.isNotEmpty(), "Voces locales es-ES: ${tts.voices.value.size}. ${tts.status.value}", "Instala datos de voz es-ES desde Android."),
            check("ai.configuration", "ai", config.endpoint.isNotBlank() && config.model.isNotBlank(),
                "Proveedor ${if (config.endpoint.isNotBlank() && config.model.isNotBlank()) "configurado" else "sin configurar"}; conectividad sin verificar.", "Configura endpoint y modelo."),
            check("ai.consent", "ai", config.enabled && config.consented, if (config.enabled && config.consented) "Consentimiento para texto activo; contexto vehicular excluido." else "Consulta externa desactivada o sin consentimiento."),
            check("ai.credential", "ai", credentialSaved, if (credentialSaved) "Credencial presente; validez sin verificar." else "Sin credencial guardada.", "Guarda una credencial si el servidor la requiere."),
            check("vehicle.source", "vehicle", source == "SIMULATED", "Fuente $source; lectura física sin verificar. Capacidades: ${capabilities.sorted().joinToString().ifBlank { "ninguna" }}."),
        )
    }

    fun bluetoothPairing(): DiagnosticCheck {
        val start = SystemClock.elapsedRealtime()
        return try {
            val devices = bluetooth.pairedDevices()
            check("bluetooth.paired", "bluetooth", devices.isNotEmpty(), "Dispositivos emparejados: ${devices.size}; compatibilidad ELM sin verificar.",
                "Empareja un ELM327 Classic en Ajustes de Android.", started = start)
        } catch (_: SecurityException) {
            check("bluetooth.paired", "bluetooth", false, "No se pudo consultar emparejamiento por permiso.",
                "Concede permiso Bluetooth.", started = start)
        }
    }
}

package ai.copilotdrive.app.platform

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ai.copilotdrive.core.ProviderConfiguration
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("copilot_preferences")
data class VoicePreferences(val onboarded: Boolean = false, val allowSystemRecognition: Boolean = false, val voice: String = "", val rate: Float = 1f, val pitch: Float = 1f, val ai: ProviderConfiguration = ProviderConfiguration())
class Preferences(context: Context) {
    private val store = context.dataStore
    private val onboarded = booleanPreferencesKey("onboarded")
    private val consent = booleanPreferencesKey("allow_system_recognition")
    private val voice = stringPreferencesKey("voice")
    private val rate = floatPreferencesKey("rate")
    private val pitch = floatPreferencesKey("pitch")
    private val aiEnabled = booleanPreferencesKey("ai_enabled")
    private val aiEndpoint = stringPreferencesKey("ai_endpoint")
    private val aiModel = stringPreferencesKey("ai_model")
    private val aiTimeout = androidx.datastore.preferences.core.intPreferencesKey("ai_timeout")
    private val aiConsent = booleanPreferencesKey("ai_consent")
    private val aiContext = booleanPreferencesKey("ai_context")
    private val aiTools = booleanPreferencesKey("ai_tools")
    private val aiProviderName = stringPreferencesKey("ai_provider_name")
    val state = store.data.map {
        VoicePreferences(it[onboarded] ?: false, it[consent] ?: false, it[voice].orEmpty(), it[rate] ?: 1f, it[pitch] ?: 1f,
            ProviderConfiguration(it[aiEnabled] ?: false, it[aiEndpoint].orEmpty(), it[aiModel].orEmpty(),
                it[aiTimeout] ?: 10_000, it[aiConsent] ?: false, it[aiContext] ?: false, it[aiTools] ?: false, it[aiProviderName].orEmpty()))
    }
    suspend fun save(value: VoicePreferences) { store.edit {
        it[onboarded] = value.onboarded; it[consent] = value.allowSystemRecognition; it[voice] = value.voice; it[rate] = value.rate; it[pitch] = value.pitch
        it[aiEnabled] = value.ai.enabled; it[aiEndpoint] = value.ai.endpoint; it[aiModel] = value.ai.model
        it[aiTimeout] = value.ai.timeoutMs; it[aiConsent] = value.ai.consented; it[aiContext] = value.ai.shareVehicleContext; it[aiTools] = value.ai.toolCalling
        it[aiProviderName] = value.ai.providerName
    } }
}

package ai.copilotdrive.app.platform

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("copilot_preferences")
data class VoicePreferences(val onboarded: Boolean = false, val allowSystemRecognition: Boolean = false, val voice: String = "", val rate: Float = 1f, val pitch: Float = 1f)
class Preferences(context: Context) {
    private val store = context.dataStore
    private val onboarded = booleanPreferencesKey("onboarded")
    private val consent = booleanPreferencesKey("allow_system_recognition")
    private val voice = stringPreferencesKey("voice")
    private val rate = floatPreferencesKey("rate")
    private val pitch = floatPreferencesKey("pitch")
    val state = store.data.map { VoicePreferences(it[onboarded] ?: false, it[consent] ?: false, it[voice].orEmpty(), it[rate] ?: 1f, it[pitch] ?: 1f) }
    suspend fun save(value: VoicePreferences) { store.edit { it[onboarded] = value.onboarded; it[consent] = value.allowSystemRecognition; it[voice] = value.voice; it[rate] = value.rate; it[pitch] = value.pitch } }
}

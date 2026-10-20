package ai.copilotdrive.app.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences as StoredPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ai.copilotdrive.core.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val recoveredKey = booleanPreferencesKey("layout_file_recovered")
private val Context.layoutStore by preferencesDataStore("dashboard_layout",
    corruptionHandler = ReplaceFileCorruptionHandler {
        androidx.datastore.preferences.core.mutablePreferencesOf(recoveredKey to true)
    })

class DataStoreLayoutRepository(private val store: DataStore<StoredPreferences>) : LayoutRepository {
    constructor(context: Context) : this(context.applicationContext.layoutStore)
    private val key = stringPreferencesKey("layout_json")
    override val state = store.data.map { prefs ->
        val result = LayoutCodec.decode(prefs[key])
        if (prefs[recoveredKey] == true) result.copy(notice = "Archivo de diseño corrupto; restaurado Classic") else result
    }.catch { error ->
        if (error is IOException) emit(LayoutLoadResult(WidgetLayouts.preset(DashboardPreset.CLASSIC),
            "No se pudo leer el diseño; Classic temporal. Puedes reintentar guardando.")) else throw error
    }
    override suspend fun save(layout: DashboardLayout) {
        val encoded = LayoutCodec.encode(layout)
        store.edit { it[key] = encoded; it.remove(recoveredKey) }
    }
}

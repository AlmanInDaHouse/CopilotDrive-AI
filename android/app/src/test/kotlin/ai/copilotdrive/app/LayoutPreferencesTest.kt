package ai.copilotdrive.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ai.copilotdrive.app.platform.DataStoreLayoutRepository
import ai.copilotdrive.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class LayoutPreferencesTest {
    @Test fun datastoreRoundTripCorruptJsonMigrationAndProcessReopen() = runBlocking {
        val directory = Files.createTempDirectory("layout-test").toFile()
        val file = File(directory, "layout.preferences_pb")
        var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        var store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        var repository = DataStoreLayoutRepository(store)
        assertEquals(DashboardPreset.CLASSIC, repository.state.first().layout.preset)
        val media = WidgetLayouts.preset(DashboardPreset.MEDIA)
        repository.save(media)
        assertEquals(media, repository.state.first().layout)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        repository = DataStoreLayoutRepository(store)
        assertEquals(media, repository.state.first().layout)
        val key = stringPreferencesKey("layout_json")
        store.edit { it[key] = "{" }
        assertNotNull(repository.state.first().notice)
        assertEquals(DashboardPreset.CLASSIC, repository.state.first().layout.preset)
        store.edit { it[key] = LayoutCodec.encode(media).replace("\"schemaVersion\":1", "\"schemaVersion\":0") }
        assertEquals(media, repository.state.first().layout)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }
    @Test fun datastoreReadAndWriteErrorsAreExplicit() = runBlocking {
        val broken = object : androidx.datastore.core.DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("read") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw IOException("write")
        }
        val repository = DataStoreLayoutRepository(broken)
        assertNotNull(repository.state.first().notice)
        try { repository.save(WidgetLayouts.preset(DashboardPreset.CLASSIC)); fail("must throw") } catch (_: IOException) { }
    }
    @Test fun datastoreCancellationIsNotRecovery() = runBlocking {
        val cancelled = object : androidx.datastore.core.DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw CancellationException() }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw CancellationException()
        }
        try { DataStoreLayoutRepository(cancelled).state.first(); fail("must cancel") } catch (_: CancellationException) { }
    }
}

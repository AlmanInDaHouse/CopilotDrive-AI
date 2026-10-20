package ai.copilotdrive.app

import ai.copilotdrive.app.platform.AndroidTextToSpeech
import ai.copilotdrive.app.platform.AndroidHTTPTransport
import ai.copilotdrive.app.platform.KeystoreSecretStore
import ai.copilotdrive.core.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class Sprint2PlatformTest {
    @Test fun credentialIsEncryptedInPrivateStorageAndCanBeRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val secrets = KeystoreSecretStore(context)
        val sample = "test-only-token-value"
        try {
            secrets.put(sample)
            assertEquals(sample, secrets.get())
            val stored = context.getSharedPreferences("provider_secret", 0).all.values.joinToString()
            assertFalse(stored.contains(sample))
        } finally { secrets.clear() }
        assertNull(secrets.get())
    }

    @Test fun voiceCatalogReportsMissingOrInstalledSpanishVoiceWithoutInventingOne() = runBlocking {
        val tts = AndroidTextToSpeech(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            tts.initialize()
            println("TTS diagnostic: ${tts.status.value}; local es-ES voices=${tts.voices.value.size}")
            if (tts.voices.value.isEmpty()) assertTrue(tts.status.value.contains("No hay") || tts.status.value.contains("ausente"))
            else assertTrue(tts.status.value.contains("disponible"))
        } finally { tts.close() }
    }

    @Test fun localDebugChatCompletionsTransportReturnsTextWithoutCredential() = runBlocking {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 5_000
        var requestLine = ""
        val worker = thread {
            server.use { listener -> listener.accept().use { socket ->
                socket.soTimeout = 5_000
                val reader = socket.getInputStream().bufferedReader()
                requestLine = reader.readLine()
                while (true) {
                    val line = reader.readLine()
                    if (line.isNullOrEmpty()) break
                }
                val response = """{"choices":[{"finish_reason":"stop","message":{"content":"Respuesta de prueba local."}}]}"""
                val bytes = response.toByteArray()
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                socket.getOutputStream().write(bytes)
                socket.getOutputStream().flush()
            } }
        }
        val provider = OpenAICompatibleProvider(
            { ProviderConfiguration(enabled = true, endpoint = "http://127.0.0.1:${server.localPort}/v1", model = "mock", consented = true) },
            { null }, AndroidHTTPTransport(), allowLocalHttp = true)
        val result = provider.generate(AIRequest("Explícame CVT"))
        worker.join(6_000)
        assertTrue(result is AIResponse.Text)
        assertEquals("/v1/chat/completions", requestLine.substringAfter("POST ").substringBefore(' '))
    }
}

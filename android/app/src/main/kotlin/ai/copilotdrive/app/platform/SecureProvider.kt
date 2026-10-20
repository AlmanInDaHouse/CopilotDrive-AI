package ai.copilotdrive.app.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ai.copilotdrive.core.HTTPResponse
import ai.copilotdrive.core.HTTPTransport
import ai.copilotdrive.core.SecretStore
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class KeystoreSecretStore(context: Context) : SecretStore {
    private val prefs = context.getSharedPreferences("provider_secret", Context.MODE_PRIVATE)
    private val alias = "copilot_provider_token_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    override fun get(): String? = try {
        val iv = prefs.getString("iv", null) ?: return null
        val data = prefs.getString("data", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.getDecoder().decode(iv)))
        String(cipher.doFinal(Base64.getDecoder().decode(data)), Charsets.UTF_8)
    } catch (_: Exception) { null }
    override fun put(secret: String) {
        require(secret.length in 1..4_096)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encoded = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("iv", Base64.getEncoder().encodeToString(cipher.iv))
            .putString("data", Base64.getEncoder().encodeToString(encoded)).commit())
    }
    override fun clear() { prefs.edit().remove("iv").remove("data").apply() }
}

/** Redirects are disabled so Authorization can never be forwarded to another origin. */
class AndroidHTTPTransport : HTTPTransport {
    override suspend fun post(url: String, token: String, body: String, timeoutMs: Int): HTTPResponse = suspendCancellableCoroutine { continuation ->
        val connection = AtomicReference<HttpURLConnection?>()
        val worker = thread(name = "copilot-provider", start = false) {
            try {
                if (!continuation.isActive) return@thread
                val active = (URL(url).openConnection() as HttpURLConnection).also { connection.set(it) }
                if (!continuation.isActive) { active.disconnect(); return@thread }
                active.instanceFollowRedirects = false
                active.connectTimeout = timeoutMs
                active.readTimeout = timeoutMs
                active.requestMethod = "POST"
                active.doOutput = true
                active.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (token.isNotBlank()) active.setRequestProperty("Authorization", "Bearer $token")
                active.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = active.responseCode
                val stream = if (status in 200..299) active.inputStream else active.errorStream
                val bytes = stream?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4_096)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > 65_536) throw IllegalStateException("response limit")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: ByteArray(0)
                if (continuation.isActive) continuation.resume(HTTPResponse(status, String(bytes, Charsets.UTF_8)))
            } catch (failure: Exception) {
                if (continuation.isActive) continuation.resumeWithException(failure)
            } finally { connection.get()?.disconnect() }
        }
        continuation.invokeOnCancellation { connection.get()?.disconnect(); worker.interrupt() }
        worker.start()
    }
}

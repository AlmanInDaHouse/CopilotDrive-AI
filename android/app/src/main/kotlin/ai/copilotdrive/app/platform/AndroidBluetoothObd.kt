package ai.copilotdrive.app.platform

import ai.copilotdrive.core.BluetoothTransport
import ai.copilotdrive.core.ObdWireRequest
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PairedObdDevice(val address: String, val name: String)

/** Android public SPP API. All blocking calls run on one private worker. Closing the socket unblocks cancellation. */
class AndroidBluetoothObd(private val context: Context) : BluetoothTransport {
    private val adapter: BluetoothAdapter? get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun available(): Boolean = permitted() && adapter?.isEnabled == true

    fun present(): Boolean = adapter != null

    fun permitted(): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<PairedObdDevice> {
        if (!permitted()) throw SecurityException("Bluetooth permission required")
        return adapter?.bondedDevices?.map { PairedObdDevice(it.address, it.name ?: "Dispositivo sin nombre") }
            ?.sortedBy { it.name } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    override suspend fun open(deviceAddress: String) = blocking {
        if (!permitted()) throw SecurityException("Bluetooth permission required")
        val radio = adapter ?: throw IOException("Bluetooth no disponible")
        if (!radio.isEnabled) throw IOException("Bluetooth apagado")
        close()
        val device = radio.getRemoteDevice(deviceAddress)
        if (device.bondState != android.bluetooth.BluetoothDevice.BOND_BONDED) throw IOException("Adaptador no emparejado")
        val candidate = device.createRfcommSocketToServiceRecord(SPP_UUID)
        socket = candidate
        candidate.connect()
    }

    override suspend fun exchange(request: ObdWireRequest): String = blocking {
        if (!permitted()) throw SecurityException("Bluetooth permission revoked")
        val channel = socket ?: throw IOException("OBD desconectado")
        channel.outputStream.write((request.wire + "\r").toByteArray(Charsets.US_ASCII))
        channel.outputStream.flush()
        val response = StringBuilder()
        while (response.length < 512) {
            val value = channel.inputStream.read()
            if (value < 0) throw IOException("OBD desconectado")
            if (value == '>'.code) return@blocking response.toString()
            response.append(value.toChar())
        }
        throw IOException("Respuesta ELM demasiado larga")
    }

    override fun close() {
        val old = socket
        socket = null
        try { old?.close() } catch (_: IOException) { }
    }

    fun dispose() { close(); worker.shutdownNow() }

    private suspend fun <T> blocking(action: () -> T): T = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { close() }
        worker.execute {
            try {
                val value = action()
                if (continuation.isActive) continuation.resume(value)
            } catch (failure: Exception) {
                if (continuation.isActive) continuation.resumeWithException(failure)
            }
        }
    }

    companion object { private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") }
}

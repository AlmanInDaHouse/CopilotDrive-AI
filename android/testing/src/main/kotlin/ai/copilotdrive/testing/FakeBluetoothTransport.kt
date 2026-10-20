package ai.copilotdrive.testing

import ai.copilotdrive.core.BluetoothTransport
import ai.copilotdrive.core.ObdWireRequest
import kotlinx.coroutines.delay
import java.io.IOException

/** Serial ELM fixture: command order, latency and disconnection are deterministic. */
class FakeBluetoothTransport : BluetoothTransport {
    val sent = mutableListOf<ObdWireRequest>()
    val replies = mutableMapOf<ObdWireRequest, String>()
    var latencyMs = 0L
    var connectLatencyMs = 0L
    var openFailure: Exception? = null
    var exchangeFailure: Exception? = null
    var permissionGranted = true
    var radioPresent = true
    var connected = false
    var opens = 0
    var closes = 0

    init {
        replies[ObdWireRequest.CAPABILITIES_00] = "41 00 08 18 00 01 >"
        replies[ObdWireRequest.CAPABILITIES_20] = "41 20 00 02 00 00 >"
        replies[ObdWireRequest.RPM] = "41 0C 1A F8 >"
        replies[ObdWireRequest.SPEED] = "41 0D 28 >"
        replies[ObdWireRequest.COOLANT] = "41 05 80 >"
        replies[ObdWireRequest.FUEL] = "41 2F 80 >"
    }

    override suspend fun open(deviceAddress: String) {
        opens++
        delay(connectLatencyMs)
        if (!radioPresent) throw IOException("Bluetooth absent")
        if (!permissionGranted) throw SecurityException("Permission revoked")
        openFailure?.let { throw it }
        connected = true
    }

    override suspend fun exchange(request: ObdWireRequest): String {
        if (!connected) throw IOException("Disconnected")
        sent += request
        delay(latencyMs)
        if (!permissionGranted) throw SecurityException("Permission revoked")
        exchangeFailure?.let { throw it }
        return replies[request] ?: "OK >"
    }

    override fun close() { closes++; connected = false }
}

package ai.copilotdrive.core

import kotlin.math.round

enum class ReadPid(val command: String, val bytes: Int) {
    RPM("010C", 2), SPEED("010D", 1), COOLANT("0105", 1), FUEL("012F", 1),
}

/** Parser only. No arbitrary command transport or ECU write operation exists. */
object ObdDecoder {
    fun decode(pid: ReadPid, response: String): Double? {
        val raw = response.uppercase().replace(Regex("\\s+"), "")
        val match = Regex("41${pid.command.drop(2)}([0-9A-F]{${pid.bytes * 2}})(?![0-9A-F])").find(raw) ?: return null
        val bytes = match.groupValues[1].chunked(2).map { it.toInt(16) }
        return when (pid) {
            ReadPid.RPM -> round((bytes[0] * 256 + bytes[1]) * 2.5) / 10.0
            ReadPid.SPEED -> bytes[0].toDouble()
            ReadPid.COOLANT -> bytes[0] - 40.0
            ReadPid.FUEL -> round(bytes[0] * 1000.0 / 255) / 10.0
        }
    }
}

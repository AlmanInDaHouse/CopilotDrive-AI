package ai.copilotdrive.core

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SharedFixturesTest {
    @Test fun `shared version one fixture decodes without loss`() {
        val text = javaClass.getResource("/vehicle-simulated-v1.json")!!.readText()
        val snapshot = ContractJson.decodeVehicle(text)
        assertEquals(42.0, snapshot.fuelLevelPercent.value!!, 0.0)
        assertTrue(snapshot.simulationMode)
        assertNull(snapshot.vehicleRangeKm.value)
    }
    @Test fun `OBD fixture matches Python decoder`() {
        val fixture = Json.parseToJsonElement(javaClass.getResource("/obd-read-v1.json")!!.readText()).jsonArray
        fixture.forEach { row ->
            val data = row.jsonObject
            val pid = ReadPid.entries.single { it.command == data.getValue("pid").jsonPrimitive.content }
            val actual = ObdDecoder.decode(pid, data.getValue("response").jsonPrimitive.content)
            val expected = data.getValue("value").jsonPrimitive.doubleOrNull
            assertEquals(expected, actual)
        }
    }
}

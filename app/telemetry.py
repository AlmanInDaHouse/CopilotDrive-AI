"""Telemetry sources. Only mode-01 OBD read requests are allowed."""
from __future__ import annotations

import math
import os
import re
import threading
import time
from typing import Any

# Standard read-only OBD-II service 01 PIDs, never ECU writes / Mode 04.
PIDS = {
    "rpm": ("010C", 2),
    "speed_kmh": ("010D", 1),
    "coolant_c": ("0105", 1),
    "fuel_percent": ("012F", 1),
}


def alerts(snapshot: dict[str, Any], max_coolant_c: float = 110.0) -> list[str]:
    """Advisory threshold only; not a substitute for vehicle warnings."""
    if snapshot.get("status") != "connected":
        return []
    temp = snapshot.get("coolant_c")
    if isinstance(temp, (int, float)) and temp >= max_coolant_c:
        return [f"Refrigerante a {temp:.0f} °C: se ha superado el umbral configurado ({max_coolant_c:.0f} °C)."]
    return []


class Simulator:
    def snapshot(self) -> dict[str, Any]:
        t = time.monotonic()
        return {
            "source": "simulator",
            "status": "connected",
            "speed_kmh": round(55 + 18 * math.sin(t / 11), 1),
            "rpm": round(2100 + 600 * math.sin(t / 9)),
            "coolant_c": round(89 + 3 * math.sin(t / 23), 1),
            "fuel_percent": 62.0,
            "battery_voltage": 13.9,
            "updated_at": int(time.time()),
        }


class Elm327Serial:
    """Supports serial ELM327 (USB or paired Bluetooth RFCOMM serial).

    No arbitrary command interface exists. Diagnostic read-only requests ONLY.
    Unsupported sensors return null. Not a certified automotive interface.
    """

    def __init__(self, port: str, baudrate: int = 38400) -> None:
        if not port:
            raise ValueError("OBD_PORT debe especificarse")
        self.port = port
        self.baudrate = baudrate
        self._serial: Any = None
        self._lock = threading.Lock()

    def _connect(self) -> None:
        import serial  # optional dependency (pyserial)
        self._serial = serial.Serial(self.port, baudrate=self.baudrate, timeout=1.2)
        for setup in ("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0"):
            self._send(setup)

    def _send(self, command: str) -> str:
        assert self._serial is not None
        self._serial.reset_input_buffer()
        self._serial.write((command + "\r").encode("ascii"))
        self._serial.flush()
        response = self._serial.read_until(b">", 512).decode("ascii", errors="ignore")
        return response.upper()

    @staticmethod
    def decode(pid: str, response: str) -> float | None:
        # Strip whitespace, echoed request, and adapters' formatting.
        raw = re.sub(r"\s+", "", response.upper())
        token = "41" + pid[2:]
        m = re.search(re.escape(token) + r"([0-9A-F]{2})([0-9A-F]{2})?", raw)
        if not m:
            return None
        a = int(m.group(1), 16)
        b = int(m.group(2), 16) if m.group(2) is not None else 0
        if pid == "010C":
            if m.group(2) is None:
                return None
            return round((256 * a + b) / 4, 1)
        if pid == "010D":
            return float(a)
        if pid == "0105":
            return float(a - 40)
        if pid == "012F":
            return round(100 * a / 255, 1)
        return None

    def snapshot(self) -> dict[str, Any]:
        result: dict[str, Any] = {
            "source": "obd-elm327", "status": "disconnected", "speed_kmh": None,
            "rpm": None, "coolant_c": None, "fuel_percent": None,
            "battery_voltage": None, "updated_at": int(time.time()),
        }
        with self._lock:
            try:
                if self._serial is None or not self._serial.is_open:
                    self._connect()
                for key, (command, _) in PIDS.items():
                    result[key] = self.decode(command, self._send(command))
                voltage_raw = self._send("ATRV")  # Voltage reported by adapter
                voltage_match = re.search(r"(\d+(?:\.\d+)?)\s*V", voltage_raw)
                if voltage_match:
                    result["battery_voltage"] = float(voltage_match.group(1))
                result["status"] = "connected"
            except (Exception) as exc:
                result["error"] = "No disponible: verifica puerto, permisos y adaptador"
                if self._serial is not None:
                    try:
                        self._serial.close()
                    except Exception:
                        pass
                self._serial = None
        return result


def build_source():
    if os.getenv("TELEMETRY_MODE", "simulator").lower() == "obd":
        return Elm327Serial(os.getenv("OBD_PORT", ""), int(os.getenv("OBD_BAUDRATE", "38400")))
    return Simulator()

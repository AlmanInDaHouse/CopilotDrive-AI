"""A controlled assistant. The model never has direct access to car actuators."""
from __future__ import annotations

import json
import os
import re
import unicodedata
from urllib import request

from app.telemetry import alerts


def normalize(text: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFKD", text.lower()) if not unicodedata.combining(c))


def value(snapshot: dict, key: str, unit: str) -> str:
    datum = snapshot.get(key)
    if not isinstance(datum, (int, float)):
        return "no disponible"
    return f"{datum:g} {unit}".strip()


def model_answer(prompt: str, snapshot: dict) -> str | None:
    """Optional OpenAI-compatible API; disabled unless explicitly configured."""
    base = os.getenv("AI_API_BASE", "").rstrip("/")
    model = os.getenv("AI_MODEL", "")
    if not base or not model:
        return None
    context = {k: v for k, v in snapshot.items() if k in (
        "source", "status", "speed_kmh", "rpm", "coolant_c", "fuel_percent", "battery_voltage"
    )}
    payload = json.dumps({
        "model": model, "stream": False, "temperature": 0.2,
        "messages": [
            {"role": "system", "content": (
                "Eres un copiloto para consultas informativas en español. Los datos de coche "+
                "son solo telemetría orientativa; las ausencias son desconocidas. "+
                "No hagas diagnósticos concluyentes. No tienes permiso ni capacidad "+
                "para controlar motor, frenos, dirección, puertas, clima ni otros actuadores. "+
                "No afirmes haber ejecutado acciones. Sé breve y evita distraer al conductor. "+
                "Datos actuales JSON: " + json.dumps(context, ensure_ascii=False))},
            {"role": "user", "content": prompt},
        ],
    }, ensure_ascii=False).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if os.getenv("AI_API_KEY"):
        headers["Authorization"] = "Bearer " + os.environ["AI_API_KEY"]
    req = request.Request(base + "/chat/completions", data=payload, headers=headers, method="POST")
    try:
        with request.urlopen(req, timeout=8) as response:
            data = json.load(response)
        content = data["choices"][0]["message"]["content"]
        return str(content)[:650]
    except (OSError, ValueError, KeyError, IndexError, TypeError):
        return None


def handle(text: str, snapshot: dict, threshold: float = 110.0) -> dict[str, str | None]:
    msg = normalize(text).strip()
    if not msg:
        return {"reply": "Dime qué quieres consultar.", "action": None}
    if any(word in msg for word in ("pon musica", "reproduce musica", "play music", "reproduce mi lista")):
        return {"reply": "Solicitando reproducción en el reproductor local.", "action": "music.play"}
    if any(word in msg for word in ("pausa", "para la musica", "deten la musica")):
        return {"reply": "Pausando el reproductor local.", "action": "music.pause"}
    if any(word in msg for word in ("siguiente cancion", "siguiente tema", "siguiente pista")):
        return {"reply": "Pasando a la siguiente pista local.", "action": "music.next"}
    if "temperatura" in msg or "refrigerante" in msg:
        return {"reply": "Temperatura del refrigerante: " + value(snapshot, "coolant_c", "°C") + ".", "action": None}
    if "revoluciones" in msg or "rpm" in msg:
        return {"reply": "Revoluciones: " + value(snapshot, "rpm", "rpm") + ".", "action": None}
    if "velocidad" in msg:
        return {"reply": "Velocidad informada por el adaptador: " + value(snapshot, "speed_kmh", "km/h") + ".", "action": None}
    if "bateria" in msg or "voltaje" in msg:
        return {"reply": "Voltaje leído en el adaptador: " + value(snapshot, "battery_voltage", "V") + ".", "action": None}
    if "combustible" in msg or "gasolina" in msg or "deposito" in msg:
        return {"reply": "Nivel de combustible: " + value(snapshot, "fuel_percent", "%") + ".", "action": None}
    if any(word in msg for word in ("estado del coche", "como esta el coche", "estado del motor", "diagnostico", "monitoriza")):
        a = alerts(snapshot, threshold)
        if snapshot.get("status") != "connected":
            answer = "No puedo leer el adaptador OBD-II. Comprueba la conexión."
        else:
            answer = ("Modo " + ("demostración" if snapshot["source"] == "simulator" else "OBD de solo lectura") + ". " +
                      "Refrigerante: " + value(snapshot, "coolant_c", "°C") + ". " +
                      "Revoluciones: " + value(snapshot, "rpm", "rpm") + ". " +
                      "Combustible: " + value(snapshot, "fuel_percent", "%") + ". " +
                      ("Aviso: " + a[0] if a else "No hay avisos de umbral configurado; esto no descarta averías."))
        return {"reply": answer, "action": None}
    if any(word in msg for word in ("ayuda", "que puedes hacer", "comandos")):
        return {"reply": "Puedo leer temperatura, velocidad, rpm, combustible y voltaje disponibles. También controlar audio local. La conversación libre requiere configurar un modelo de IA.", "action": None}
    llm = model_answer(text[:400], snapshot)
    return {"reply": llm or "No tengo ese comando. Prueba «estado del coche», «temperatura» o «pon música». Puedes activar un modelo de IA opcional.", "action": None}

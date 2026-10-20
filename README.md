# CopilotDrive-AI · Android MVP

App Android convencional para dispositivos abiertos, con dashboard Compose, simulador explícito,
push-to-talk, TTS local, sesiones multimedia autorizadas, navegación externa y contactos con confirmación.
El prototipo Python/web se conserva y funciona por separado.

**Versión experimental para laboratorio detenido.** No controla el vehículo. No es Android Auto, AAOS ni CarPlay.
El movimiento del vehículo se considera desconocido; la velocidad del simulador no permite deducirlo.

### Probar Android

1. Instala el APK debug generado en `android/app/build/outputs/apk/debug/app-debug.apk`.
   En esta sesión también se entrega una copia en `artifacts/CopilotDrive-AI-debug.apk` (no versionada).
2. Abre **CopilotDrive AI** y completa el aviso inicial.
3. Pulsa **Probar consulta**: se consulta combustible simulado (42 %) y se intenta TTS con una voz local instalada.
4. Para voz real: **Ajustes → voz**. Si no hay reconocimiento local, autoriza expresamente el motor del sistema;
   puede enviar audio a su proveedor. Regresa y pulsa **Pulsar para hablar**, concede micrófono y di
   «Jarvis, cuánta gasolina queda». Sin motor STT o voz TTS instalada aparece el estado correspondiente.
5. Audio real: desactiva audio de demostración, autoriza sesiones multimedia y abre un reproductor compatible.
   Para contactos, activa su permiso en Ajustes; «llama a Mercedes» pide elegir si hay varias coincidencias y
   confirmar antes de abrir el marcador. Maps/Waze se abren con «llévame a Gran Vía, Madrid» o «Waze Madrid».

El audio de demostración cambia metadatos/controles pero no reproduce sonido. El LLM es opcional:
esta entrega Android usa el resolver local; el proveedor remoto aún no está implementado.
Bluetooth OBD, POI real, wake word y firma de producción están pendientes.

### Compilar y comprobar

JDK 17 y Android SDK platform 35 / build-tools 35.0.0. Gradle Wrapper incluido y verificado con SHA-256.

```powershell
# Desde la raíz del repositorio en Windows; usa el JDK local .tools si está presente.
.\scripts\android.ps1
python -m unittest discover -s tests -v
python scripts/check_repository.py
```

```bash
cd android
./gradlew :core:test :testing:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# Con dispositivo/emulador conectado:
./gradlew :app:connectedDebugAndroidTest
# Solo núcleo JVM, sin SDK:
./gradlew -PcoreOnly :core:test :testing:test
```

El usuario que instala el APK no necesita estas herramientas. Sin API keys para compilación ni tests.
[Instalación detallada](docs/INSTALL_ANDROID.md) · [Resultados reales](docs/TEST_RESULTS.md) ·
[Estado](docs/IMPLEMENTATION_STATUS.md) · [Limitaciones](docs/KNOWN_LIMITATIONS.md) ·
[Arquitectura](docs/ARCHITECTURE_V1.md) · [Próximos pasos](docs/NEXT_STEPS.md).

---

## Prototipo Python/web conservado (MVP 0.1)

**Asistente experimental open-source para pantallas de infoentretenimiento, PC y Raspberry Pi.**

![status](https://img.shields.io/badge/status-experimental-orange) ![license](https://img.shields.io/badge/license-MIT-green)

> **AVISO:** prototipo solo para pruebas en laboratorio y con el vehículo estacionado. No instalar en un vehículo para usar la interfaz en marcha. No controla actuadores, no es un producto homologado y no sustituye avisos ni instrumentación originales.

## ¿Qué hace hoy?

- Panel responsive de velocidad, rpm, temperatura de refrigerante, combustible y voltaje.
- **Simulación claramente marcada** que funciona sin coche ni adaptador.
- Lectura OBD-II **solo consulta** mediante ELM327 USB o serial (RFCOMM Bluetooth previamente emparejado), cuando se configuran los permisos y puerto.
- Consultas de texto en español y pulsar-para-hablar si el navegador admite Web Speech Recognition.
- Respuestas habladas con la voz del navegador (Web Speech Synthesis).
- Reproducción **de archivos de audio elegidos localmente por el usuario**. No integra Spotify en v0.1.
- Respuestas de IA opcionales con un endpoint **OpenAI-compatible** (OpenAI, Ollama, proxy compatible, etc.). Sin API ni claves, el router de comandos funciona igualmente.
- Reglas informativas de umbral configurables; no sustituye diagnóstico.

**Esta sección describe el prototipo web, no el APK Android descrito arriba. Ninguna versión implementa Android Auto ni Android Automotive.** Tampoco funciona en todas las pantallas OEM. El acceso a micrófono en otros dispositivos requiere contexto seguro HTTPS; en localhost depende del navegador.

## Instalación rápida: modo simulador

Necesitas Python 3.12+:

```bash
python -m app.server
```

Abre **http://127.0.0.1:8787** en Chrome o navegador compatible. El servidor escucha **solo en localhost** por defecto.

Con Docker Compose (Docker instalado):

```bash
docker compose up --build -d
```

Visita http://127.0.0.1:8787. No necesitas instalar librerías Python para ejecutar el modo simulador nativo.

## Consultas disponibles

- «¿Cómo está el coche?»
- «Temperatura del motor»
- «Velocidad» / «Revoluciones» / «Combustible» / «Voltaje»
- «Pon música», «Pausa la música», «Siguiente canción» (archivos locales cargados previamente).

## Telemetría real con ELM327 serial (solo lectura)

En Linux con Python directo:

```bash
python -m pip install pyserial==3.5
TELEMETRY_MODE=obd OBD_PORT=/dev/ttyUSB0 python -m app.server
```

`OBD_PORT` puede ser `/dev/rfcomm0` si emparejaste Bluetooth SPP. En Windows configura un puerto `COMx` y define variables de entorno de PowerShell. **No se garantiza la compatibilidad Bluetooth BLE** ni datos completos: depende de vehículo, adaptador y sistema. No uses adaptadores desconocidos en vehículos de producción.

En Docker, el mapeo específico de dispositivos seriales se configura por separado en un archivo de override, con permisos mínimos. El compose por defecto no monta dispositivos ni permite acceso a la red CAN.

Si el modo OBD no se conecta, muestra `disconnected`, **no inventa valores de ejemplo**.

## IA opcional

Define las variables de entorno `AI_API_BASE`, `AI_MODEL` y, para un proveedor que requiera token, `AI_API_KEY`. Ejemplo con endpoint local OpenAI-compatible:

```bash
AI_API_BASE=http://127.0.0.1:11434/v1 AI_MODEL=qwen2.5:3b python -m app.server
```

Debes tener el modelo y el servidor instalados por separado; el ejemplo no los instala. Las llamadas a la IA son **solo informativas**, sin herramientas de ejecución. La ruta para chat no sirve para controlar el coche.

## Test

```bash
python -m unittest discover -s tests -v
```

## Privacidad y seguridad

- Nunca publica el servicio en todas las interfaces de red de forma predeterminada.
- No guarda historial, VIN, localización ni telemetría en bases de datos.
- Sin LLM configurado, las preguntas no se envían a un proveedor de modelos; el reconocimiento de voz del navegador sí puede depender de servicios externos.
- Con LLM, se envían texto y métricas al endpoint elegido por el usuario.
- Los avisos se muestran en la interfaz abierta. **No hay monitorización garantizada en segundo plano ni alertas de seguridad certificadas**.
- No gestiona Spotify, clima, cerraduras, acelerador, frenos, dirección ni parámetros ECU.

Lee `SECURITY.md` y `docs/ARCHITECTURE.md`.

Para el APK Android y un adaptador ELM327 Bluetooth Classic ya emparejado, sigue [la guía de conexión](docs/CONNECT_OBD.md). Esta integración local no transmite telemetría al proveedor de IA externo; el servidor Python anterior tiene una política de IA distinta descrita arriba.

## Licencia y contribuciones

MIT. Pull requests bienvenidos una vez publicado el repositorio en GitHub.

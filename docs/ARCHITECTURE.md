# Arquitectura y hoja de ruta

## Arquitectura v0.1

```mermaid
flowchart LR
    UI[Panel web local] -->|consulta /api/telemetry| Backend[Servidor Python]
    UI -->|texto o voz browser| Assistant[Router de comandos]
    Assistant -->|consultas generales, opcional| LLM[API OpenAI-compatible]
    Assistant -->|acciones permitidas| Music[Reproductor local en navegador]
    Backend --> Adapter{Fuente telemetría}
    Adapter --> SIM[Simulador explícito]
    Adapter --> OBD[ELM327 serial OBD-II, servicio 01]
    Backend --> Rules[Reglas umbral / avisos]
```

- La interfaz nunca envía instrucciones CAN, comandos OBD ni parámetros del vehículo.
- El servidor NO implementa escritura de ECU, conducción autónoma, apertura de puertas ni controles de clima.
- La IA recibe instantáneas informativas, sin herramientas para acceder al vehículo.
- La conectividad es local por defecto. Toda exposición externa requiere TLS, autenticación y una revisión de seguridad.
- No se recolectan ni almacenan mensajes, ubicación, VIN ni telemetría de forma persistente en v0.1.
- Las lecturas OBD-II estándar varían según modelo, año y adaptador.

## Roadmap

1. v0.1: demostración local y conector ELM327 serial de solo lectura; comandos tipados y voz del navegador.
2. v0.2: aplicación Android nativa (Kotlin) en pantallas Android compatibles, Bluetooth y permisos correctamente gestionados, micrófono y TTS nativos.
3. v0.3: integración Spotify App Remote con autorización individual y control de reproducción, y otros conectores multimedia.
4. v0.4: panel de configuración y detección de compatibilidad de adaptadores y métricas, modelos LLM locales/opcionales.
5. v1.0: distribución de APK firmados, instalador en Linux, actualización, diagnóstico del instalador y matriz de compatibilidad verificada.
6. Conformidad Android Auto/AAOS separada: solo categorías de apps admitidas, interfaces y permisos oficiales. No prometer disponibilidad general en pantallas OEM.

## Seguridad automotriz

No probar en marcha. Conectar o desconectar OBD solo siguiendo las indicaciones del fabricante.
El aparato no es un sistema de seguridad funcional ni tiene homologación; las alarmas son orientativas.
El término «batería» muestra voltaje detectado por el adaptador, no un test clínico del estado de batería.

## Próximas pruebas

- ELM327 genuino USB, sensor de RPM / temperatura, detección de PIDs soportados, reconexión.
- Pruebas unitarias con respuestas mal formadas e interrupción serial.
- Tests de seguridad de endpoints y velocidad de actualización.
- Prueba en laboratorio / coche parado de varios modelos y años.

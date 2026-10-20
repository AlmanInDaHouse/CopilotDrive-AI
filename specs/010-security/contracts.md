# Contratos — Seguridad y privacidad

ToolRegistry allowlist, PolicyEngine, trusted confirmation, Closed arguments.

HTTPTransport impone respuesta máxima de 64 KiB y no sigue redirecciones; proveedor limita request a 16 KiB y timeout a 30 s. Una revocación o cambio de configuración durante la petición descarta la respuesta. SecretStore nunca expone la clave a UI, DataStore ni BuildConfig.

Fuentes de verdad implementadas: android/core/src/main/kotlin/ai/copilotdrive/core/. Sin interfaz de comandos arbitrarios.

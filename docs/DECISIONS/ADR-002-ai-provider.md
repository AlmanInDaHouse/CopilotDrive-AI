# ADR-002: Proveedor conversacional opcional

Estado: implementado en Sprint 2 (2026-10-20).

## Decisión

`AIProvider.generate(AIRequest)` devuelve `AIResponse.Text`, `ProposedTool`, `Unavailable` o `Invalid`. `OpenAICompatibleProvider` usa `/chat/completions` y JSON estricto. El modo básico solo solicita texto; tool calling es opt-in para servidores compatibles. El modelo no decide permisos ni confirma contactos. Una propuesta se vuelve `ToolRequest` y pasa por PolicyEngine y CapabilityRegistry; `contacts.search` y `phone.open_dialer` quedan excluidas de propuestas remotas. La respuesta de una herramienta procede de su ejecución real.

La app persiste endpoint, modelo, timeout y consentimiento en DataStore. El token se cifra con AES-GCM mediante una clave no exportable de Android Keystore; solo el ciphertext queda en SharedPreferences privados. Backup está desactivado y las reglas de extracción excluyen SharedPreferences. El token no se escribe en BuildConfig ni logs. Se permite HTTP únicamente hacia localhost, 127.0.0.1 y 10.0.2.2 en APK debug. Las redirecciones no se siguen. Requests y responses tienen límites de 16/64 KiB y el transporte se desconecta al cancelar.

El historial es efímero por sesión, limitado a ocho mensajes, 2000 caracteres y un presupuesto conservador de 2000 bytes UTF-8 para tokenizadores basados en bytes; se borra con el botón, al destruir el ViewModel y al terminar el proceso. Solo se comparte texto conversacional y ese historial tras consentimiento explícito. Los resultados de herramientas no se incorporan al historial remoto. Telemetría, ubicación y contactos no se envían. La preferencia de contexto del vehículo está reservada y deshabilitada en UI hasta tener un flujo de consentimiento específico.

## Consecuencias

Sin token, consentimiento o red, el dashboard y comandos locales siguen funcionando. Distintos servidores pueden carecer de tool calling; el usuario puede desactivarlo. El almacenamiento Keystore reduce extracción casual, pero no protege frente a un dispositivo comprometido, usuario con control completo, capturas de pantalla del momento de entrada o un proveedor malicioso. No se ha probado una API comercial real en este sprint.

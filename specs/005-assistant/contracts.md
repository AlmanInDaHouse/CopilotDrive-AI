# Contratos — Orquestador

ToolRequest id/arguments; descriptor fija capability/confirmation/timeout, nunca el LLM. ToolResult status/message/error.

AIRequest incluye texto, historial por rol, locale y lista de herramientas permitidas. AIResponse es Text, ProposedTool, Unavailable o Invalid. ToolProposal se convierte a ToolRequest y atraviesa ToolRegistry/PolicyEngine. ProviderConfiguration controla endpoint, modelo, timeout, consentimiento y tool calling. DTO JSON de Chat Completions se valida separadamente del dominio.

Fuentes de verdad implementadas: android/core/src/main/kotlin/ai/copilotdrive/core/. Sin interfaz de comandos arbitrarios.

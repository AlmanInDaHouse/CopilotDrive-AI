# Arquitectura del dashboard modular

`DashboardLayout.kt` en core contiene WidgetType, WidgetSize, WidgetVisibility,
WidgetPlacement, WidgetInstance, DashboardLayout, DashboardPreset, WidgetCapability,
LayoutValidationResult, LayoutLoadResult, LayoutRepository y DashboardEditSession.
WidgetDefinition amplía el contrato reservado original, sin dependencias Android.

WidgetRegistry es cerrado: Vehicle, Media, Navigation, Assistant, Places, ObdStatus,
Clock y SystemStatus. Una instancia de cada tipo, IDs estables, máximo ocho. Tamaños
compacto/expandido; vehículo, audio, Jarvis y sistema admiten además panorámico.
Capability es una dependencia informativa, nunca un permiso ni una prueba de conexión.

LayoutEngine valida versión, IDs/tipos duplicados, tamaños, posiciones, límites,
solapes y existencia de alguna tarjeta visible. First-fit estable usa cuatro columnas
canónicas y hasta 32 filas lógicas. Cambios de orden/tamaño reempaquetan sin azar.
Ocultas también tienen posición válida; proyección omite ocultas y adapta a 1/2/4
columnas según dp disponibles divididos por fontScale. Los spans se proyectan
proporcionalmente: expandida ocupa una columna en una pantalla de dos columnas;
panorámica ocupa ambas. No convierte píxeles en dp. En landscape bajo, la cabecera
se compacta para mantener el combustible visible antes de desplazar.
Cada fila tiene altura natural; una LazyColumn desplaza el contenido sin scroll anidado.
Rotar no modifica el JSON. Véase ADR-007.

DashboardCustomization tiene StateFlow separado del DashboardState existente.
Conserva original/borrador; solo save escribe. DataStoreLayoutRepository usa archivo
`dashboard_layout.preferences_pb`, independiente de preferencias de voz/IA.
JSON explícito v1 estricto, máximo 32 KiB; v0 migra en memoria a v1 y se guarda como
v1 en el siguiente guardado. Falta/corrupción/tipo desconocido/versión futura/layout
inválido recuperan Classic con mensaje. Corrupción del contenedor DataStore recupera
solo el archivo de layout. IO de lectura usa Classic temporal con aviso; IO de
escritura mantiene el borrador y no anuncia éxito. Cancelación se propaga.

La seguridad es efímera: laboratorio explícito y confirmación de entorno de prueba
detenido, TTL 120 s con elapsedRealtime; UNKNOWN sigue UNKNOWN. Velocidad positiva
fresca implica MOVING incluso simulada; cero no demuestra aparcamiento. Movimiento,
background, pérdida de conexión OBD real, revocación o TTL descartan. Un guardado
ya confirmado que haya llegado a disco antes de la cancelación puede estar aplicado;
la revocación no revierte una escritura atómica ya terminada.

Las tarjetas consumen las fuentes originales del único DashboardViewModel. No crean
conexiones, timers, sensores ni AIProvider. El ticker existente actualiza hora/vehículo;
no se incrementa la frecuencia OBD por tarjeta. Media usa supportedControls y
ToolRegistry; navegación usa herramientas existentes. Places solo muestra resultados
de sesión, no dispara búsquedas. Assistant refleja VoiceSessionCoordinator. ObdStatus
usa estado Bluetooth/ELM/PIDs/error; SystemStatus capacidades y tres diagnósticos recientes.
La ausencia de carátula no elimina controles. No se dibuja cartografía ficticia.

Compose conserva claves por instancia y pasa datos específicos a tarjetas. Preview
no ejecuta acciones externas; los controles fijos de voz siguen operativos. La barra
esencial está fuera de la lista: fuente simulada/real, voz/micrófono, respuesta completa
accesible en diálogo, PTT, consulta local y ajustes. No existen herramientas LLM para
layout, permisos o credenciales; políticas anteriores permanecen intactas.

No se han medido FPS, consumo ni recomposiciones. El número acotado de widgets y
reutilización de fuentes son decisiones arquitectónicas, no resultados de benchmark.

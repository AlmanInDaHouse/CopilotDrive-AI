# Contratos ejecutables — layout v1

WidgetDefinition amplía la reserva original con WidgetType, WidgetSize y
WidgetCapability tipados; conserva id/title/minWidth/minHeight/configurationSchema/visibilityRules.
WidgetInstance: id estable/type/size/placement/visibility. WidgetPlacement: column/row.
DashboardLayout: schemaVersion=1/preset/widgets. DashboardPreset: Classic/Driver/Media/Laboratory.
WidgetVisibility: VISIBLE/HIDDEN. LayoutValidationResult: errores explicativos.
DashboardEditSession: original/draft y operaciones puras. LayoutRepository: Flow de
LayoutLoadResult y save suspend. DashboardCustomization: un coordinador StateFlow,
independiente del dashboard, serializa guardados y comprueba seguridad antes de guardar.

IDs y tipos únicos, máximo ocho instancias, límites 4×32, tamaños soportados,
sin solapes incluso ocultos, al menos una tarjeta visible. Capabilities informativas
no conceden permisos. Ausencia de capability muestra indisponibilidad, no conexión.

Fuentes de verdad implementadas: android/core/src/main/kotlin/ai/copilotdrive/core/. Sin interfaz de comandos arbitrarios.

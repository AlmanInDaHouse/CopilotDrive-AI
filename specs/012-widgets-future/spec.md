# Sprint 6 — dashboard modular v1

Registro cerrado: VEHICLE, MEDIA, NAVIGATION, ASSISTANT, PLACES, OBD_STATUS, CLOCK,
SYSTEM_STATUS. Solo consumen fuentes existentes; sin permisos nuevos, embedding ni
herramientas para que el LLM cambie el layout.

Cuadrícula canónica de cuatro columnas. COMPACT=1×1, EXPANDED=2×1, WIDE=4×1.
La celda es lógica, no altura física: filas crecen con contenido y fuente. Proyección
first-fit estable a 1/2/4 columnas por espacio dp/fontScale; rotar no modifica persistencia.
Una lista desplaza filas; barra fija conserva voz/PTT, ajustes y origen vehicular.

Editor separado: añadir, ocultar, mover con botones, tamaño, preview, presets,
guardar, cancelar y restaurar Classic. Una instancia por tipo; ninguna superposición.
Requiere modo laboratorio y confirmación explícita de entorno de prueba detenido.
La confirmación NO verifica aparcamiento. UNKNOWN nunca es PARKED. MOVING bloquea
siempre. Background, revocación y TTL de confirmación de 120 s con reloj monotónico
descartan edición sin guardar. No persistir consentimiento ni borradores.

DataStore guarda solo JSON de layout versionado. Ausencia, corrupción, versión futura,
tipo desconocido o layout inválido recuperan Classic con motivo visible. v0 (mismas
instancias sin preset) migra a v1. Error IO mantiene original y borrador; cancelación
coroutine se propaga. Presets no alteran permisos, proveedores ni datos.

# Auditoría de accesibilidad — 2026-10-20

Inspección de Dashboard/LegacyDashboard, ocho widgets, editor, Ajustes, onboarding,
Device Lab, Places, diálogos, barra de voz y controles multimedia. Comprobaciones
de semántica/estado/objetivos Compose en AccessibilityTest y geometría en
DashboardAdaptiveTest; ninguna equivale a TalkBack manual.

| Hallazgo | Corrección y comprobación |
|---|---|
| A01: switches/sliders aislados sin nombre accesible | Nombres en STT, demo multimedia, IA, herramientas, contexto, velocidad, tono y timeout; test navega Ajustes y comprueba cada nodo |
| A02: botones Subir/Bajar/tamaño/ocultar repetidos | Nombre incluye tarjeta, estado Tamaño actual; test de editor y deshabilitado del primer Subir |
| A03: confirmación laboratorio y toggles Places sin etiqueta propia | Etiquetas explícitas, selección de destino con semántica selected; prueba de toggles y navegación atrás |
| A04: micrófono apagado solo implícito | Texto y stateDescription activo/apagado en barra fija; liveRegion Polite solo en estado/respuesta, no en telemetría/hora |
| A05: secciones sin heading y métricas separadas | Eyebrow con heading; pares nombre/valor agrupados; gráficos decorativos redundantes sin anuncio adicional |
| A06: cabecera Device Lab/Places podía ocupar el ancho del botón Volver | Título usa weight para reservar espacio; navegación Compose existente preservada |
| A07: segunda entrada de voz habilitada durante transcripción/ejecución | Botón del widget deshabilitado en todos los estados ocupados; PTT fijo conserva Detener |

Material Button/Switch/Slider/Checkbox conservan roles, foco y estados nativos.
No se añade contentDescription al texto normal ni se agrupa una tarjeta entera con
acciones independientes. El orden de foco sigue el orden visual de composición;
no se fuerza un índice que contradiga la cuadrícula. La barra esencial está fuera
del scroll. Respuesta completa en diálogo desplazable, con Cerrar y dismiss nativos.
Se comprueban objetivos mínimos de 48dp para PTT, estado/respuesta y consulta.
Botones de editor/media mantienen mínimos de 48–56dp. Sin pruebas humanas del
orden pronunciado, Switch Access ni teclado externo.

La paleta explícita usa fondo #0B1014 y tarjeta #141C21, texto #F0F5F3,
secundario #A7B4BC, menta #B7F6CE y ámbar #F1BC79. El informe de sprint registra
ratios calculados para texto activo: blanco 17.35/15.65, secundario 9.01/8.13,
menta 15.56/14.03 y ámbar 11.12/10.03 (fondo/tarjeta; luminancia sRGB relativa).
El mínimo calculado entre estos pares es 8.13:1. Estados deshabilitados conservan semántica
nativa y la explicación de contexto no disponible. No se afirma una certificación
WCAG de todos los colores internos de Material ni de atribuciones del proveedor.
Errores y simulación tienen texto, sin dependencia exclusiva de color.

Fuentes 1.5× y 2×, vertical y paisaje pequeño: ver ejecución y dimensiones reales
en SPRINT7_REPORT. Los tests controlados en dp se registran por separado de los
overrides del emulador. TalkBack no está instalado en el emulador observado; no se
ha instalado ni habilitado ningún servicio de accesibilidad. Hardware y TalkBack:
PENDING. Repetir con humano: recorrido por headings, estados, diálogo/respuesta
larga, editor completo, permisos, error TTS, retorno/back y fuente extrema OEM.

Referencia de implementación: [Semántica Compose](https://developer.android.com/develop/ui/compose/accessibility/semantics).

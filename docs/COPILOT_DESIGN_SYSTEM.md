# COPILOTDRIVE AI — Instrumentación serena

Sistema implementado en `CockpitDesignSystem.kt`, compartido por dashboard, ocho widgets, onboarding, ajustes, Places y Device Lab. APK Android convencional, sin integración arbitraria de Activity en Android Auto.

## Tres direcciones consideradas

| Dirección | Legibilidad / uso automotriz | Coste | Decisión |
|---|---|---|---|
| Instrumentación serena | Jerarquía vehicular, superficies grafito, acento menta reservado, blanco cálido y estado escrito. Vacíos honestos en primer plano. | Bajo: Canvas pequeño estático, sin bucles de animación, sombras ni fondos complejos. | Seleccionada: máxima claridad de fuente y funcionamiento degradado. |
| Atlas de viaje | Destino dominante y composición cartográfica. Buena para navegación; puede sugerir mapas/rutas que esta app no calcula. | Medio si se añadieran mapas y movimiento. | Descartada para este sprint: sin proveedor de mapa/ETA interno. |
| Estudio editorial | Titulares grandes, números y espacios amplios. Atractiva en móvil vertical, pero consume demasiado alto en 800×480. | Bajo. | Se incorpora la claridad tipográfica, sin adoptar la densidad editorial como layout global. |

## Tokens implementados

| Rol | Valor |
|---|---|
| Fondo | `#090F14` |
| Panel / elevado | `#131D25` / `#1B2933` |
| Texto / secundario | `#F4F2EB` / `#A6B4BE` |
| Acento | `#91DEBC` |
| Precaución / error | `#F0BF77` / `#FFA69E` |
| Separador | `#34444F` |
| Escala espacial | 4, 8, 12, 16, 20, 24, 32 dp; tokens 8/16/24 para componentes nuevos |
| Formas | Controles 12 dp; paneles 16 dp; Jarvis 24 dp; estado 8 dp |
| Elevación | Paneles planos, sin sombras añadidas; jerarquía por superficie y espacio |
| Texto | Android nativo, sp, títulos 22/28/32, display 46, cuerpo 14/16; números monoespaciados |
| Objetivos | 48 dp mínimo; PTT 56 dp; onboarding 60 dp |

`CockpitMark` dibuja cinco símbolos propios de trazo consistente: vehículo, voz, ruta, audio y sistema. Son decorativos junto a etiquetas visibles y se excluyen del árbol accesible. Jarvis tiene una firma de barras de voz y arco, estática según estado. No hay animación perpetua ni trabajo de proveedor desde composición. Material conserva ripple, estado disabled, escalado y diálogo accesible.

## Composición y estados

Cabecera de marca + reloj + distintivo REAL/DEMO. Vehículo usa un titular de conexión y un dato de combustible dominante, con métricas secundarias alineadas. Multimedia tiene estado vacío sin canción/caratula inventadas. Navegación muestra destino y aplicaciones disponibles, sin mapa ficticio ni ETA. Jarvis mantiene PTT, cancelación y acceso a respuesta completa siempre visibles en la barra fija.

`EmptyState`, `ScreenHeading`, `ModeBadge` y `DashboardCard` son reutilizables. Los loaders de arranque, búsquedas y conexión muestran estados escritos; errores y precauciones usan texto además de color. No se altera el contrato de layout por estética: ocho tipos, presets, codec v1 y DataStore conservados. `LegacyDashboardScreen` sigue disponible como referencia histórica; las primitivas compartidas usan el tema nuevo.

## Adaptación

Columnas calculadas con ancho **dp disponible** y fontScale: 1, 2 o 4. Cabecera compacta en horizontal corto; los controles inferiores se reorganizan por anchura efectiva. LazyColumn permite alcanzar contenido que no cabe y deja espacio a los controles fijos. Se respeta safeDrawingPadding del contenedor; no se convierte resolución física en tamaño lógico. Ajustes y formularios mantienen scroll e insets de IME a cargo del sistema.

La paleta de cabina es oscura y fija; no se ofrece un tema claro no implementado. El tamaño de fuente sigue Android. Las verificaciones instrumentadas y visuales se registran por separado de TalkBack humano y del HONOR.

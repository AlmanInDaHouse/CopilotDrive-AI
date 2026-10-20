# Sprint 5: Places y transferencia de navegación

Rama local `feat/sprint5-places-navigation`, creada desde `main` `4fea84df755bdb11b1f4c3f33a53d1ba9cd58f5a` tras `git fetch origin` y fast-forward. Árbol inicial limpio. Los workflows remotos de ese commit terminaron en éxito: [Tests](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39365833566) y [Android](https://github.com/AlmanInDaHouse/CopilotDrive-AI/actions/runs/39365833567).

## Implementación

- Core tipado: `PlaceSearchRequest`, `PlaceSearchResult`, `PlaceDetails`, `PlaceCategory`, `LocationSnapshot`, `LocationAvailability`, `PlaceSelectionState`, `PlacesProviderError`, `PlacesProvider` y `LocationProvider`. Datos desconocidos nulos y fuente/timestamp/disponibilidad explícitos.
- `PlacesController` mantiene resultados solo durante la sesión, descarta resultados duplicados o inválidos, rechaza fixes de más de 30 s, invalida selección al cambiar categoría/borrar y exige destino vigente dentro de 120 s. Búsqueda y apertura requieren declaración actual de entorno detenido; la declaración caduca a los 120 s o al pasar a segundo plano, permitiendo búsquedas posteriores por voz desde el dashboard durante esa ventana.
- `places.search_nearby`, `places.select_result` y `navigation.open_selected_place` se despachan por `ToolRegistry`/`PolicyEngine`. Jarvis resuelve categorías locales, muestra ratings existentes, selecciona por ordinal y exige confirmación de voz antes de transferir. Sin proveedor de rutas responde que no dispone de ETA verificada.
- Android: permiso coarse bajo demanda, fuente `LocationManager`, consentimiento de Places independiente de IA, Nearby Search (New) con Google Places SDK `5.1.1`, campo mínimo explícito y demo determinista rotulada. Panel Compose para búsqueda/selección/apertura/borrado, con atribución «Google Maps» y atribuciones de terceros devueltas. AndroidNavigation existente abre el destino real.
- Device Lab registra permiso, estado de proveedor, consentimiento, disponibilidad de Maps/Waze, código de error y latencia de búsqueda sin guardar coordenadas ni términos. El emulador y fake no acreditan GPS o apps físicas.

## Validación

Python 12/12; JVM core 17/17, testing 56/56 y app 2/2; emulador Android 15/API 35 15/15; total 102/102. Android lint 0 errores, 12 avisos. `check_repository.py` y `git diff --check` aprobaron. Smoke adicional 1/1: Jarvis respondió a combustible local con Wi-Fi/datos desactivados tras revocar ubicación; la conectividad se restauró. APK instalado y MainActivity arrancó. Los tests no usan credenciales ni una API real. El APK sin clave arranca en `NOT_CONFIGURED`; la demostración está etiquetada.

APK local `artifacts/CopilotDrive-AI-sprint5-debug.apk` (19.825.143 bytes), SHA-256 `49125CDEBFD8B1E8D8DA116A2F99180A988209EEF9C77E9A85C86C0669FF3291`. `apksigner` verifica firma debug v2, un firmante. Detalle en `TEST_RESULTS.md`.

## Límites

No hay clave Google configurada en este checkout, por lo que **ninguna búsqueda comercial está verificada**. No existe geocodificador fiable para centro manual. Ratings/reseñas pueden aumentar el SKU y el coste. No hay ETA ni lectura de ruta activa. La firma debug no es de producción. GPS físico, Google Maps/Waze reales, voz acústica y ELM327 siguen pendientes en `PHYSICAL_VALIDATION_MATRIX.md`. Antes de distribución real se revisarán atribuciones, términos, privacidad y facturación de Google para la versión/región efectiva.

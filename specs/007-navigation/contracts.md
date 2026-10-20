# Contratos — Navegación y POI

NavigationProvider.openDestination; NavigationSnapshot sin ETA; PlacesProvider resultados marcados simulados.

Fuentes de verdad implementadas: android/core/src/main/kotlin/ai/copilotdrive/core/. Sin interfaz de comandos arbitrarios.

## Sprint 5

`PlaceSearchRequest(category, center, radiusMeters)` admite solo categorías enumeradas y un `LocationSnapshot` autorizado con TTL 30 s. `PlaceSearchResult` incluye ID/nombre/categoría/dirección/coordenadas/rating/recuento/distancia/apertura/fuente/timestamp/disponibilidad/atribuciones; desconocidos son null. `PlaceSelectionState` caduca a los 120 s. `PlacesProviderError` conserva código técnico sin cuerpo personal de respuesta.

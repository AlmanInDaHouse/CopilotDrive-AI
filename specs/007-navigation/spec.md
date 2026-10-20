# Navegación y POI

Maps y Waze oficiales, destinos como texto codificado; validar, resolver intent, fallback explícito. POI real no configurado.

Alcance por etapa: consultar docs/IMPLEMENTATION_STATUS.md. Una especificación no implica implementación completa.

Sprint 5: Nearby Search real opcional mediante Places SDK Android o demo etiquetada. Categorías allowlist, resultados tipados con nulos para información ausente, deduplicación, distancia en línea recta y selección de 120 s. La apertura requiere selección vigente y destino válido. El intent no prueba ruta activa ni ETA. Sin clave: NOT_CONFIGURED. El centro manual requiere geocodificador fiable y permanece deshabilitado.

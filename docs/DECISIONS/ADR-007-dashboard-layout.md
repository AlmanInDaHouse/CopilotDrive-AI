# ADR-007 — registro nativo y documento de layout independiente

Estado: aceptado, 2026-10-20.

Core contiene contratos cerrados, validación, codec/migración, first-fit y edición
pura. App posee DataStore, lifecycle y Compose; sin ViewModel por widget. Documento
canónico de cuatro columnas independiente de dp/densidad. Proyección adaptativa
por ancho/fontScale no escribe DataStore. Filas de altura natural y una lista evitan
cortar contenido. Botones accesibles de orden/tamaño, sin gestos obligatorios.

Seguridad de sesión con TTL monotónico, nunca en JSON. Laboratorio explícito y
confirmado; UNKNOWN no certifica aparcamiento. Movimiento descarta incluso simulado.
Background revoca confirmación. Ninguna herramienta LLM accede al repositorio.
Capacidades faltantes muestran indisponibilidad. JSON inválido recupera Classic;
IO se muestra al usuario y no simula guardado. Cancelación se propaga.

Ocho tipos únicos acotan validación y memoria. Sin nuevos permisos/dependencias.
LegacyDashboardScreen conserva la referencia Sprint 5; ausencia de layout migra
al preset Classic. La validación física sigue separada de la de emulador.

# ADR-001 — Android convencional primero
Estado: aceptado. Fecha: 2026-10-20.

Contexto: prototipo Python funcional; pantallas OEM cerradas no equivalen a Android abierto.
Decisión: APK Kotlin/Compose, minSdk 26, compile/target 35, JVM core independiente. Mantener Python/web.
Android Auto, AAOS y CarPlay requieren trabajo y validación independientes. Sin permisos privilegiados.
Consecuencia: instalar solo en dispositivos que permitan APKs; capacidades se detectan en ejecución.
No se promete disponibilidad de sensores, reconocimiento offline, TTS español o sesiones de otras apps.

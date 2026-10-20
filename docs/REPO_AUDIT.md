# Auditoría — 2026-10-20
Base: 04efb44, árbol limpio, remoto oficial AlmanInDaHouse/CopilotDrive-AI. Rama de trabajo feat/android-mvp.

| Área | Estado inicial y reutilización |
|---|---|
| app/server.py | HTTP stdlib, localhost, rutas estáticas cerradas, CSP. Preservar como simulador opcional. JSON de lista causa AttributeError en POST: corregir con prueba HTTP. |
| app/assistant.py | Router español y chat opcional sin herramientas. Reutilizar intenciones, no acoplar APK al servidor. Algunas respuestas individuales omiten procedencia simulada. |
| app/telemetry.py | Simulador y serial ELM327 con 4 PIDs modo 01. Parser RPM acepta respuesta truncada; validar bytes. No capacidades por PID ni frescura por métrica. |
| web/ | HTML/CSS/JS responsive, Web Speech y audio local. Conservar íntegro; no implica integración Spotify. |
| tests/test_core.py | 6 unittest, todos pasan en Python 3.13.14 (0.002 s). Sin pruebas HTTP, lifecycle ni contratos. |
| .github/workflows/tests.yml | Python 3.12 en Ubuntu. Mantener y añadir workflow Android separado. |
| Docker/compose | Python slim, usuario sin privilegios, puerto publicado en localhost. Sin Android. |

Leídos README, SECURITY, arquitectura, todos los fuentes, tests y configuración versionados.
No JDK, Gradle, SDK ni adb detectados inicialmente. Herramientas de desarrollo se preparan en .tools ignorado.
No hardware ni API keys disponibles. No se han modificado preferencias globales del equipo.

Deuda: endpoints sin autenticación (solo laboratorio local), sin timeout HTTP de lectura, proveedor LLM
Python recibe todas las métricas disponibles; Android no reutilizará esa divulgación. El simulador
Python tiene reloj real; el harness Kotlin inyectará reloj determinista. No hay puente de red Android-Python
hasta validar esquema versionado y consentimiento. No eliminar el prototipo ni anunciar compatibilidad OEM.

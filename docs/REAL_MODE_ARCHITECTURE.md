# REAL / DEMO — contrato Sprint 11

`OperationMode` es un enum de core, independiente de `VehicleConnectionState`. El modo inicial es REAL; el proveedor Bluetooth inicia DISCONNECTED con métricas nulas. El ticker no abre sockets. Solo selección manual + confirmación invocan connect.

## Inventario de fuentes y simulaciones

| Punto auditado | Antes / uso | Sprint11 |
|---|---|---|
| `core/BluetoothObd.kt` | Estado y snapshot inicial SIMULATED; `snapshot()` renovaba números ficticios. | Inicial DISCONNECTED/nulos. Solo `disconnect(simulator = true)` explícito activa simulación. Fallos no lo invocan. |
| `core/Contracts.kt`, `Ports.kt` | `simulatedVehicle` y `SimulatorVehicleProvider`, combustible 42 y métricas de fixture. | Conservados para DEMO y tests; no elegidos por REAL. Invariantes prohíben FIXTURE/SIMULATED en snapshot real. |
| `core/DemoMediaAdapter.kt` | Catálogo y reproducción demo. | Conservado; Android selecciona `AndroidMediaAdapter` por defecto. Sin sesión, metadata nula y controles vacíos. |
| `DashboardViewModel.kt` | `demoMedia = true`, flujo inicial demo y selección independiente de lugares. | Flags iniciales false; OperationMode coordina cambio explícito, invalida caché/resultados y detiene audio. |
| `core/Places.kt` / `platform/AndroidPlaces.kt` | Proveedor demo y ubicación fija de Madrid opcionales; Google SDK opcional. | Coordenadas de fixture solo con DEMO. REAL usa Android coarse y Google configurado tras consentimiento; ausencia de clave no usa fake. |
| `platform/AndroidActions.kt` | Navegación real por intent, contactos reales tras permiso. | Preservados. Mensaje de apertura no afirma ruta/ETA; marcador nunca llama. |
| `SecureProvider.kt`, `core/OpenAICompatibleProvider.kt` | IA opcional con transporte y credencial protegida. | Preservados: configuración/consentimiento explícitos y sin datos vehiculares externos. |
| `ui/Dashboard.kt`, `ModularDashboard.kt` | Dibujaban valores del proveedor simulado inicial y vacíos poco diferenciados. | Distintivos de modo/fuente, métricas tipadas, vacío real y representación diferenciada de caducidad/PID. No se inventa ETA/caratula. |
| `CopilotApp.kt` | Onboarding «Empieza con un vehículo simulado» y botón de retorno al simulador. | Onboarding REAL; elección global de DEMO explicada en Ajustes. |
| `android/testing`, `androidTest` | Harness, transportes, MediaSession y estados sintéticos. | Conservados, pruebas ejecutadas solo en AVDs desechables. Captura multimedia fixture etiquetada dentro de la imagen. |
| `app/telemetry.py`, `app/assistant.py`, `app/server.py` y web | Prototipo Python/web de demostración y sus fixtures. | Se conserva funcional según AGENTS; no se empaqueta ni se usa como fuente del APK Android. REAL/DEMO de este sprint es el contrato de operación Android. |

## Persistencia y migración

`copilot_preferences` conserva nombre, ubicación y todas sus claves previas. Añade `operation_mode` y `real_mode_notice_acknowledged`; no modifica el DataStore `dashboard_layout` ni su schema. Instalación nueva: REAL y onboarding nuevo. Instalación anterior sin clave: REAL con aviso visible y descartable. No existía preferencia persistente de simulación: el valor anterior era un default en memoria, no una elección que pueda recuperarse. Una elección REAL/DEMO guardada se conserva. Datos corruptos desconocidos no habilitan demo.

El aviso pendiente sobrevive a guardar otros ajustes hasta reconocerlo. `copy` conserva voz/consentimientos/IA y el layout se restaura por su coordinador existente. Error de lectura conserva defaults REAL con aviso de error; error de escritura se muestra y no promete persistencia.

El cambio de modo cancela voz y pruebas acústicas, borra historial efímero y resultados/destino de Places, revoca su consentimiento por sesión, cierra OBD e invalida la caché. DEMO selecciona proveedores demo explícitamente. Volver a REAL elimina sus snapshots antes del siguiente dato y nunca conecta por sí solo. En segundo plano se detienen voz/TTS y OBD real.

## Telemetría honesta

El schema JSON v1 de Metric/VehicleSnapshot no cambia. `TelemetryState` deriva CURRENT, DEMO, DISCONNECTED, UNSUPPORTED, STALE o UNAVAILABLE a partir de conexión, availability y PIDs descubiertos. Esto distingue un PID ausente de una lectura NO DATA sin romper fixtures compartidos.

| Condición | UI / herramienta |
|---|---|
| Sin OBD | «OBD desconectado» y «No disponible»; Jarvis explica que todavía no está conectado. |
| Conectado, PID no anunciado | «No compatible»; herramienta devuelve UNSUPPORTED. |
| PID soportado sin lectura | «No disponible»; UNAVAILABLE. |
| Más de 10 s / reloj inválido | «Dato obsoleto»; STALE, sin repetir el número. |
| Lectura real válida | Valor, unidad y fuente OBD; nunca FIXTURE/SIMULATED en snapshot real. |
| DEMO explícito | Valores ficticios etiquetados visualmente y con prefijo hablado «Simulación». |

`ToolRegistry` mantiene política determinista, límites de tiempo y permisos. Las consultas de combustible no necesitan IA ni claves. La respuesta escrita sobrevive a fallos TTS. El atajo «Probar consulta» aporta texto al mismo pipeline local: no activa un proveedor ficticio en REAL.

## Límites de confianza

Se preserva Bluetooth Classic ELM327. Solo vocabulario cerrado AT de inicialización y PIDs de lectura mode 01. No ECU writes, borrado DTC, CAN libre, VIN ni comandos del modelo. Una radio disponible no implica ELM conectado; Nissan Juke 2020–2023 y sus PIDs siguen sin validación hardware. UNKNOWN no se convierte en PARKED; edición de widgets exige laboratorio y confirmación temporal.

DEMO no es fallback de fallo. Fakes de pruebas, harness y fixtures permanecen. MediaSession real sin sesión produce vacío. Places sin clave queda NOT_CONFIGURED. IA externa es opt-in, con HTTPS/Keystore y consentimiento separado, sin telemetría/contactos/ubicación. Contactos requieren permiso y ACTION_DIAL confirmado. Abrir Maps/Waze no confirma que exista ruta activa.

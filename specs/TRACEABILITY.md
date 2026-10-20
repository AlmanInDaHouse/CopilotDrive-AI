# Trazabilidad de aceptaci?n

| Spec | Evidencia automatizada | Lo que no demuestra |
|---|---|---|
| 000,001 | Gradle compile/assemble/lint; Python unittest | Homologaci?n, todas las pantallas OEM |
| 002 | DashboardUiTest, DashboardPresentationTest | Ergonom?a f?sica y accesibilidad completa |
| 003 | ScenarioTest media, MediaSelectionTest, PlatformMediaTest con sesi?n Android real | Spotify/SoundCloud/YouTube f?sicos |
| 004 | ScenarioTest voz, AppFlowTest hasta respuesta con ViewModel real | STT/TTS ac?sticos, ruido y llamadas reales |
| 005 | ScenarioTest policy, tools, malicious AI proposals, timeouts | Proveedor remoto, que est? deshabilitado |
| 006 | ContractTest, SharedFixturesTest, BluetoothObdTest y tests/test_contracts.py | Adaptador y vehículo físico; las pruebas OBD son fake |
| 007 | ContractTest URI, ScenarioTest navigation | Ruta, ETA ni recepci?n real Maps/Waze |
| 008 | ScenarioTest b?squeda, ambig?edad, confirmaci?n ?nica/caducada | Agenda y marcador de un dispositivo f?sico |
| 009 | ScenarioTest denegaci?n/revocaci?n y PlatformMediaTest | Todas las variantes OEM |
| 010 | Policy tests, scripts/check_repository.py y lint | Auditor?a de penetraci?n o CVE exhaustiva |
| 011 | DashboardUiTest onboarding, AppFlowTest, instalaci?n adb debug | Firma de producci?n y distribuci?n |
| 012 | DashboardLayoutTest, DashboardCustomizationTest, LayoutPreferencesTest, editor Compose y DataStore | Sin prueba OEM, editor no expuesto a IA |
| 013 | DiagnosticsTest, BluetoothObdTest, ScenarioTest, AppFlowTest Device Lab, Sprint2PlatformTest HTTP loopback, matriz manual | Sin teléfono, adaptador, coche ni proveedor remoto real validados físicamente |
| 014 | DeviceQualificationTest, qualify_android y recuperacion emulada | Sin promocion a evidencia fisica |
| 015 | PerformanceReadinessTest, StabilityScenarioTest, StabilityJourneyTest, runners de estabilidad/benchmark y comparacion | Emulador no representa telefono; PSS no prueba fuga; benchmark no es test funcional |

Los comandos, recuentos y resultados efectivos se mantienen en docs/TEST_RESULTS.md.

Sprint 9/spec016: scripts jank_evidence/host_health/compatibility_android,
test_jank_evidence, PlatformMediaTest y StabilityJourneyTest relacionan trazas,
calificación API26/30/35, revocación y lifecycle. No prueban fluidez OEM ni ausencia
de fugas; ver JANK_INVESTIGATION, MEMORY_INVESTIGATION y aceptación con bloqueos.


Sprint10 /017: PhysicalPilotTest (session/build/evidence), PhysicalVoicePilotTest
(callback/human separation, cancellation/recovery/privacy, A provider independence),
PhysicalPilotUiTest (confirmation UI and packaged coarse-only permissions),
test_api26_offline.py (network parser/restoration/failure and signature conflicts).
Actual B and API results: docs/SPRINT10_REPORT.md; physical observations PENDING.

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
| 012 | WidgetDefinition en core; editor no expuesto | Editor a?n no implementado |

Los comandos, recuentos y resultados efectivos se mantienen en docs/TEST_RESULTS.md.

# Preparación técnica de distribución de prueba — Sprint 8

Resultado: candidata técnica para revisión y ensayos consentidos; no publicación,
firma de producción ni verificación física. Estado exacto de ejecución en
SPRINT8_REPORT, TEST_RESULTS y STABILITY_TEST_REPORT. Una firma Android Debug no
constituye una release de producción.

| Comprobación | Estado y evidencia |
| --- | --- |
| Dependencias | Versiones fijadas; core/harness sin dependencias Android. Macrobenchmark1.3.4 opt-in y ProfileInstaller1.4.1 solo añadido a benchmark. No actualización global de SDK/toolchain. No auditoría CVE exhaustiva |
| Credenciales | Token cifrado AES/GCM en Keystore; IO serializado fuera de Main. Consentimiento revalidado tras lectura suspendida y tras respuesta; sin caché de token en ViewModel. Fixtures de prueba solo en tests |
| Claves Places | Vacías en ambos APK de esta ejecución; benchmark fuerza vacío. Android funciona sin claves y sin Python |
| Manifest/permisos | RECORD_AUDIO, READ_CONTACTS, INTERNET, coarse location, Bluetooth clásico hasta API30 y CONNECT. Sin ubicación precisa/background/scan privilegiado. Hardware opcional |
| Exportados debug | Launcher MainActivity; PreviewActivity de tooling y ComponentActivity de ui-test-manifest solo debug; ProfileInstallReceiver requiere DUMP. MediaAccessService no exportado, protegido por BIND_NOTIFICATION_LISTENER_SERVICE |
| Exportados benchmark | MainActivity y ProfileInstallReceiver protegido por DUMP. Sin Activity de tooling/test-manifest. Profileable shell solo en esta variante |
| APK del runner de benchmark | Instrumentación en su propio proceso/paquete; dependencias oficiales solicitan QUERY_ALL_PACKAGES, almacenamiento y REORDER_TASKS para pruebas/trazas. Es un APK de herramientas separado, no permisos de producción de CopilotDrive |
| Red | Producción y benchmark deniegan cleartext; HTTP debug limitado a localhost/127.0.0.1/10.0.2.2. Redirects HTTP desactivados. Nada de credenciales comerciales en CI |
| Backup | allowBackup=false/fullBackupContent=false y reglas de extracción excluyen secretos. Prueba física de backup/restore OEM pendiente |
| Logging | Sin cuerpos HTTP, audio, transcripciones ni credenciales en diagnósticos. Informes numéricos redactados. Revisar trazas/logs locales antes de exportar |
| Firma | APK debug y benchmark firmados con certificado Android Debug local. Release pública no generada; no claves de producción |
| Sin Internet | Consultas locales/simulador disponibles; IA y Places siguen opcionales. Regresión offline y proveedores fake/loopback separada de servicio real |
| Sin micrófono/TTS/Bluetooth | Permiso/capacidad tipados, respuesta textual y retorno a simulador; recuperación en harness. API35 no acredita acústica física |
| Telemetría | Solo PIDs de lectura allowlisted; sin VIN/CAN libre/acciones intrusivas. Contexto vehicular/contactos/ubicación excluidos de IA |
| Simulación | UI y salida hablada etiquetadas; UNKNOWN no equivale a PARKED. Editor solo con laboratorio y confirmación temporal |
| Layouts | Casos Compose y matriz emulada; fuentes/insets/TalkBack OEM requieren acta manual |
| Rendimiento | TTID/TTFD instrumentados separados de cota ADB y PSS. No presupuesto inestable obligatorio en CI; muestras/condiciones en PERFORMANCE_COMPARISON |
| Fluidez | Jank medido en las cuatro interacciones del emulador; causa sin aislar bajo renderer software/presión de host. Umbrales y fluidez en hardware PENDING; no se declara rendimiento de producción aprobado |
| Estabilidad | Runner no destructivo; configuración restaurada y recuperación observada según STABILITY_TEST_REPORT. Una ejecución acotada no demuestra ausencia de fugas |
| API26/API30 | PENDING ejecución: solo imagen API35 disponible. minSdk 26 es requisito de instalación, no certificación |
| Hardware y servicios comerciales | PENDING: voz audible/STT, ELM327/coche, reproductores, Maps/Waze/GPS, TalkBack, contactos ficticios y proveedores con cuota |

Antes de distribución externa: revisar los componentes de test presentes en debug,
obtener hardware/consentimiento y ejecutar los recorridos de PHYSICAL_TEST_PROCEDURES.
Una futura variante de distribución debe diseñarse y revisarse por separado, con
firma/secretos/backup/privacidad y compatibilidad verificadas. No se publica desde
este sprint ni se activa escucha permanente o servicio prolongado de micrófono.

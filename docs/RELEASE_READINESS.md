# Preparación actual — Sprint 11

Candidata debug Android convencional 0.11.0-debug (versionCode 2), con REAL predeterminado y rediseño nativo. [SPRINT11_REPORT.md](SPRINT11_REPORT.md) contiene hashes, firma, pruebas, capturas y pendientes. No se ha hecho push, merge ni release.

No se declara lista para producción comercial: firma de producción, credenciales restringidas, pruebas hardware y nueva validación física permanecen pendientes. El APK debug es para prueba consentida; no se instala automáticamente. Se conserva la política coarse-only, sin micrófono de fondo ni escrituras al vehículo.

Historial conservado a continuación.

---

# Sprint10 — candidata debug para instalación controlada

Revisión técnica local, sin publicación ni firma de producción. Resultados finales
y hash en SPRINT10_REPORT.md. Instalación física pendiente de autorización explícita;
preflight pasivo en scripts/pilot_package.py comprueba paquete/SDK/firma/permisos y
posibles conflictos con la instalación previa. No uninstall, downgrade ni data clear.

Se corrigió el permiso preciso transitivo de Places observado en APK Sprint9: el
manifest final conserva coarse y elimina FINE mediante tools:node=remove, con test
de permisos empaquetados. ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE y permiso interno
de receptor de AndroidX siguen declarados por dependencias; listado final completo
obligatorio en apk-metadata.json. No hay nuevo permiso privilegiado, background o CAN.
Las comprobaciones previas sobre manifest fuente no eran auditoría del merge real.
La [documentación oficial de Places](https://developers.google.com/maps/documentation/places/android-sdk/legacy/current-place#permissions)
explica la retirada de permisos transitivos cuando no se usa findCurrentPlace; aquí
se conserva coarse para AndroidLocationProvider propio y se retira solo FINE.

Gate físico: PENDING. Gate performance: BLOCKED por recursos del host. Motores OEM,
Pico TTS de API26, TalkBack, OBD y proveedores no se aprueban por compilar ni por callback.
No benchmark nuevo ni servicio comercial. Ver limitaciones e instrucciones del piloto.

## Evaluación histórica Sprint9

# Preparación técnica de distribución de prueba — Sprint 9

Resultado: candidata técnica para revisión y ensayos consentidos; no publicación,
firma de producción ni verificación física. Estado exacto de ejecución en
SPRINT9_REPORT, TEST_RESULTS y STABILITY_TEST_REPORT. Una firma Android Debug no
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
| API26/API30/API35 | 34/34 instrumentadas por versión en AVDs oficiales; rotación/foreground en26 y smoke offline completo en30/35. Offline26 BLOCKED por svc wifi137, estado observado restaurado; no certificación OEM |
| Hardware y servicios comerciales | PENDING: voz audible/STT, ELM327/coche, reproductores, Maps/Waze/GPS, TalkBack, contactos ficticios y proveedores con cuota |

Antes de distribución externa: revisar los componentes de test presentes en debug,
obtener hardware/consentimiento y ejecutar los recorridos de PHYSICAL_TEST_PROCEDURES.
Una futura variante de distribución debe diseñarse y revisarse por separado, con
firma/secretos/backup/privacidad y compatibilidad verificadas. No se publica desde
este sprint ni se activa escucha permanente o servicio prolongado de micrófono.

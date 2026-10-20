# Limitaciones conocidas

PR #1 y #2 están fusionadas; el workflow Android más reciente de `main` (`b3646df`) terminó correctamente. Sprint 2 permanece en una rama local sin CI remoto.

| Área | Limitación e impacto | Próxima validación/cambio |
|---|---|---|
| Hardware | Sin vehículo ni pantalla aftermarket física; no homologación ni uso en marcha | Matriz de dispositivos en laboratorio |
| Telemetría Android | RFCOMM y descubrimiento PID implementados y probados con fake; ningún adaptador o coche físico validado | Matriz física de teléfono, adaptador y vehículo |
| IA | Adaptador Chat Completions real implementado, pero no validado con un servidor comercial ni uno local real | Probar interoperabilidad y respuesta bajo condiciones de red reales |
| Contexto IA | Solo texto e historial efímero; vehículo/contactos/ubicación no se transmiten; toggle de contexto deshabilitado | Diseñar contexto por dato con consentimiento específico |
| Credenciales | Keystore cifra token local, pero un dispositivo comprometido o controlado por el usuario puede exponerlo al usar la app | Recomendar credencial con cuota y revocación; prueba de backup/restore |
| Voz | STT/TTS depende de servicios y voces instalados; el emulador API 35 informó motor TTS ausente y 0 voces locales es-ES. Fallback STT puede usar red tras consentimiento | Prueba acústica física, llamadas entrantes, ruido, motores OEM |
| Wake word | Contrato reservado, sin detector local ni escucha permanente | Elegir motor/licencia y medir consumo |
| Media | Sesiones Android autorizadas; controles pueden ser ignorados por proveedor. Sin seek ni SDK Spotify específico | Probar varios reproductores físicos; añadir seek tipado |
| Carátulas | Solo bitmaps entregados por MediaSession; URI de carátula remota no descargada | Adaptador de imágenes local con límites, si se justifica |
| Navegación | Solo apertura intent; no puede verificar ruta activa, ETA ni distancia | Prueba con apps Maps/Waze instaladas; SDK autorizado para rutas integradas |
| Contactos | Consulta local y ACTION_DIAL; no llamada automática. Muestra hasta cinco números | Validar agenda física, nombres duplicados y accesibilidad |
| POI | Puerto/fake de pruebas; herramienta real siempre NOT_CONFIGURED | Proveedor autorizado, ubicación y cuotas |
| Estado de conducción | UNKNOWN; confirmar entorno de prueba no certifica PARKED | Fuente fiable antes de funciones avanzadas |
| UI | Dashboard adaptable, sin editor de widgets; selección de voces puede ser larga | 800×480 y 360×640 revisados con scroll; falta tamaño de fuente máximo y hardware físico |
| Distribución | APK debug con clave del entorno; Sprint 2 solo local y sin release | CI de la nueva rama tras autorización de push; firma de producción fuera del repositorio |
| Dependencias | Versiones estables fijadas, avisos de actualización de lint; no auditoría CVE exhaustiva | Auditoría y renovación controlada de la matriz |
| Python heredado | Servidor local sin auth; LLM opcional envía métricas y texto según README original | Mantener en localhost; puente Android futuro requiere nuevo diseño de consentimiento |

Los fakes prueban contratos, no disponibilidad de servicios de terceros. Ver TEST_RESULTS.md para qué
se ejecutó realmente. No se debe convertir una especificación o una prueba definida en una afirmación de soporte.

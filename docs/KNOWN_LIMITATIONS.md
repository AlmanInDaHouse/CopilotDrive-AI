# Limitaciones conocidas

| Área | Limitación e impacto | Próxima validación/cambio |
|---|---|---|
| Hardware | Sin vehículo ni pantalla aftermarket física; no homologación ni uso en marcha | Matriz de dispositivos en laboratorio |
| Telemetría Android | Solo simulador y parser OBD probado; no transporte Bluetooth, detección PID ni reconexión real | Adaptador read-only separado con fixtures seriales |
| IA | Resolver local + AIProvider/fake; no configuración de endpoint remoto en APK | Consentimiento, almacenamiento seguro, HTTPS y contrato cerrado antes de habilitar red |
| Voz | STT/TTS depende de servicios y voces instalados; fallback STT puede usar red tras consentimiento | Prueba acústica física, llamadas entrantes, ruido, motores OEM |
| Wake word | Contrato reservado, sin detector local ni escucha permanente | Elegir motor/licencia y medir consumo |
| Media | Sesiones Android autorizadas; controles pueden ser ignorados por proveedor. Sin seek ni SDK Spotify específico | Probar varios reproductores físicos; añadir seek tipado |
| Carátulas | Solo bitmaps entregados por MediaSession; URI de carátula remota no descargada | Adaptador de imágenes local con límites, si se justifica |
| Navegación | Solo apertura intent; no puede verificar ruta activa, ETA ni distancia | Prueba con apps Maps/Waze instaladas; SDK autorizado para rutas integradas |
| Contactos | Consulta local y ACTION_DIAL; no llamada automática. Muestra hasta cinco números | Validar agenda física, nombres duplicados y accesibilidad |
| POI | Puerto/fake de pruebas; herramienta real siempre NOT_CONFIGURED | Proveedor autorizado, ubicación y cuotas |
| Estado de conducción | UNKNOWN; confirmar entorno de prueba no certifica PARKED | Fuente fiable antes de funciones avanzadas |
| UI | Dashboard adaptable, sin editor de widgets; selección de voces puede ser larga | 800×480 y 360×640 revisados con scroll; falta tamaño de fuente máximo y hardware físico |
| Distribución | APK debug con clave del entorno; sin release ni push | Firma de producción fuera del repositorio tras aceptación |
| Dependencias | Versiones estables fijadas, avisos de actualización de lint; no auditoría CVE exhaustiva | Auditoría y renovación controlada de la matriz |
| Python heredado | Servidor local sin auth; LLM opcional envía métricas y texto según README original | Mantener en localhost; puente Android futuro requiere nuevo diseño de consentimiento |

Los fakes prueban contratos, no disponibilidad de servicios de terceros. Ver TEST_RESULTS.md para qué
se ejecutó realmente. No se debe convertir una especificación o una prueba definida en una afirmación de soporte.

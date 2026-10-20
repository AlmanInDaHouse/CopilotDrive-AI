# Limitaciones conocidas

PR #1 a #4 están fusionadas. Sprint 4 está en una rama local sin CI remoto propio; los resultados actuales se describen en `SPRINT4_REPORT.md`.

| Área | Limitación e impacto | Próxima validación/cambio |
|---|---|---|
| Hardware | Sin vehículo ni pantalla aftermarket física; no homologación ni uso en marcha | Matriz de dispositivos en laboratorio |
| Telemetría Android | RFCOMM y descubrimiento PID implementados y probados con fake; ningún adaptador o coche físico validado | Matriz física de teléfono, adaptador y vehículo |
| IA | Adaptador Chat Completions probado contra servidor HTTP loopback en emulador, pero no contra proveedor comercial | Probar interoperabilidad y respuesta bajo condiciones de red reales |
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
| Distribución | APK Sprint 3 debug firmado localmente; sin release ni CI remoto de la rama | CI tras autorización de push; firma de producción fuera del repositorio |
| Dependencias | Versiones estables fijadas, avisos de actualización de lint; no auditoría CVE exhaustiva | Auditoría y renovación controlada de la matriz |
| Python heredado | Servidor local sin auth; LLM opcional envía métricas y texto según README original | Mantener en localhost; puente Android futuro requiere nuevo diseño de consentimiento |

Los fakes prueban contratos, no disponibilidad de servicios de terceros. Ver TEST_RESULTS.md para qué
se ejecutó realmente. No se debe convertir una especificación o una prueba definida en una afirmación de soporte.
# Sprint 4 — límites actuales

El Device Lab informa disponibilidad y resultados técnicos. Sus checks no certifican micrófono, voz audible, RFCOMM físico, lectura del coche ni compatibilidad comercial. El nivel de evidencia permanece `PENDING` en la app; las observaciones físicas se documentan manualmente. No existe exportación de diagnóstico ni memoria persistente de resultados. p50/p95 de voz requieren al menos 20 muestras por etapa y no representan calidad acústica.

El contrato de contexto vehicular no transmite datos al proveedor. Las pruebas IA opcionales pueden consumir cuota. La conectividad, modelo y tool calling de cada proveedor compatible deben probarse por separado. El simulador y el servidor HTTP local no garantizan compatibilidad de adaptadores ni APIs externas.\n

## Sprint 5 — límites adicionales y corrección de POI

La línea anterior de POI («puerto/fake; siempre NOT_CONFIGURED») describe el estado antes de Sprint 5. Ahora hay proveedor Google Places SDK opcional y demo explícita. Este checkout no tiene clave, factura ni consulta comercial validada. Sin clave se mantiene `NOT_CONFIGURED` salvo demo. El centro manual no está disponible porque falta geocodificación autorizada y fiable. Nearby Search de ratings/reseñas puede ser facturable; cuota, condiciones EEE, atribuciones y políticas de privacidad requieren revisión de despliegue. Solo se ha probado la navegación simulada en emulador; Maps/Waze físicos y GPS siguen pendientes. No hay ETA ni ruta activa observada. La precisión depende del dispositivo y servicio de ubicación; se descartan fixes mayores de 30 s. El control «entorno detenido» es declaración del usuario, no detección verificada de movimiento.

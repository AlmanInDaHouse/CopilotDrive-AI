# Limitaciones conocidas

Base Sprint 7: Sprint 6 fusionado por PR #7, main `1328077`; CI Android/Python del
merge aprobado. Sprint 7 permanece local sin CI remoto propio; ver `SPRINT7_REPORT.md`.

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
| POI | Google Places opcional y demo; sin consulta comercial/GPS físico validado | Clave restringida, ubicación y cuotas en proyecto de prueba |
| Estado de conducción | UNKNOWN; confirmar entorno de prueba no certifica PARKED | Fuente fiable antes de funciones avanzadas |
| UI | Editor de widgets disponible solo en laboratorio explícito y temporal; sin drag-and-drop, se usan botones | Hardware físico y escalas de fuente extremas pendientes; resoluciones reales ejecutadas en SPRINT6_REPORT.md |
| Distribución | APK Sprint 7 debug firmado localmente; sin release ni CI remoto de Sprint 7 | CI tras autorización de push; firma de producción fuera del repositorio |
| Dependencias | Versiones estables fijadas, avisos de actualización de lint; no auditoría CVE exhaustiva | Auditoría y renovación controlada de la matriz |
| Python heredado | Servidor local sin auth; LLM opcional envía métricas y texto según README original | Mantener en localhost; puente Android futuro requiere nuevo diseño de consentimiento |

Los fakes prueban contratos, no disponibilidad de servicios de terceros. Ver TEST_RESULTS.md para qué
se ejecutó realmente. No se debe convertir una especificación o una prueba definida en una afirmación de soporte.

## Sprint 7 — alcance comprobado

Solo emulador Android 15/API35 conectado. Ningún teléfono físico, ELM327/coche ni
TalkBack ensayado. Los contratos y el runner distinguen evidencia; no hay certificación
física cerrada. Semántica automatizada no equivale a lector de pantalla humano.
Las carreras nativas STT/TTS/socket se han endurecido por inspección de código,
compilación y regresiones de contratos; OEM, micrófono/acústica y RFCOMM requieren hardware.

Arranque/PSS y cota superior de dashboard se documentan en PERFORMANCE_BASELINE.
Debug/emulador, proceso frío con cachés calientes: sin equivalencia a release o teléfono.
No TTFD exacto, FPS, energía, recomposiciones o percentiles de voz con muestras insuficientes.
Persistencia tras force-stop no equivale a restauración por LMK de Android. No se
ha medido una campaña prolongada de horas ni todas las combinaciones de insets/fuente.
El runner físico requiere confirmación humana y flags; los flags por sí solos no
constituyen consentimiento. No exporta pantalla, conversaciones ni identificadores.
La señal SIGKILL/corte de energía puede impedir finally; revisar/restaurar manualmente
si el host se cierra abruptamente durante una matriz de display en emulador.
# Sprint 4 — límites actuales

El Device Lab informa disponibilidad y resultados técnicos. Sus checks no certifican micrófono, voz audible, RFCOMM físico, lectura del coche ni compatibilidad comercial. El nivel de evidencia permanece `PENDING` en la app; las observaciones físicas se documentan manualmente. No existe exportación de diagnóstico ni memoria persistente de resultados. p50/p95 de voz requieren al menos 20 muestras por etapa y no representan calidad acústica.

El contrato de contexto vehicular no transmite datos al proveedor. Las pruebas IA opcionales pueden consumir cuota. La conectividad, modelo y tool calling de cada proveedor compatible deben probarse por separado. El simulador y el servidor HTTP local no garantizan compatibilidad de adaptadores ni APIs externas.\n

## Sprint 6 — límites del editor y la evidencia

El editor funciona solo con modo laboratorio y confirmación explícita temporal.
No existe una señal certificada de aparcamiento: velocidad cero sigue UNKNOWN.
El control de movimiento consume las lecturas originales; no garantiza detectar
movimiento si no hay sensor/PID válido. La declaración del usuario no sustituye esa
medición. No hay edición por voz/LLM ni drag-and-drop; los botones son la alternativa
accesible elegida. No hay widgets remotos, incrustación de apps ni mapa integrado.
La versión 0 migrable usa el mismo modelo de instancias; no se importa un formato
arbitrario de terceros. La migración se guarda como v1 al próximo guardado explícito.
Una escritura atómica ya finalizada no se revierte al salir a segundo plano.

Las capturas/resoluciones y escalas ejecutadas se detallan en SPRINT6_REPORT.md.
No hay validación física de pantallas, TalkBack, acústica, GPS, ELM327 ni reproductores
comerciales. No se han medido FPS, recomposiciones, batería ni latencia del editor.
La matriz física existente permanece PENDING donde ya lo estaba.

## Sprint 5 — límites adicionales y corrección de POI

Sprint 5 sustituyó el puerto/fake de POI por Google Places SDK opcional y demo explícita. Este checkout no tiene clave, factura ni consulta comercial validada. Sin clave se mantiene `NOT_CONFIGURED` salvo demo. El centro manual no está disponible porque falta geocodificación autorizada y fiable. Nearby Search de ratings/reseñas puede ser facturable; cuota, condiciones EEE, atribuciones y políticas de privacidad requieren revisión de despliegue. Solo se ha probado la navegación simulada en emulador; Maps/Waze físicos y GPS siguen pendientes. No hay ETA ni ruta activa observada. La precisión depende del dispositivo y servicio de ubicación; se descartan fixes mayores de 30 s. El control «entorno detenido» es declaración del usuario, no detección verificada de movimiento.

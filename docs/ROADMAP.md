# Sprint 11 — REAL y cabina nativa

REAL predeterminado y DEMO explícito; rediseño Compose y APK versionCode 2. Arquitectura, pruebas y límites en [SPRINT11_REPORT.md](SPRINT11_REPORT.md). La base remota canónica es el merge Sprint10.1 PR #11 `be3344c`; Sprint9 y el piloto físico previo están preservados.

Sprint 12 propuesto: validación consentida del nuevo APK en HONOR, luego ELM327/Juke detenido; proveedor Places de pruebas con clave restringida y cuota; sesiones multimedia y Maps/Waze reales. Rendimiento emparejado solo con host apto. No ampliar a controles ECU ni Android Auto dentro de este sprint.

---

# Sprint10 — piloto físico preparado, calificación abierta

Sprint9 fusionado por PR#10 en `793a353`; Android/Tests del merge success verificados.
Sprint10 implementa confirmación acústica por intento, etapas de voz redactadas,
contratos de acta/build y preflight pasivo de firma/permisos. API26: causa de svc
fundamentada y alternativa Settings, sin elevar permisos. Exclusión explícita de
ubicación precisa añadida al manifest fusionado. Ver SPRINT10_REPORT.md para ejecución.
Sin teléfono ni observación humana: físico PENDING. Sprint11: campaña consentida
STT/TTS/TalkBack y luego OBD/media/navegación; rendimiento solo con recursos válidos.

## Histórico hasta Sprint9

# Roadmap por incrementos
0. Auditoría, baseline, specs y memoria.
1. Shell Compose y simulador explícito.
2. Core, políticas, contratos versionados y harness determinista.
3. Push-to-talk, TTS, voces locales, cancelación y consentimiento.
4. MediaSession con permiso de notificaciones y selección de sesión.
5. Intents Maps/Waze, contactos locales, confirmación y ACTION_DIAL.
6. Proveedor IA remoto opcional con configuración segura, timeout y minimización: implementado en Sprint 2; interoperabilidad comercial pendiente.
7. Bluetooth OBD de solo lectura con RFCOMM, harness serial, selector y dashboard: implementado en Sprint 3; validación física pendiente.
8. Onboarding, diagnósticos y APK debug de revisión: implementados; release firmada tras validación física.
9. Sprint 4 propuesto: matriz física OBD y STT/TTS, seguida de interoperabilidad LLM con consentimiento y sin telemetría externa.

Estado real, sin equiparar código con prueba física: IMPLEMENTATION_STATUS.md y TEST_RESULTS.md.
# Sprint 4 y siguiente

Sprint 4 instala infraestructura de diagnóstico, harness y matriz de validación física sin declarar hardware verificado. Sprint 5 recomendado: ensayos con teléfonos, voces, adaptadores y coche concretos; reparar incompatibilidades medidas; medir latencia de voz con muestra suficiente. El intercambio de contexto vehicular requiere revisión independiente de minimización, consentimiento y revocación antes de activarse.\n
## Sprint 5

Lugares cercanos: contratos tipados, ubicación aproximada solicitada al buscar, consentimiento Google Places independiente de IA, proveedor oficial opcional, demostración determinista, selección temporal y handoff a Maps/Waze. La validación física de OBD, voz, GPS y aplicaciones de navegación sigue como trabajo independiente. Ver `SPRINT5_REPORT.md`.

## Sprint 6

Dashboard modular implementado: ocho widgets nativos, registro cerrado, cuadrícula lógica, presets Classic/Driver/Media/Laboratory, editor seguro en laboratorio, DataStore versionado, recuperación y pruebas. Evidencia y límites en `SPRINT6_REPORT.md`. Sprint 7 recomendado: completar matriz física de pantallas/voz/OBD/MediaSession/GPS/Maps/Waze y resolver incompatibilidades observadas; sin habilitar edición en conducción.

## Sprint 7/8 fusionados, Sprint 9 local

Sprint 7/PR #8 fusionado en `328ffec`, con CI Android/Tests aprobado. Sprint 7 implementa contratos
de calificación y runner consentido con hashes/firma, semántica accesible, limpieza y
recuperación de voz, revalidación de consentimiento Places y mediciones del emulador.
Ver SPRINT7_REPORT para resultados. La certificación física sigue PENDING.
Sprint 8 añade TTID/TTFD instrumentados, benchmark aislado, contratos de rendimiento,
runner de estabilidad restaurable y correcciones mínimas en IO/MediaSession. Resultados
y límites en SPRINT8_REPORT. Sprint 8 ya está fusionado por PR #9, main `504e9a7`,
con Android y Tests del merge en success. Sprint 9 investiga jank con Perfetto,
memoria y compatibilidad API26/30/35; hardware consentido sigue independiente.
Ver SPRINT9_REPORT y NEXT_STEPS para resultados y pendientes reales.

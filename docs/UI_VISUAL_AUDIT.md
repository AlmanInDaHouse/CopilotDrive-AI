# Auditoría visual — Sprint 11

## Estado anterior observado

Se abrió el APK que ya estaba instalado en `CopilotDrive_API35`, antes de modificar fuentes.
La captura auténtica está en `artifacts/sprint11/before/dashboard.png` (1280×800).
Su hash registrado es `c23f86e1b7dc7f3dea05d9af6e41374ef2b79a58d1227b302b7ca1ab12400ea2`;
no se equipara este APK emulado al APK físico Sprint10 archivado. La UI anterior muestra 42 %, 850 rpm y una pista demo desde el arranque. Son datos
etiquetados como simulación, pero ocupan la experiencia normal. Es el principal fallo
de confianza de datos, además del problema visual.

Composición observada: cabecera genérica «Tu viaje, a tu manera»; botones cápsula;
tarjetas con contorno y jerarquías semejantes; combustible simulado dominante;
Jarvis queda por debajo del primer pliegue y depende de una barra inferior larga.
La navegación presenta estados técnicos, y «SIMULADOR» aparece al pie en vez de
establecerse primero como una elección de operación. El mismo estilo de contenedor
se repite en pantallas largas de configuración.

## Cambios implementados

Marca COPILOTDRIVE AI y reloj con números monoespaciados; distintivo de modo al inicio
y fuente fija junto a voz. Hero vehicular con superficie elevada, conexión como
titular y métricas alineadas. Sin OBD, un vacío deliberado y sin barra de combustible
a cero que parezca una lectura. Jarvis usa superficie verde profunda, firma de voz
estática y estados escritos. Multimedia vacía no tiene pista/caratula inventada.
Navegación identifica la app externa y la ausencia de ETA.

El sistema usa paneles con papeles visuales diferenciados (vehículo, voz, secundarios),
espaciado consistente y menos colores de estado arbitrarios. Se sustituyen cabeceras
en Ajustes, Places y Device Lab, y se reescribe onboarding para REAL. Ajustes se
ordena General, Voz, IA, Vehículo, Integraciones, Privacidad, Diagnósticos y Apariencia.
Permanecen los ocho widgets y su contrato/persistencia; no hay maqueta web.

## Método de verificación

Las capturas posteriores proceden de UiAutomation/screencap del APK en un AVD,
nunca de imagen generada ni de Compose Preview. El fixture multimedia está aislado
en `MediaFixtureVisualTest` y lleva «TEST FIXTURE · DEMO» dentro de la imagen.
Las capturas REAL usan el ViewModel y adaptadores Android, sin inyectar lecturas.
Las escenas DEMO se activan expresamente por la UI del APK.

`RealModeJourneyTest` cubre dashboard REAL, DEMO, Jarvis en espera/respuesta, audio
vacío, Ajustes, Device Lab y Places. `sprint11_visual_matrix.py` captura resoluciones
físicas 1080×2412, 2412×1080, 800×480, 1024×600, 1280×720 y comparación 1280×800,
con densidad explícita; además fuente 150 %. Los tests adaptativos ejercitan dp
y fontScale hasta 2. No se equiparan píxeles físicos con dp.

La matriz restaura tamaño/densidad/fontScale/rotación en finally. La suite completa
usa AVD desechable: puede escribir credenciales de fixture, preferencias y layout.
El emulador original se reserva aparte con hashes de APK y ambos DataStore para
comparación al recuperarlo. No se modifica el teléfono físico.

Resultados observados, imágenes seleccionadas e incidencias: se registran al final
de esta auditoría y en [SPRINT11_REPORT.md](SPRINT11_REPORT.md). Las pruebas de
semántica no sustituyen una revisión humana TalkBack; las capturas de emulador no
certifican insets ni legibilidad física en HONOR.

## Iteración observada 1

La primera suite API35 ejecutó 42 casos con tres fallos conservados en
`artifacts/sprint11/api35-attempt1.failure.log`: visibilidad tras scroll dentro de
una sección larga, espera de resultados Places que aún no estaban compuestos y
espera de readiness tras cargar las preferencias reales. Los cuatro recorridos
nuevos REAL/DEMO pasaron, incluida persistencia y ausencia de cifra ficticia.

Se mantiene cada aserción: se lleva el control accesible a la vista antes de
verificarlo; la búsqueda espera su estado accesible READY antes de desplazar la
lista al resultado; el helper de estabilidad espera dashboard-ready, además de
la existencia del contenedor. Se añadió stateDescription de Places para anunciar
el estado sin depender de que su panel esté visible.

La inspección de las capturas detectó un titular vehicular heredando el color
secundario de surfaceVariant, duplicación de «Micrófono apagado» y una captura de
Ajustes durante el cierre del diálogo. Se fijó contentColor del panel, se evitó
duplicar el prefijo y se estabilizó la captura respecto a Window/RenderThread.
La fuente inferior ahora distingue conexión en curso y errores, además de desconexión.
Las imágenes de esta iteración se conservan fuera de Git para no confundirlas con
la candidata final.

## Resultado final observado

Código `7463623`, APK `37adf5ee0bcc7668e7aebb529feb26feb8b28bca155c48f5456feb6ffd56f606`. Tras la variante compacta y la acción de lectura ampliada, 42/42 en API26/30/35.
7 muestras de resolución/fontScale con restauración verificada. La vista 800×480
muestra ahora el dato de combustible antes de la explicación de fuente; las acciones
inferiores siguen visibles. El fixture tiene insets y fondo nativo completos.
Se inspeccionaron las capturas sin convertir los resultados simulados en validación real.

| Antes | REAL final |
|---|---|
| ![Antes](assets/sprint11/before-dashboard.png) | ![REAL](assets/sprint11/compare-1280x800.png) |

[Vertical](assets/sprint11/portrait-1080x2412.png) · [800×480](assets/sprint11/landscape-800x480.png) ·
[DEMO explícito](assets/sprint11/sprint11-dashboard-demo.png) ·
[Fixture etiquetado](assets/sprint11/sprint11-media-test-fixture.png).
Contraste mínimo calculado para el texto secundario sobre las tres superficies: 7,01:1.
Capturas y manifiesto sanitizados conservados en docs/assets; logs/intentos crudos en
artifacts ignorado. Jank bloqueado por la guarda de memoria, no declarado aprobado.

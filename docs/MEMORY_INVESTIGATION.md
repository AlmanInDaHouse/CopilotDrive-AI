# Sprint 9 — memoria y ciclo de vida

El incremento histórico de 7007 KiB procede de 20 muestras Debug.getPss dentro
del proceso instrumentado (142575 → 149582 KiB; máximo150973). Incluye Compose,
instrumentación, pruebas, recreaciones, JIT y cachés. No demuestra una fuga ni
puede compararse como si fuera el mismo heap que un arranque sin instrumentación.
La serie de procesos reiniciados de Sprint 8 (+4 KiB) tampoco prueba retención.

## Auditoría

| Componente | Propiedad/cierre observado en código | Lo que sigue sin demostrar |
| --- | --- | --- |
| AndroidTextToSpeech | Application Context desde ViewModel; espera suspendida acotada; stop en background; close/shutdown en onCleared; continuación de utterance cancelada | Retención interna del motor OEM o binder tras muchas sesiones acústicas |
| AndroidSpeechRecognizer | Crea recognizer solo al pulsar; closeSession idempotente en Main, cancel/destroy y continuación cancelada; identidad de sesión protege reemplazos | Precisión/STT y referencias retenidas por servicios de terceros |
| AndroidMediaAdapter | unregister callback/listener y limpia controllers/artwork al cerrar; demo evita polling Android | Bitmaps grandes y cachés publicadas por un reproductor real |
| BluetoothObdProvider/AndroidBluetoothObd | Un poller manual; socket close desbloquea IO; generación descarta apertura cancelada; worker shutdownNow al disponer | Retención de driver/RFCOMM físico; el emulador no la reproduce |
| DataStore | Delegate por Application Context, sin instancia por widget; lecturas Flow asíncronas; layout/codec acotados | Corrupción o apagado abrupto OEM fuera de casos ya probados |
| ViewModel/navegación | viewModelScope dueño de collectors; ticker cancelado en background; diálogos/drafts/conversación efímeros; cierre adaptadores en onCleared | No existe un análisis de dominadores que certifique ausencia de retención |
| Imágenes | MediaMetadata entrega Bitmap; no fetch/decodificación URI en composición | Tamaño máximo de bitmaps y memoria GPU en hardware |

No se encontró captura de Activity en los adaptadores construidos por el ViewModel;
se usa Application. Una revisión estática no prueba que toda referencia del framework
se libere inmediatamente. Las corrutinas DataStore/collectors de ViewModel pueden
seguir vivas mientras vive el ViewModel: no son automáticamente fugas.

## Instrumentación reproducible

StabilityJourneyTest ahora registra en el mismo PID y después de cada ciclo:
PSS total, heap Java asignado (totalMemory-freeMemory), heap nativo asignado,
PSS gráfico de MemoryInfo, Activities antiguas todavía accesibles por WeakReference
y número de jobs hijos directos del ViewModel. Verifica que recreate conserva el
mismo ViewModel y que no ha cambiado PID. Las referencias guardadas por el test
son débiles; el recolector puede liberar objetos en momentos no deterministas.
No se fuerza GC ni se interpreta un objeto vivo sin GC como fuga demostrada.

El runner conserva las nuevas columnas numéricas y separa la fase sostenida de
la fase force-stop/reinicio. Preferencias se restauran en finally; layout se compara
en cada ciclo, display y micrófono se restauran/verifican. No se concede un micrófono
previamente denegado. Usar AVD de pruebas, nunca datos privados para heap profiling.

```powershell
$apk = 'android/app/build/outputs/apk/debug/app-debug.apk'
$hash = (Get-FileHash $apk -Algorithm SHA256).Hash
python scripts/stability_android.py --serial emulator-5554 --apk $apk --expected-sha256 $hash --cycles 100 --restart-cycles 20 --allow-active --out artifacts/sprint9/stability-100.json
```

Heap dump/dominadores se consideran un siguiente paso si aparecen referencias
crecientes que sobrevivan a varias ventanas de GC o crecimiento sostenido del heap.
No se captura un heap privado ni se declara resuelta una fuga por estabilizar PSS.
Resultados ejecutados y límites temporales se completan en SPRINT9_REPORT y
STABILITY_TEST_REPORT; las mediciones de instrumentación no son consumo de producción.

## Resultado ejecutado — 100 ciclos y 20 reinicios

API35 en AVD independiente; configuración solicitada 1536 MiB, pero el emulador
registró «Increasing RAM size to2560MB». No se presenta 1536 como RAM efectiva.
APK `501b01a1…bc08d5c`, AndroidTest
`fb098e17…52b1f2e0`. Duración total 330627 ms; fase sostenida hasta 240753 ms.
100 muestras en el mismo PID, después 20 reinicios con PSS separado.
Layout, pantalla, permiso y hashes de ambos paquetes verificados al terminar.
0 crashes y 0 ANR observados por historial acotado exit-info de esta ventana.

| Observación sostenida | Primera | Última | Mínima | Máxima |
| --- | ---: | ---: | ---: | ---: |
| PSS total instrumentado (KiB) | 154150 | 146935 | 144886 | 156432 |
| Heap Java asignado (KiB) | 20936 | 7368 | 7280 | 20936 |
| Heap nativo asignado (KiB) | 22699 | 19976 | 19965 | 23479 |
| PSS gráfico reportado (KiB) | 0 | 0 | 0 | 0 |
| Activities anteriores WeakReference vivas | 1 | 1 | 1 | 3 |
| Jobs directos del ViewModel | 10 | 10 | 10 | 10 |

El cero gráfico es un valor devuelto por MemoryInfo, no una estimación de que no
existen bitmaps/GPU. Heap asignado y PSS son magnitudes distintas: no sumar columnas.
Las referencias débiles bajan durante la serie y no se acumulan con los 100 ciclos;
una referencia viva al final no basta para declarar retención indebida. La nueva
prueba finishingActivityCancelsViewModelJobs aprobó en 26/30/35 y observa cancelación
al finalizar Activity; tampoco acredita ausencia universal de objetos retenidos.

**Comparación PSS inconclusa por entorno.** El host terminó con 302–346 MiB
disponibles: HOST_PHYSICAL_MEMORY_PRESSURE. La caída de 7215 KiB no es una mejora
demostrada ni contradice el aumento histórico de 7007 KiB. Cambian AVD, binario
instrumentado, duración, cachés y condiciones del host. No se identifica una fuga
reproducible ni se certifica su ausencia. El patrón acotado de referencias y tareas
no justifica exportar un heap privado; no se capturaron heap dumps ni conversaciones.
Raw numérico: artifacts/sprint9/stability-100.json y memory-host.json.

La población de reinicios se midió aparte: PSS inicial no instrumentado 78729 KiB;
20 reinicios entre 78379 y 79970 KiB, último 78566 KiB. No se resta esa población
del heap sostenido ni se usa para descartar retención en un proceso prolongado.

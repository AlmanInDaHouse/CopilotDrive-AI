# Matriz de privacidad de ubicación

| Flujo | Origen | Destinatario | Permiso / consentimiento | Retención |
| --- | --- | --- | --- | --- |
| Dashboard, combustible local, OBD | Ninguno | Ninguno | Ninguno de ubicación | Ninguna ubicación |
| Búsqueda real | `LocationManager`, red preferida | Google Places SDK | `ACCESS_COARSE_LOCATION` al pulsar búsqueda y consentimiento Places de sesión | Fix y resultados solo en memoria; TTL fix 30 s |
| Búsqueda simulada | Centro ficticio Madrid | Proveedor fake local | Confirmación de entorno detenido | Solo memoria; etiqueta SIMULACIÓN |
| Selección de resultado | Resultado vigente | Solo app local | Selección explícita; caduca en 120 s | Solo memoria |
| Abrir Maps/Waze | Coordenadas o dirección del lugar elegido | Aplicación instalada elegida | Pulsar botón o confirmar voz | Su política de retención independiente |
| IA externa | Texto e historial efímero acotado | Endpoint IA configurado | Consentimiento IA separado | Nunca se añade ubicación, resultados Places ni telemetría al contexto IA |
| Diagnóstico | Estado, código, latencia | Historial técnico local | Acción del usuario | Máximo 100 eventos de sesión; sin coordenadas ni búsquedas |

No se solicita ubicación en segundo plano. Un permiso denegado o revocado deja disponibles el dashboard, Jarvis local, audio y OBD. La búsqueda por voz devuelve instrucciones para entrar en Lugares y conceder el permiso; no activa un diálogo inesperado.
La declaración de entorno detenido dura como máximo 120 s y se borra al pasar la app a segundo plano. No equivale a una medición de movimiento.

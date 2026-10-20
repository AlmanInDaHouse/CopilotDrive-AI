# ADR-006: Places SDK for Android (New)

Fecha: 2026-10-20. Estado: adoptado para Sprint 5.

Se usa el proveedor oficial **Google Places SDK for Android (New)**, fijado en `5.1.1` por compatibilidad con Kotlin 2.2. `6.0.2` requiere metadatos Kotlin 2.4 y no compila en esta base. La interfaz `PlacesProvider` mantiene intercambiables el adaptador real y la demostración determinista. El dashboard, comandos locales y simulador funcionan sin clave ni backend.

La consulta `searchNearby` usa tipos permitidos, radio 3 km, máximo 10 resultados y una lista explícita de campos: ID, nombre, dirección, coordenadas, rating y número de reseñas. Rating y recuento pueden activar la tarifa **Nearby Search Enterprise**; se omiten horarios y rutas. El equipo debe configurar facturación, cuotas y alertas antes de uso real. Ningún test de CI usa la API comercial.

La clave es **de aplicación Android**, restringida en Google Cloud por `ai.copilotdrive.app`, SHA-1 de firma y API Places SDK for Android. Aparece técnicamente en el APK; no equivale a un secreto de servidor. Una clave de web service o servidor exige un backend opcional con control de abuso y nunca debe añadirse aquí. El valor llega desde una propiedad Gradle o variable de entorno local y no se versiona.

El panel identifica Google Places y muestra las atribuciones devueltas cuando existan. No solicita fotos ni reseñas individuales. Antes de distribuir una build real se debe revisar la política de presentación, marca, Términos de Uso y Política de Privacidad para el país y versión de SDK aplicables. En el EEE rigen términos específicos desde julio de 2025.

Fuentes oficiales: [configuración](https://developers.google.com/maps/documentation/places/android-sdk/config), [Nearby Search](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search), [campos y facturación](https://developers.google.com/maps/documentation/places/android-sdk/usage-and-billing), [atribuciones y políticas](https://developers.google.com/maps/documentation/places/android-sdk/policies), [restricciones de clave](https://developers.google.com/maps/faq).

# Seguridad y privacidad

Sin historial persistente/audio bruto/secretos en logs. Network Security Config no cleartext. Unknown motion no equivale a parked.

Sprint 2 habilita INTERNET solo para la IA opcional. Requiere consentimiento para texto e historial, credencial cifrada con Android Keystore y HTTPS remoto. HTTP loopback se admite solo en APK debug. No se envían contactos, ubicación ni telemetría.

Alcance por etapa: consultar docs/IMPLEMENTATION_STATUS.md. Una especificación no implica implementación completa.

## Sprint 3: frontera OBD

El transporte Bluetooth solo admite la inicialización interna ATZ, ATE0, ATL0, ATS1 y ATSP0 y solicitudes de lectura 0100, 0120, 010C, 010D, 0105, 012F. Nunca admite servicio 04, escritura ECU, tramas CAN arbitrarias ni comandos ELM procedentes del usuario o LLM. Las herramientas `vehicle.get_fuel`, `vehicle.get_temperature` y `vehicle.get_snapshot` pasan por `VehicleContextEngine`; el proveedor remoto recibe texto e historial acotado, nunca telemetría. El consentimiento de conexión Bluetooth no autoriza enviar datos al proveedor remoto. No se lee ni guarda VIN.

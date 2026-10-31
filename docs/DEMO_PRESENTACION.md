# Datos de presentación

El seed usa las cuentas `comprador.pruebas@agromarket.test` y
`proveedor.pruebas@agromarket.test`. Las contraseñas se solicitan al ejecutar el script y no se guardan
en el repositorio. Los registros incluyen una indicación explícita de demostración.

Desde la raíz del backend:

```bash
python3 scripts/seed-presentation.py
```

El destino predeterminado es la API de AgroMarket en Cloud Run, proyecto `eventflow-509015`.
Para otro entorno, define `AGROMARKET_API_URL`. El script carga los datos mediante los casos de uso
existentes de la API, conserva las reglas de inventario y no elimina registros. Puede repetirse sobre
el conjunto original sin duplicarlo; no restablece transacciones que se hayan avanzado durante la demo.

## Recorrido sugerido

1. Entra como comprador: explora productos agrícolas y reciclables, filtra y abre favoritos.
2. Abre el café o maíz para mostrar detalle, reputación y disponibilidad.
3. Abre conversaciones: ambos participantes tienen mensajes de ejemplo. Envía un mensaje desde un
   dispositivo y observa su llegada en el otro.
4. Muestra reservas pendientes, aceptadas, confirmadas, listas para entrega, entregadas y completadas.
5. Entra como proveedor: revisa sus publicaciones y acepta una reserva pendiente.
6. Abre analítica y reseñas: las operaciones completadas aportan volumen y reputación.

Hay 24 publicaciones (18 del proveedor y 6 del comprador), 15 reservas en distintos estados,
11 conversaciones con 44 mensajes, 9 reseñas de proveedor, 4 de producto y 12 favoritos.
Las dos cuentas pueden operar como compradoras y proveedoras.

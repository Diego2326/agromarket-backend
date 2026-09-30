# AgroMarket Backend

API REST en Kotlin, Spring Boot y PostgreSQL. Implementa el flujo comercial del cronograma: autenticación,
usuarios, publicaciones, reservas e inventario, favoritos, conversaciones, reputación y analítica.

## Arquitectura

Cada módulo está separado en capas:

- `domain`: modelo y contratos del negocio.
- `application`: casos de uso y orquestación.
- `infrastructure`: JPA, PostgreSQL, BCrypt, JWT y Spring Security.
- `presentation`: controladores, validación y DTOs HTTP.

Una cuenta puede operar como compradora y proveedora; no se restringen estas capacidades mediante roles separados.

## Configuración local

1. Crea la base de datos PostgreSQL `agromarket`.
2. Ajusta el archivo `.env` que ya está creado localmente. Este archivo está ignorado por Git.
3. Como referencia para otros entornos, utiliza `.env.example`.
4. Ejecuta:

```bash
./gradlew bootRun
```

Flyway aplica automáticamente todas las migraciones. Hibernate está configurado en modo `validate`, por lo que no altera el esquema.

Para poblar un entorno local con 12 usuarios, 24 publicaciones, 10 reservas, conversaciones y reseñas:

```bash
DEMO_DATA_ENABLED=true ./gradlew bootRun
```

La cuenta inicial es `proveedor1@agromarket.test` y todas las cuentas demo utilizan `password123`. El seed es
idempotente y está desactivado por defecto; no debe habilitarse en producción.

Swagger queda disponible en `http://localhost:8080/swagger-ui.html`.

La referencia de DTOs, enums, errores y mapeo de endpoints para Android está en
[`docs/FRONTEND_API_TYPES.md`](docs/FRONTEND_API_TYPES.md).

## Endpoints

| Método | Ruta | Autenticación | Uso |
|---|---|---|---|
| `GET` | `/api/health` | No | Comprobar disponibilidad de la API |
| `POST` | `/api/auth/register` | No | Registrar una cuenta y obtener JWT |
| `POST` | `/api/auth/login` | No | Iniciar sesión |
| `GET` | `/api/users/me` | Bearer JWT | Consultar el perfil actual |
| `PATCH` | `/api/users/me` | Bearer JWT | Actualizar nombre, teléfono o ubicación |
| `GET` | `/api/categories` | Bearer JWT | Consultar categorías y productos |
| `GET` | `/api/unit-types` | Bearer JWT | Consultar unidades de medida |
| `GET` | `/api/publications` | Bearer JWT | Buscar, filtrar, ordenar y paginar publicaciones activas |
| `GET` | `/api/publications/{id}` | Bearer JWT | Consultar el detalle de una publicación activa |
| `GET` | `/api/publications/mine` | Bearer JWT | Listar las publicaciones propias en cualquier estado |
| `POST` | `/api/publications` | Bearer JWT | Crear una publicación activa |
| `PUT` | `/api/publications/{id}` | Bearer JWT | Editar una publicación propia |
| `PATCH` | `/api/publications/{id}/status` | Bearer JWT | Pausar, reactivar o cerrar una publicación propia |
| `POST` | `/api/reservations` | Bearer JWT | Reservar cantidad y comprometer inventario |
| `GET` | `/api/reservations/mine` | Bearer JWT | Reservas realizadas como comprador |
| `GET` | `/api/reservations/received` | Bearer JWT | Reservas recibidas como proveedor |
| `PATCH` | `/api/reservations/{id}/accept` | Bearer JWT | Aceptar una reserva recibida |
| `PATCH` | `/api/reservations/{id}/reject` | Bearer JWT | Rechazar y restituir inventario |
| `PATCH` | `/api/reservations/{id}/cancel` | Bearer JWT | Cancelar una reserva permitida |
| `PATCH` | `/api/reservations/{id}/confirm` | Bearer JWT | Confirmar una reserva aceptada |
| `PATCH` | `/api/reservations/{id}/ready` | Bearer JWT | Marcar lista para entrega |
| `PATCH` | `/api/reservations/{id}/delivered` | Bearer JWT | Marcar como entregada |
| `PATCH` | `/api/reservations/{id}/complete` | Bearer JWT | Completar la operación |
| `GET` | `/api/favorites` | Bearer JWT | Listar favoritos |
| `POST` | `/api/favorites/{publicationId}` | Bearer JWT | Agregar favorito |
| `DELETE` | `/api/favorites/{publicationId}` | Bearer JWT | Quitar favorito |
| `POST` | `/api/conversations` | Bearer JWT | Iniciar chat desde una publicación |
| `GET` | `/api/conversations` | Bearer JWT | Listar conversaciones propias |
| `GET` | `/api/conversations/{id}/messages` | Bearer JWT | Consultar mensajes autorizados |
| `POST` | `/api/conversations/{id}/messages` | Bearer JWT | Enviar mensaje |
| `POST` | `/api/reviews/providers` | Bearer JWT | Calificar proveedor tras interacción elegible |
| `POST` | `/api/reviews/products` | Bearer JWT | Calificar producto tras operación completada |
| `GET` | `/api/reputation/providers/{id}` | Bearer JWT | Resumen separado de reputación del proveedor |
| `GET` | `/api/reputation/products/{id}` | Bearer JWT | Resumen separado de reputación del producto |
| `GET` | `/api/analytics/me` | Bearer JWT | Métricas comerciales de la cuenta actual |

El listado acepta los parámetros `query`, `categoryId`, `productId`, `location`, `minPrice`, `maxPrice`,
`availableFrom`, `availableTo`, `minUnitsAvailable`, `page`, `size`, `sort` y `direction`. Los valores de `sort`
son `DATE_CREATED`, `PRICE` y `AVAILABLE_DATE`.

Ejemplo de registro:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "productor@example.com",
    "password": "password123",
    "fullName": "Productor Demo",
    "phone": "5555-1234",
    "location": "Guatemala"
  }'
```

La respuesta de registro y login contiene `accessToken`, `tokenType`, `expiresAt` y `user`. Para acceder al perfil:

```bash
curl http://localhost:8080/api/users/me \
  -H 'Authorization: Bearer EL_TOKEN_RECIBIDO'
```

## Pruebas

```bash
./gradlew test
```

La suite usa H2 únicamente durante las pruebas, en modo de compatibilidad PostgreSQL. El entorno de ejecución utiliza PostgreSQL real.

Las pruebas cubren el flujo comercial completo, aislamiento por propietario, restitución única de inventario,
elegibilidad de reseñas, seed reproducible y reservas concurrentes sin sobreventa.

## Alcance pospuesto

De acuerdo con el contrato mínimo del cronograma, este corte no incluye carga de imágenes en Supabase Storage,
pagos, notificaciones push, motor completo de precio sugerido ni administración avanzada. Las publicaciones ya
conservan referencias de catálogo y el diseño modular permite agregar esos componentes sin cambiar el flujo central.

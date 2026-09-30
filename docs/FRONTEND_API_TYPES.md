# AgroMarket - Contrato y tipos para frontend

Referencia práctica del contrato implementado por `agromarket-backend`. Está orientada al cliente Android con
Kotlin, Jetpack Compose, Retrofit y `kotlinx.serialization`, pero los nombres de campos y estructuras JSON aplican
igual para cualquier frontend.

> Fuente de verdad: el código actual del backend. Swagger/OpenAPI continúa disponible en `/swagger-ui.html`.

## 1. Convenciones

- Base URL local: `http://localhost:8080`.
- Android Emulator accediendo al backend de la computadora: `http://10.0.2.2:8080`.
- Todos los endpoints, excepto `/api/health` y `/api/auth/**`, requieren:

```http
Authorization: Bearer <accessToken>
```

- El cuerpo y las respuestas usan `application/json`.
- Los UUID se transportan como `String`.
- `Instant` se devuelve con zona UTC, por ejemplo `2026-09-30T18:10:00Z`.
- `LocalDate` usa `YYYY-MM-DD`, por ejemplo `2026-10-15`.
- `LocalDateTime` no incluye zona, por ejemplo `2026-09-30T14:10:00.123`.
- Los importes se reciben y devuelven como números JSON. En los DTO de red se usa `Double`; para cálculos locales
  de dinero conviene convertirlos a `BigDecimal`.
- Los nombres de enums son sensibles a mayúsculas y deben enviarse exactamente como aparecen aquí.

Tipos base recomendados:

```kotlin
typealias Uuid = String
typealias IsoInstant = String
typealias IsoLocalDate = String
typealias IsoLocalDateTime = String
typealias Money = Double
```

Imports utilizados por los ejemplos:

```kotlin
import kotlinx.serialization.Serializable
```

## 2. Enums

```kotlin
@Serializable
enum class PublicationStatus {
    ACTIVE,
    PAUSED,
    SOLD_OUT,
    CLOSED,
}

@Serializable
enum class PublicationSort {
    DATE_CREATED,
    PRICE,
    AVAILABLE_DATE,
}

@Serializable
enum class SortDirection {
    ASC,
    DESC,
}

@Serializable
enum class ReservationStatus {
    PENDING_PROVIDER,
    ACCEPTED_PENDING_PAYMENT,
    CONFIRMED,
    READY_FOR_DELIVERY,
    DELIVERED,
    COMPLETED,
    REJECTED,
    CANCELLED,
}
```

### Estados de publicación

| Estado | Significado |
|---|---|
| `ACTIVE` | Visible y admite nuevas reservas. |
| `PAUSED` | Oculta temporalmente y no admite reservas. |
| `SOLD_OUT` | Sin unidades reservables. Se asigna automáticamente. |
| `CLOSED` | Cerrada definitivamente. |

Transiciones manuales permitidas:

```text
ACTIVE ──► PAUSED ──► ACTIVE
   │          │
   └──────────┴─────► CLOSED

SOLD_OUT ───────────► CLOSED
```

`SOLD_OUT` no se solicita manualmente. El backend lo asigna cuando `unitsAvailable` llega a cero.

### Estados de reserva

```text
PENDING_PROVIDER
    ├── reject ──► REJECTED                 (restituye stock)
    ├── cancel ──► CANCELLED                (restituye stock)
    └── accept ──► ACCEPTED_PENDING_PAYMENT
                         ├── cancel ──► CANCELLED
                         └── confirm ──► CONFIRMED
                                             ├── cancel ──► CANCELLED
                                             └── ready ──► READY_FOR_DELIVERY
                                                               └── delivered ──► DELIVERED
                                                                                     └── complete ──► COMPLETED
```

## 3. Tipos comunes y errores

### Catálogos

```kotlin
@Serializable
data class CatalogItemDto(
    val id: Int,
    val code: String,
    val name: String,
)

@Serializable
data class CategoryDto(
    val id: Int,
    val code: String,
    val name: String,
    val products: List<CatalogItemDto>,
)
```

`CatalogItemDto` se reutiliza para producto, categoría resumida y unidad de medida.

### Problem Details

Los errores usan `application/problem+json`:

```kotlin
@Serializable
data class ApiProblem(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String? = null,
    val errors: Map<String, List<String>>? = null,
)
```

Ejemplo de validación:

```json
{
  "type": "about:blank",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "La solicitud contiene datos inválidos",
  "instance": "/api/publications",
  "errors": {
    "price": ["El precio debe ser mayor que cero"],
    "title": ["El título es obligatorio"]
  }
}
```

Códigos habituales:

| Código | Uso |
|---|---|
| `400` | JSON, parámetro, validación o transición de negocio inválida. |
| `401` | Falta el token, es inválido, expiró o la cuenta está deshabilitada. |
| `403` | El usuario está autenticado, pero el recurso pertenece a otra persona. |
| `404` | Recurso inexistente o publicación no visible públicamente. |
| `409` | Duplicado, integridad o conflicto concurrente. |

## 4. Salud

```kotlin
@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: IsoInstant,
)
```

| Método | Ruta | Request | Response |
|---|---|---|---|
| `GET` | `/api/health` | - | `HealthResponse` |

## 5. Autenticación y usuario

```kotlin
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String? = null,
    val location: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: IsoInstant,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Uuid,
    val email: String,
    val fullName: String,
    val phone: String?,
    val location: String?,
    val enabled: Boolean,
    val createdAt: IsoInstant,
    val updatedAt: IsoInstant,
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val location: String? = null,
)
```

Validaciones principales:

- `password`: entre 8 y 72 caracteres.
- `fullName`: máximo 120 caracteres.
- `phone`: máximo 30 caracteres.
- `location`: máximo 160 caracteres.
- El correo se normaliza a minúsculas.

En `UpdateProfileRequest`, un campo `null` conserva el valor actual. Para borrar `phone` o `location`, se puede
enviar una cadena vacía. `fullName` no acepta una cadena vacía.

| Método | Ruta | Request | Response |
|---|---|---|---|
| `POST` | `/api/auth/register` | `RegisterRequest` | `201 AuthResponse` |
| `POST` | `/api/auth/login` | `LoginRequest` | `200 AuthResponse` |
| `GET` | `/api/users/me` | - | `200 UserDto` |
| `PATCH` | `/api/users/me` | `UpdateProfileRequest` | `200 UserDto` |

## 6. Publicaciones y catálogos

```kotlin
@Serializable
data class PublicationRequest(
    val categoryId: Int,
    val productId: Int,
    val title: String,
    val description: String? = null,
    val price: Money,
    val units: Int,
    val unitTypeId: Int,
    val location: String? = null,
    val availableDate: IsoLocalDate? = null,
)

@Serializable
data class ChangePublicationStatusRequest(
    val status: PublicationStatus,
)

@Serializable
data class PublicationDto(
    val id: Uuid,
    val ownerId: Uuid,
    val ownerName: String?,
    val category: CatalogItemDto,
    val product: CatalogItemDto,
    val title: String,
    val description: String?,
    val price: Money,
    val currencyCode: String,
    val units: Int,
    val unitsAvailable: Int,
    val unitsCommitted: Int,
    val unitType: CatalogItemDto,
    val location: String?,
    val availableDate: IsoLocalDate?,
    val status: PublicationStatus,
    val createdAt: IsoLocalDateTime,
    val updatedAt: IsoLocalDateTime,
)

@Serializable
data class PublicationPageDto(
    val content: List<PublicationDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
```

Validaciones:

- `categoryId`, `productId` y `unitTypeId` deben existir.
- El producto debe pertenecer a la categoría enviada.
- `title`: obligatorio, máximo 100 caracteres.
- `description`: máximo 5000 caracteres.
- `price`: mínimo `0.01`, máximo 2 decimales.
- `units`: entero mayor que cero.
- `location`: máximo 100 caracteres.
- `availableDate`: hoy o una fecha futura.
- `PUT` es una edición completa: se deben enviar nuevamente todos los campos obligatorios.
- No se permite reducir `units` por debajo de `unitsCommitted`.

Consulta del catálogo:

| Método | Ruta | Response |
|---|---|---|
| `GET` | `/api/categories` | `List<CategoryDto>` |
| `GET` | `/api/unit-types` | `List<CatalogItemDto>` |

Publicaciones:

| Método | Ruta | Request | Response |
|---|---|---|---|
| `GET` | `/api/publications` | Query params | `PublicationPageDto` |
| `GET` | `/api/publications/{id}` | - | `PublicationDto` |
| `GET` | `/api/publications/mine` | Query params | `PublicationPageDto` |
| `POST` | `/api/publications` | `PublicationRequest` | `201 PublicationDto` |
| `PUT` | `/api/publications/{id}` | `PublicationRequest` | `200 PublicationDto` |
| `PATCH` | `/api/publications/{id}/status` | `ChangePublicationStatusRequest` | `200 PublicationDto` |

`GET /api/publications` y el detalle muestran únicamente publicaciones `ACTIVE`. `/mine` incluye todos los estados
del usuario autenticado.

Modelo recomendado para formar los query params:

```kotlin
data class PublicationQuery(
    val query: String? = null,
    val categoryId: Int? = null,
    val productId: Int? = null,
    val location: String? = null,
    val minPrice: Money? = null,
    val maxPrice: Money? = null,
    val availableFrom: IsoLocalDate? = null,
    val availableTo: IsoLocalDate? = null,
    val minUnitsAvailable: Int? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: PublicationSort = PublicationSort.DATE_CREATED,
    val direction: SortDirection = SortDirection.DESC,
)
```

`size` debe estar entre 1 y 100.

## 7. Reservas e inventario

```kotlin
@Serializable
data class CreateReservationRequest(
    val publicationId: Uuid,
    val quantity: Int,
)

@Serializable
data class ReservationDto(
    val id: Uuid,
    val publicationId: Uuid,
    val publicationTitle: String,
    val productId: Int,
    val productName: String,
    val buyerId: Uuid,
    val buyerName: String,
    val providerId: Uuid,
    val providerName: String,
    val quantity: Int,
    val unitPrice: Money,
    val total: Money,
    val currencyCode: String,
    val expectedAvailableDate: IsoLocalDate?,
    val status: ReservationStatus,
    val createdAt: IsoLocalDateTime,
    val updatedAt: IsoLocalDateTime,
)
```

Al crear una reserva, el backend descuenta inmediatamente `quantity` de `unitsAvailable`. Un rechazo o cancelación
válida devuelve esa cantidad exactamente una vez. El precio unitario queda congelado en la reserva aunque después
cambie la publicación.

| Método | Ruta | Actor | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/reservations` | Comprador | `CreateReservationRequest` | `201 ReservationDto` |
| `GET` | `/api/reservations/mine` | Comprador | - | `List<ReservationDto>` |
| `GET` | `/api/reservations/received` | Proveedor | - | `List<ReservationDto>` |
| `PATCH` | `/api/reservations/{id}/accept` | Proveedor | Sin body | `ReservationDto` |
| `PATCH` | `/api/reservations/{id}/reject` | Proveedor | Sin body | `ReservationDto` |
| `PATCH` | `/api/reservations/{id}/cancel` | Participante | Sin body | `ReservationDto` |
| `PATCH` | `/api/reservations/{id}/confirm` | Comprador | Sin body | `ReservationDto` |
| `PATCH` | `/api/reservations/{id}/ready` | Proveedor | Sin body | `ReservationDto` |
| `PATCH` | `/api/reservations/{id}/delivered` | Proveedor | Sin body | `ReservationDto` |
| `PATCH` | `/api/reservations/{id}/complete` | Comprador | Sin body | `ReservationDto` |

No se puede reservar una publicación propia, una publicación no activa, cantidades no positivas ni más unidades de
las disponibles.

## 8. Favoritos

No hay un DTO adicional: el listado devuelve publicaciones completas.

| Método | Ruta | Response |
|---|---|---|
| `GET` | `/api/favorites` | `List<PublicationDto>` |
| `POST` | `/api/favorites/{publicationId}` | `204 No Content` |
| `DELETE` | `/api/favorites/{publicationId}` | `204 No Content` |

Agregar o eliminar es idempotente. El listado solo devuelve publicaciones actualmente activas.

## 9. Conversaciones y mensajes

```kotlin
@Serializable
data class StartConversationRequest(
    val publicationId: Uuid,
)

@Serializable
data class SendMessageRequest(
    val content: String,
)

@Serializable
data class ConversationDto(
    val id: Uuid,
    val publicationId: Uuid,
    val publicationTitle: String,
    val buyerId: Uuid,
    val buyerName: String,
    val providerId: Uuid,
    val providerName: String,
    val createdAt: IsoLocalDateTime,
    val updatedAt: IsoLocalDateTime,
)

@Serializable
data class MessageDto(
    val id: Uuid,
    val senderId: Uuid,
    val senderName: String,
    val content: String,
    val createdAt: IsoLocalDateTime,
)
```

| Método | Ruta | Request | Response |
|---|---|---|---|
| `POST` | `/api/conversations` | `StartConversationRequest` | `201 ConversationDto` |
| `GET` | `/api/conversations` | - | `List<ConversationDto>` |
| `GET` | `/api/conversations/{id}/messages` | - | `List<MessageDto>` |
| `POST` | `/api/conversations/{id}/messages` | `SendMessageRequest` | `201 MessageDto` |

Reglas:

- La conversación solo puede iniciarse desde una publicación activa.
- El propietario no puede iniciar una conversación consigo mismo.
- Para una misma combinación publicación/comprador/proveedor se reutiliza la conversación existente.
- Solo comprador y proveedor pueden leer o enviar mensajes.
- `content` es obligatorio y admite hasta 2000 caracteres.

## 10. Reseñas y reputación

```kotlin
@Serializable
data class SupplierReviewRequest(
    val conversationId: Uuid,
    val purchased: Boolean,
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class ProductReviewRequest(
    val reservationId: Uuid,
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class SupplierReviewDto(
    val id: Uuid,
    val reviewerId: Uuid,
    val reviewerName: String,
    val purchased: Boolean,
    val rating: Int,
    val comment: String?,
    val createdAt: IsoLocalDateTime,
)

@Serializable
data class ProductReviewDto(
    val id: Uuid,
    val reviewerId: Uuid,
    val reviewerName: String,
    val productId: Int,
    val productName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: IsoLocalDateTime,
)

@Serializable
data class ReviewSummaryDto<T>(
    val average: Double?,
    val count: Int,
    val reviews: List<T>,
)

typealias SupplierReputationDto = ReviewSummaryDto<SupplierReviewDto>
typealias ProductReputationDto = ReviewSummaryDto<ProductReviewDto>
```

| Método | Ruta | Request | Response |
|---|---|---|---|
| `POST` | `/api/reviews/providers` | `SupplierReviewRequest` | `201 SupplierReviewDto` |
| `POST` | `/api/reviews/products` | `ProductReviewRequest` | `201 ProductReviewDto` |
| `GET` | `/api/reputation/providers/{supplierId}` | - | `SupplierReputationDto` |
| `GET` | `/api/reputation/products/{productId}` | - | `ProductReputationDto` |

Reglas:

- `rating` siempre está entre 1 y 5.
- `comment` admite hasta 1000 caracteres.
- Proveedor: solo el comprador puede reseñar y debe existir al menos un mensaje de cada participante.
- Producto: solo el comprador puede reseñar y la reserva debe estar `COMPLETED`.
- Solo se admite una reseña de proveedor por conversación y una reseña de producto por reserva.
- `average` es `null` cuando todavía no existen reseñas.

## 11. Analítica

```kotlin
@Serializable
data class UserAnalyticsDto(
    val activePublications: Long,
    val reservationsMade: Long,
    val reservationsReceived: Long,
    val unitsCommitted: Long,
    val completedOperations: Long,
    val completedVolume: Money,
    val reservationsByStatus: Map<String, Long>,
)
```

| Método | Ruta | Response |
|---|---|---|
| `GET` | `/api/analytics/me` | `UserAnalyticsDto` |

`reservationsByStatus` usa los nombres de `ReservationStatus` como claves. Todas las métricas corresponden
exclusivamente al usuario autenticado.

Ejemplo:

```json
{
  "activePublications": 4,
  "reservationsMade": 3,
  "reservationsReceived": 8,
  "unitsCommitted": 27,
  "completedOperations": 2,
  "completedVolume": 1450.00,
  "reservationsByStatus": {
    "PENDING_PROVIDER": 2,
    "COMPLETED": 2,
    "REJECTED": 1
  }
}
```

## 12. Mapa rápido endpoint → tipo

| Endpoint | Request | Success response |
|---|---|---|
| `POST /api/auth/register` | `RegisterRequest` | `AuthResponse` |
| `POST /api/auth/login` | `LoginRequest` | `AuthResponse` |
| `GET /api/users/me` | - | `UserDto` |
| `PATCH /api/users/me` | `UpdateProfileRequest` | `UserDto` |
| `GET /api/categories` | - | `List<CategoryDto>` |
| `GET /api/unit-types` | - | `List<CatalogItemDto>` |
| `GET /api/publications` | `PublicationQuery` | `PublicationPageDto` |
| `GET /api/publications/{id}` | - | `PublicationDto` |
| `GET /api/publications/mine` | `PublicationQuery` | `PublicationPageDto` |
| `POST /api/publications` | `PublicationRequest` | `PublicationDto` |
| `PUT /api/publications/{id}` | `PublicationRequest` | `PublicationDto` |
| `PATCH /api/publications/{id}/status` | `ChangePublicationStatusRequest` | `PublicationDto` |
| `POST /api/reservations` | `CreateReservationRequest` | `ReservationDto` |
| `GET /api/reservations/mine` | - | `List<ReservationDto>` |
| `GET /api/reservations/received` | - | `List<ReservationDto>` |
| Acciones `/api/reservations/{id}/...` | Sin body | `ReservationDto` |
| `GET /api/favorites` | - | `List<PublicationDto>` |
| `POST/DELETE /api/favorites/{id}` | Sin body | `204 No Content` |
| `POST /api/conversations` | `StartConversationRequest` | `ConversationDto` |
| `GET /api/conversations` | - | `List<ConversationDto>` |
| `GET /api/conversations/{id}/messages` | - | `List<MessageDto>` |
| `POST /api/conversations/{id}/messages` | `SendMessageRequest` | `MessageDto` |
| `POST /api/reviews/providers` | `SupplierReviewRequest` | `SupplierReviewDto` |
| `POST /api/reviews/products` | `ProductReviewRequest` | `ProductReviewDto` |
| `GET /api/reputation/providers/{id}` | - | `SupplierReputationDto` |
| `GET /api/reputation/products/{id}` | - | `ProductReputationDto` |
| `GET /api/analytics/me` | - | `UserAnalyticsDto` |

## 13. Recomendación para Retrofit

El interceptor debe agregar el token únicamente cuando exista:

```kotlin
class AuthInterceptor(
    private val tokenProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}
```

Ejemplo parcial del servicio:

```kotlin
interface AgroMarketApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("api/publications")
    suspend fun publications(
        @Query("query") query: String? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("productId") productId: Int? = null,
        @Query("location") location: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: PublicationSort = PublicationSort.DATE_CREATED,
        @Query("direction") direction: SortDirection = SortDirection.DESC,
    ): PublicationPageDto

    @POST("api/reservations")
    suspend fun createReservation(@Body request: CreateReservationRequest): ReservationDto

    @PATCH("api/reservations/{id}/accept")
    suspend fun acceptReservation(@Path("id") id: Uuid): ReservationDto

    @GET("api/conversations/{id}/messages")
    suspend fun messages(@Path("id") id: Uuid): List<MessageDto>

    @POST("api/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: Uuid,
        @Body request: SendMessageRequest,
    ): MessageDto
}
```

Para respuestas no exitosas, deserializar `errorBody()` como `ApiProblem` y mostrar `detail`; cuando `errors` no sea
`null`, puede asociarse cada lista de mensajes al campo correspondiente del formulario.

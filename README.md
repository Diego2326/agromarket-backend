# AgroMarket Backend

API REST en Kotlin, Spring Boot y PostgreSQL. Los módulos implementados actualmente son autenticación y usuarios.

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

Flyway aplica automáticamente `V1__create_users.sql`. Hibernate está configurado en modo `validate`, por lo que no altera el esquema.

Swagger queda disponible en `http://localhost:8080/swagger-ui.html`.

## Endpoints

| Método | Ruta | Autenticación | Uso |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Registrar una cuenta y obtener JWT |
| `POST` | `/api/auth/login` | No | Iniciar sesión |
| `GET` | `/api/users/me` | Bearer JWT | Consultar el perfil actual |
| `PATCH` | `/api/users/me` | Bearer JWT | Actualizar nombre, teléfono o ubicación |

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

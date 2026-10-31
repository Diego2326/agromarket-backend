# Despliegue: Cloud Run y Neon

La API admite WebSocket en Cloud Run. Neon conserva PostgreSQL; la API usa una conexión JDBC y otra
conexión directa para `LISTEN/NOTIFY`, que distribuye mensajes entre instancias. El proyecto Neon actual
tiene el pooler desactivado, así que por ahora `DB_URL`, `DB_LISTENER_URL` y `DB_MIGRATION_URL` usan el host
directo. Si se activa el pooler, cambia solo `DB_URL` al host `-pooler`.

## Preparar Neon

Usa un proyecto y una base dedicados a AgroMarket. Copia el host agrupado (`-pooler`) y el host directo de la
misma rama. La forma JDBC para cada host es:

El proyecto dedicado ya está creado: `AgroMarket` (`crimson-cake-63353950`), región `aws-us-east-2`, rama
`main`, base `agromarket` y rol `agromarket_app`.

```text
jdbc:postgresql://HOST/BASE?sslmode=require
```

`DB_URL` usa el host `-pooler` si está habilitado; de lo contrario, usa el host directo. `DB_LISTENER_URL` y
`DB_MIGRATION_URL` siempre usan el host directo: PostgreSQL `LISTEN` y las migraciones Flyway requieren una
sesión estable. `DB_USERNAME` y
`DB_PASSWORD` son el rol y contraseña de Neon. Flyway aplica las migraciones al arrancar la API.

## Guardar secretos

Selecciona el proyecto de Google Cloud que alojará AgroMarket y habilita Cloud Run, Cloud Build, Artifact
Registry y Secret Manager. Crea estos secretos en Secret Manager con los valores de Neon o un JWT aleatorio:

- `agromarket-db-url`: JDBC agrupada.
- `agromarket-db-listener-url`: JDBC directa.
- `agromarket-db-username` y `agromarket-db-password`.
- `agromarket-jwt-secret`: al menos 32 caracteres aleatorios.

Concede `roles/secretmanager.secretAccessor` a la cuenta de servicio de ejecución de Cloud Run. No pongas
credenciales en `gradle.properties`, el repositorio ni los argumentos de despliegue.

## Desplegar

Desde la raíz del backend, con el proyecto y región correctos seleccionados:

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com

gcloud run deploy agromarket-api \
  --source . \
  --project eventflow-509015 \
  --region us-central1 \
  --service-account agromarket-api-runtime@eventflow-509015.iam.gserviceaccount.com \
  --allow-unauthenticated \
  --port 8080 \
  --timeout 3600s \
  --max 3 \
  --set-env-vars JWT_ISSUER=agromarket-api,JWT_EXPIRATION_SECONDS=86400,DEMO_DATA_ENABLED=false \
  --set-secrets DB_URL=agromarket-db-url:latest,DB_LISTENER_URL=agromarket-db-listener-url:latest,DB_MIGRATION_URL=agromarket-db-listener-url:latest,DB_USERNAME=agromarket-db-username:latest,DB_PASSWORD=agromarket-db-password:latest,JWT_SECRET=agromarket-jwt-secret:latest
```

La cuenta de servicio necesita permiso de lectura para esos secretos. Comprueba
`https://URL_DEL_SERVICIO.run.app/api/health`; configura Android e iOS con la URL HTTPS publicada.

## Contrato WebSocket

Los clientes abren `wss://HOST/ws/conversations/{conversationId}` con `Authorization: Bearer <JWT>`. El
servidor valida que la cuenta esté activa y participe en la conversación. El cliente envía
`{"content":"…"}` y recibe un evento inicial `connected`, seguido por eventos como:

```json
{"type":"message","conversationId":"…","message":{"id":"…","senderId":"…","senderName":"…","content":"…","createdAt":"…"}}
```

Los mensajes REST también se publican por WebSocket. El backend guarda el mensaje, luego llama `pg_notify`;
cada instancia recibe la notificación por la conexión directa de Neon y la entrega a sus sockets. El límite
inicial de tres instancias mantiene acotadas las conexiones directas de Neon. REST sigue
disponible para cargar el historial y recuperar reconexiones. Cloud Run limita cada conexión por el timeout
configurado; los clientes vuelven a conectar y cargan el historial tras un corte.

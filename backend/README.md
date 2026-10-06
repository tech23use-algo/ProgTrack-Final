# OpenSpace Skill Planner API

Assumption: `userId`, `planId`, and `taskId` are UUIDs.

Spring Boot 3 / Java 21 backend for generating, storing, and tracking skill-learning plans. PostgreSQL is used in production; the test profile uses an in-memory H2 database.

## Requirements and configuration

- Java 21
- Maven 3.9+
- PostgreSQL 14+
- A Groq API key supplied only through `GROQ_API_KEY`

Copy `.env.example` to `.env` for local reference and export its values in your shell or deployment environment. Spring Boot does not automatically load `.env` files. Do not commit `.env`.

| Variable | Purpose | Default |
| --- | --- | --- |
| `GROQ_API_KEY` | Secret Groq API credential | unset; generation fails safely |
| `GROQ_MODEL` | Groq model | `llama-3.3-70b-versatile` |
| `GROQ_MAX_TOKENS` | Maximum generated tokens | `8192` |
| `DB_URL` | JDBC connection | `jdbc:postgresql://localhost:5432/openspace` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `openspace` / `openspace` |
| `OPENSPACE_API_KEY` | Optional shared `X-API-Key` gate | unset (authentication stub disabled) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated CORS origin patterns | `*` |

For a deployment, set `OPENSPACE_API_KEY` to a long random value and restrict CORS to trusted origins. The shared-key interceptor is a lightweight integration stub, not user identity authentication; do not use a key embedded in an Android app as proof of an individual user's identity. Bind the `userId` routes to a verified user token before treating client-supplied IDs as authorization.

The Groq API key is never returned by this service and is never included in logs. Groq model, temperature, token limit, and 30-second timeout are configured in `src/main/resources/application.yml`. Flyway applies the PostgreSQL schema migration at startup.

## Run locally

Start PostgreSQL and create the configured database, then run:

```sh
cd backend
export GROQ_API_KEY=your-groq-api-key
export DB_URL=jdbc:postgresql://localhost:5432/openspace
export DB_USERNAME=openspace
export DB_PASSWORD=your-database-password
mvn spring-boot:run
```

When `OPENSPACE_API_KEY` is configured, send it as an `X-API-Key` header with each `/api/**` request. Validation happens in the API before calling Groq. A rejected skill returns HTTP 422; generation is limited to five attempts per user per hour per running application instance.

## API examples

Create a learning plan:

```sh
curl -X POST http://localhost:8080/api/skill-plans \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-shared-api-key" \
  -d '{
    "userId": "68c05688-cfe2-40fc-bd50-82f04ca52593",
    "skill": "Java programming",
    "days": 14,
    "hoursPerDay": 1.5
  }'
```

Fetch a plan and its task/time progress:

```sh
curl http://localhost:8080/api/skill-plans/PLAN_UUID \
  -H "X-API-Key: your-shared-api-key"
```

List plans for a user:

```sh
curl http://localhost:8080/api/users/68c05688-cfe2-40fc-bd50-82f04ca52593/skill-plans \
  -H "X-API-Key: your-shared-api-key"
```

Mark a task complete or incomplete:

```sh
curl -X PATCH http://localhost:8080/api/skill-plans/PLAN_UUID/tasks/TASK_UUID \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-shared-api-key" \
  -d '{"completed":true}'
```

Delete a plan:

```sh
curl -X DELETE http://localhost:8080/api/skill-plans/PLAN_UUID \
  -H "X-API-Key: your-shared-api-key"
```

Errors use the same body shape, for example:

```json
{"code":"VALIDATION_ERROR","message":"days: must be less than or equal to 365"}
```

## Tests

```sh
cd backend
mvn test
```

The unit tests cover request validation and plan creation, retry, rejection, and safe failure paths. `GroqClientIntegrationTest` exercises the real HTTP client against MockWebServer for valid JSON, `REJECTED`, malformed JSON, and HTTP 429 with `Retry-After`.

## Docker

Build and run the container from this directory:

```sh
docker build -t openspace-skill-planner .
docker run --rm -p 8080:8080 \
  -e GROQ_API_KEY=your-groq-api-key \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/openspace \
  -e DB_USERNAME=openspace \
  -e DB_PASSWORD=your-database-password \
  -e OPENSPACE_API_KEY=your-shared-api-key \
  openspace-skill-planner
```

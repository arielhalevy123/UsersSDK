# Server and REST API

[← Documentation home](index.md)

## Where the server runs

The SDK needs a UsersSDK server. Two options:

**Hosted:** `https://userssdk-api-production.up.railway.app/` (live on Railway; see [RAILWAY_DEPLOY.md](RAILWAY_DEPLOY.md)). Health check: `GET /actuator/health`.

**Self-host with Docker Compose** (PostgreSQL + server):

```bash
git clone https://github.com/arielhalevy123/UsersSDK.git && cd UsersSDK
cp .env.example .env        # fill in DB_USER, DB_PASSWORD and JWT_SECRET (openssl rand -base64 32)
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

The Admin Portal is at `/` and interactive API docs at `/swagger-ui.html`.
Server environment variables:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | **Required.** At least 32 bytes. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Datasource (`jdbc:postgresql://...`). |
| `DATABASE_URL` or `PGHOST`/`PGPORT`/`PGUSER`/`PGPASSWORD`/`PGDATABASE` | Alternative used by Railway and similar hosts; converted to a jdbc URL automatically. |
| `PORT` | HTTP port (default 8080). |
| `SEED_DEMO_DATA` | `true` (default) creates the demo accounts below. Set `false` on public servers. |
| `CORS_ALLOWED_ORIGINS` | Extra browser origins, comma-separated. |

Run without Docker: start PostgreSQL, export the variables above, then `./gradlew bootRun`.

## Endpoints

REST API under `/api`. Authenticated calls need `Authorization: Bearer <token>` (the SDK does
this for you).

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/register` | No | Register, returns JWT + user |
| POST | `/api/auth/login` | No | Login, returns JWT + user |
| GET | `/api/auth/me` | Yes | Current user |
| GET | `/api/auth/my-admin` | Yes | The current user's admin (or self, for an admin) |
| GET | `/api/auth/my-users` | Yes | Users I manage / my group |
| GET | `/api/auth/admins` | No | Admins to choose from at registration (id + name only) |
| GET | `/api/auth/all` | Admin | All users with their fields |
| GET | `/api/auth/admin/{adminId}/users` | Yes (that admin) | Users of a given admin |
| PUT | `/api/auth/users/{id}` | Yes (self or admin) | Update name, email and custom fields |
| POST | `/api/admin/users/{id}/fields` | Yes | Add a custom field |
| GET | `/api/admin/users/{id}/fields` | Yes | List custom fields |
| PUT | `/api/admin/fields/{id}` | Yes | Update a custom field |
| DELETE | `/api/admin/fields/{id}` | Yes | Delete a custom field |
| GET | `/actuator/health` | No | Health check |

Full, browsable reference: `/swagger-ui.html` on any running server.

## Security

- Passwords are hashed with BCrypt; tokens are JWT, valid for 24 hours.
- `GET /api/auth/all` is admin-only (it used to list every user to anyone; fixed).
- CORS and HTTPS: the hosted server only serves HTTPS.

## Running it locally

### Backend

```bash
./gradlew test       # unit + integration tests (H2, no database needed)
./gradlew bootRun    # needs PostgreSQL and the variables above
```

With Docker: copy `.env.example` to `.env`, fill it in, then `docker compose up -d`. The API is
then at `http://localhost:8080/` and the admin portal at the same address in a browser.

## Deployment

Every merge to `main` that passes CI is deployed to Railway (project `userssdk`, service
`userssdk-api`) by the *Deploy server* workflow, which then waits for `/actuator/health` to
report `UP`. See [RAILWAY_DEPLOY.md](https://github.com/arielhalevy123/UsersSDK/blob/main/RAILWAY_DEPLOY.md).

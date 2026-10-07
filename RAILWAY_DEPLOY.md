# Deploying the UsersSDK server to Railway

Status: **not deployed yet.** These are the steps to run once Ariel approves. Nothing here has been
executed against Railway.

What you get: the Spring Boot API + Admin Portal on `https://<something>.up.railway.app`, backed by a
Railway PostgreSQL database. HTTPS is provided by Railway, so the Android SDK works with no cleartext
exceptions.

## What the server already handles

| Concern | How |
|---|---|
| Port | `server.port=${PORT:8080}`; Railway injects `PORT`. |
| Database | `DATABASE_URL` (`postgresql://user:pass@host:port/db`) or `PGHOST/PGPORT/PGUSER/PGPASSWORD/PGDATABASE` are converted to a `jdbc:` URL at startup (`RailwayDatabaseEnvironmentPostProcessor`). An explicit `DB_URL` / `SPRING_DATASOURCE_URL` wins. |
| JWT secret | `JWT_SECRET` (required; the app refuses to start without a 32+ byte key). |
| Health check | `GET /actuator/health` returns `{"status":"UP"}`; set as the Railway health check in `railway.json`. |
| Build | `Dockerfile` is multi-stage (builds the jar inside Docker); `railway.json` tells Railway to use it. |
| Demo accounts | `SEED_DEMO_DATA=false` disables the README demo users (admin@example.com / admin123). |
| CORS | `CORS_ALLOWED_ORIGINS` (comma-separated). The Admin Portal is same-origin and needs nothing. |

## One-time setup

```bash
# 1. Install and log in to the Railway CLI
brew install railway            # or: npm i -g @railway/cli
railway login

# 2. From the repository root
cd ~/Desktop/software_engnearing/year_c/seminar/UsersSDK

# 3. Create the project and link this folder to it
railway init --name userssdk

# 4. Add PostgreSQL
railway add --database postgres

# 5. Create the (empty) service for the API
railway add --service userssdk-api

# 6. Set variables on the API service.
#    ${{Postgres.DATABASE_URL}} is a Railway reference: it resolves to the Postgres service's URL
#    over the private network. Single quotes stop your shell from expanding it.
railway variables --service userssdk-api \
  --set 'DATABASE_URL=${{Postgres.DATABASE_URL}}' \
  --set "JWT_SECRET=$(openssl rand -base64 48)" \
  --set "SEED_DEMO_DATA=false"
#    (The JWT secret is generated in place and never printed. Keep it only in Railway.)
#    Optional: --set "CORS_ALLOWED_ORIGINS=https://your-frontend.example"

# 7. Build and deploy from the local folder (uses Dockerfile + railway.json)
railway up --service userssdk-api --detach

# 8. Give it a public HTTPS domain
railway domain --service userssdk-api
#    -> prints https://userssdk-api-production-xxxx.up.railway.app
```

If the Postgres service ended up with a different name than `Postgres`, use that name in the
reference (`${{<name>.DATABASE_URL}}`). `railway status` and the dashboard show it.

## Verify

```bash
DOMAIN=https://<railway-domain>          # from step 8
curl -s $DOMAIN/actuator/health          # {"status":"UP"}
curl -s -X POST $DOMAIN/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Smoke","email":"smoke@test.io","password":"change-me-123","role":"ADMIN"}'
railway logs --service userssdk-api      # look for "Tomcat started on port"
```

Then open `$DOMAIN/` for the Admin Portal and `$DOMAIN/swagger-ui.html` for the API docs.

## Point the apps at it

Put the domain in one place, with a trailing slash:

- **Demo apps** (`UsersSdkAndroid/app`, `barberapp`): either replace the default in
  `UsersSdkAndroid/build.gradle.kts` (`https://<railway-domain>/`) or build with
  `./gradlew assembleDebug -PusersSdkBaseUrl=https://<railway-domain>/`.
- **README**: replace `https://<railway-domain>/` in the Server section.

## Redeploying

```bash
railway up --service userssdk-api --detach
```

Or connect the GitHub repo to the service in the Railway dashboard (Settings, Source) so every push to
`main` deploys automatically.

## Notes and costs

- Railway bills by usage; a small Spring Boot app plus Postgres typically fits the Hobby plan.
  Check the current pricing before leaving it running.
- `spring.jpa.hibernate.ddl-auto=update` creates/updates tables on startup. Fine for this project;
  a production system would use migrations (Flyway/Liquibase).
- Do not set `SEED_DEMO_DATA=true` on the public server unless you want the README's admin
  credentials to work for everyone (for example during a live demo), and turn it off afterwards.
  Note that seeded users stay in the database once created.

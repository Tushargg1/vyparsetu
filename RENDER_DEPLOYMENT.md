# Render deployment

The repository includes a Render Blueprint in `render.yaml`. It deploys the Spring Boot API from `backend/Dockerfile` and uses `/actuator/health` for health checks.

## Create the service

1. Rotate any database password that has been shared outside the database provider.
2. In Render, create a **Blueprint** from `Tushargg1/vyparsetu`.
3. Enter the three secret database values when prompted; never commit them.

| Variable | Value |
| --- | --- |
| `DB_URL` | `jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED&serverTimezone=UTC` |
| `DB_USERNAME` | Managed MySQL application username |
| `DB_PASSWORD` | Newly rotated managed MySQL password |

The Blueprint generates `JWT_SECRET`, allows the production Vercel origin, activates the `render` profile, limits the database pool, and configures JVM memory ergonomics.

## Verify after deployment

```powershell
curl.exe https://<render-service>.onrender.com/actuator/health
```

Expected response: HTTP 200 with `{"status":"UP"}`.

Verify CORS:

```powershell
curl.exe -i -X OPTIONS https://<render-service>.onrender.com/api/v1/auth/otp/send `
  -H "Origin: https://vyparsetu.vercel.app" `
  -H "Access-Control-Request-Method: POST" `
  -H "Access-Control-Request-Headers: Content-Type,Authorization"
```

## Operational requirements

- The current application requires MySQL 8; Render PostgreSQL is not compatible with its Flyway migrations.
- Configure a real SMS/email `OtpSender` before public production use. The repository only includes development logging senders.
- `/tmp` storage is ephemeral. Attach persistent storage or implement object storage before relying on generated invoice documents across deploys.
- If Render assigns a URL other than `https://vyaparsetu-api.onrender.com`, update Vercel's `VITE_API_BASE_URL` and redeploy the frontend.

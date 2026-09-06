# Northflank deployment

Use the Sandbox/free resources only. Check the displayed cost and available
memory before creating resources; do not select a paid compute plan to follow
this guide. Java plus Chromium must be tested against the actual free memory
limit. The Dockerfile caps Java heap at 40% of container memory, but this does
not cap total Java memory or reserve enough memory for Chromium automatically.

## 1. Create the project and database

Create a project named `aapli-chawdi`. Add a MySQL addon using the free database
option if available in your account. Enable TLS at creation and leave public
database access disabled. Use the standard database user rather than admin
credentials for the application.

Record the internal host, port, database name, username, and password from the
addon's connection details. Use the database name actually provided; the cloud
database need not be called `chawli`.

To preserve existing villages, subscriptions, notices, and sessions, export the
local MySQL database and import it into the addon using Northflank's supported
import workflow or CLI port forwarding. Keep dumps outside the Git repository.
An empty database is also possible, but requires village data population and
new subscriptions. Hibernate creates/updates tables, not the database itself.

## 2. Build the application

Push `Dockerfile`, `.dockerignore`, and this guide to the GitHub branch you plan
to deploy. Connect that repository to Northflank and create a **combined**
service (build and deploy) called `aapli-chawdi`:

- Build method: Dockerfile
- Dockerfile path: `/Dockerfile`
- Build context: repository root (`/`)
- Instances: one, using free resources
- Start command: use the Dockerfile default

The image contains Playwright browsers and their OS dependencies. Its pinned
Playwright version must match `pom.xml`. Runtime secrets are not needed during
the build and must not be supplied as Docker build arguments.

## 3. Runtime variables

Configure these as service runtime variables or linked secrets:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://INTERNAL_HOST:PORT/DATABASE?sslMode=REQUIRED` |
| `SPRING_DATASOURCE_USERNAME` | Standard addon username |
| `DB_PASSWORD` | Addon password |
| `TELEGRAM_BOT_TOKEN` | Existing bot token |
| `GOOGLE_API_KEY` | Existing Gemini key |

Replace every JDBC placeholder with the addon connection details. Do not paste
a `mysql://` URL into the JDBC setting. `sslMode=REQUIRED` requires encryption;
for server certificate verification, configure the addon CA trust and use
`VERIFY_IDENTITY` with a matching hostname.

The Dockerfile already enables headless mode, disables slow motion and SQL
logging, limits the connection pool to five, and sets the JVM timezone to
Asia/Kolkata. Port remains 9999. No extra Northflank cron jobs are required:
the existing application schedules run inside the single service instance.

## 4. Verify startup before exposing the bot

Look for successful database connection, Tomcat startup on 9999, and
`Started AapliChawdiApplication`. Use a TCP health check on port 9999 initially;
there is no HTTP health endpoint at `/` in this application.

Before allowing public access, restrict public routing to
`/api/telegram/webhook` and add Telegram webhook-secret validation. The current
application does not authenticate its administrative APIs or validate Telegram
webhook secrets. Publishing every route exposes subscription and scrape APIs.
These protections are still outstanding; the Dockerfile does not add them.

Once those protections are in place, configure a public HTTP port for container
port **9999** in Ports & DNS. Northflank supplies an HTTPS hostname. Register:

```bash
curl -sS -X POST \
  "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  --data-urlencode "url=https://YOUR-NORTHFLANK-HOST/api/telegram/webhook" \
  --data-urlencode 'allowed_updates=["message","callback_query"]'
```

If webhook-secret validation is implemented, include its matching
`secret_token` in registration as well. Registering the new URL switches the
existing bot away from ngrok. Stop the local application before enabling the
cloud application against the same production data to avoid duplicate jobs.

## 5. Verify the complete workflow

- Send `/cancel`, then `/start`, and verify the incoming update and reply logs.
- Verify district/taluka selection using imported or newly populated data.
- Run a check for one village through a protected administrative route.
- Inspect memory usage while Chromium is active, and confirm a screenshot and
  summary are delivered. A successful startup alone does not prove scraping fits.
- Restart the service and confirm database records persist.
- Confirm the scheduled jobs use India time and configure database backups.

If memory is exhausted, do not silently upgrade to paid resources. The next
decision is separating the scraper or selecting a different free host.

## References

- [Northflank build and deploy](https://northflank.com/docs/v1/application/getting-started/build-and-deploy-your-code)
- [Northflank MySQL](https://northflank.com/docs/v1/application/databases-and-persistence/deploy-databases-on-northflank/deploy-mysql-on-northflank)
- [Playwright Java Docker](https://playwright.dev/java/docs/docker)

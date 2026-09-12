# 🌾 Aapli Chawdi Digital Alert

A Telegram-based land mutation notice monitor for Maharashtra's [Aapli Chawdi portal](https://digitalsatbara.mahabhumi.gov.in/aaplichawdi). Users subscribe to villages, receive AI-generated English summaries and notice screenshots, and can opt into objection-deadline reminders.

The application checks subscribed villages **daily at 9 AM**, or when manually triggered. It does not continuously monitor the portal, and subscribing does not immediately trigger a notice check.

## Architecture

```text
Telegram user
    │ message or inline button callback
    ▼
Telegram servers
    │ POST to registered public HTTPS webhook
    ▼
ngrok / reverse proxy → Spring Boot on port 9999
    │
    ├── TelegramWebhookController → TelegramBotService
    │                               ├── subscriptions and user sessions → MySQL
    │                               └── TelegramClient → Telegram replies
    │
    ├── Daily scheduler / manual notice API
    │       └── NoticeOrchestrationService
    │               ├── Playwright scraper → Aapli Chawdi portal
    │               ├── Gemini → captcha reading and notice summaries
    │               ├── NoticeProcessingService → MySQL and Telegram alerts
    │               └── NoticeCleanupService
    │
    └── ReminderScheduler → Telegram deadline reminders
```

Telegram is the user interface; there is no separate web frontend. Incoming updates use webhooks, not polling. The application does not register its webhook automatically.

## Features and bot flow

- Subscribe by selecting district and taluka using inline buttons, then typing a village name.
- Match English transliterations, Marathi input, and minor spelling variations against the stored village list using Gemini.
- List and remove subscriptions.
- Receive a screenshot and English summary when a new notice is processed for a subscribed village.
- Browse stored notices by village, choose active or expired notices, and open their details.
- Opt into deadline reminders for notices with a future objection deadline.
- Reset the conversation using `/cancel` or **🔄 Refresh**.

Main-menu keyboard buttons send ordinary text messages. Inline buttons send `callback_query` updates containing values such as `DISTRICT:<name>` or `NOTICE_DETAIL:<id>`.

Conversation state is stored in MySQL by Telegram `chatId`:

```text
IDLE → WAITING_FOR_DISTRICT → WAITING_FOR_TALUKA → WAITING_FOR_VILLAGE → IDLE
```

Sessions survive application restarts. District and taluka buttons use database values; the portal is not scraped on every button click.

| Command / button | Action |
|---|---|
| `/start` | Reset the session and show the main menu, unless currently awaiting village input |
| `/subscribe` / Subscribe | Start district → taluka → village selection |
| `/list` / My Subscriptions | List subscriptions |
| `/unsubscribe` / Unsubscribe | Choose a subscription to remove |
| `/notices` / My Notices | Browse stored notices |
| `/help` / Help | Show the implemented help message |
| `/cancel` / Cancel / 🔄 Refresh | Reset the session and show the main menu |

While awaiting a village, input other than cancel/refresh is passed to village matching, including `/start` and other menu commands. Use `/cancel` or **🔄 Refresh** to leave that state first.

## Notice collection and reminders

`NoticeOrchestrationService` processes subscribed villages sequentially. For each village:

1. Playwright opens Chromium, selects the location, and submits a captcha read by Gemini.
2. The scraper verifies the village dashboard and reads notice metadata from the table.
3. Mutation numbers already present in the database are skipped. New notice pages are opened to capture body text and full-page screenshots.
4. Gemini produces an English summary. Content classified as `DATA_NOT_FOUND` is skipped.
5. The notice is saved, then its screenshot and summary are sent to village subscribers. Eligible notices include reminder opt-in buttons.
6. Cleanup retains the latest **20 processed notices per village**.

Telegram browsing fetches the latest **10 processed notices** per village before filtering active/expired dates. Notices with missing or unparseable deadlines are excluded from those filtered lists.

| Scheduled job | Cron | Behavior |
|---|---|---|
| Notice check | `0 0 9 * * *` | Check all subscribed villages daily at 9 AM |
| Reminder check | `0 0 8 * * *` | Send opted-in reminders daily at 8 AM when 0–3 days remain |

No explicit scheduler timezone is configured; these times follow the runtime's scheduling timezone. The application must be running for scheduled jobs to execute.

Reminders repeat on each daily run during the final three days and on deadline day. The `reminded` flag is set only after the deadline has passed; it does not currently mean a single reminder was sent.

## Tech stack

Versions below reflect `pom.xml` and the current code, not minimum supported versions of external services.

| Component | Configuration |
|---|---|
| Java | Maven target 17 |
| Spring Boot | 4.0.6 |
| Persistence | Spring Data JPA / Hibernate, MySQL JDBC driver |
| Browser automation | Playwright 1.59.0 with Chromium |
| AI SDK | Google Gen AI Java SDK 1.56.0 |
| AI model | `gemini-2.5-flash` |
| Messaging | Telegram Bot API through Java `HttpClient` |
| Build | Maven; wrapper scripts included |

Gemini is used for captcha image reading, matching typed villages to the stored list, and notice summarization. Older district/taluka/village validation methods remain in `GeminiClient`, but the current bot flow uses `matchFromList()` for village matching.

## Local setup

### 1. Prerequisites and database

Install JDK 17 or newer, MySQL, and Maven (or use the included Maven wrapper). You also need a Telegram bot token, a Gemini API key, and Playwright's Chromium browser.

Create the database configured in `application.properties`:

```sql
CREATE DATABASE IF NOT EXISTS chawli CHARACTER SET utf8mb4;
```

Default configuration:

| Setting | Value |
|---|---|
| HTTP port | `9999` |
| Database URL | `jdbc:mysql://localhost:3306/chawli` |
| Database user | `root` |
| Schema management | `spring.jpa.hibernate.ddl-auto=update` |
| SQL logging | Enabled |
| Playwright headless | `false` — opens a visible browser |
| Playwright slow motion | `1000` milliseconds |

Use Spring configuration overrides such as `SPRING_DATASOURCE_URL` and `SPRING_DATASOURCE_USERNAME` if your database differs. Hibernate updates tables on startup; the database itself must already exist.

### 2. Set environment variables

Set these in the shell or IDE run configuration used to launch the application:

```bash
export DB_PASSWORD='your_mysql_password'
export TELEGRAM_BOT_TOKEN='your_telegram_bot_token'
export GOOGLE_API_KEY='your_gemini_api_key'
```

### 3. Install Chromium and start the application

Using Maven:

```bash
mvn compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
mvn spring-boot:run
```

You can substitute `./mvnw` for `mvn`. For an environment without a graphical display, override the browser settings before starting:

```bash
export PLAYWRIGHT_HEADLESS=true
export PLAYWRIGHT_SLOWMO=0
```

Confirm startup completes on port **9999**.

### 4. Expose and register the Telegram webhook

For local development, start ngrok in another terminal:

```bash
ngrok http 9999
```

Register the public HTTPS hostname ngrok provides, using a shell with `TELEGRAM_BOT_TOKEN` set:

```bash
curl -sS -X POST \
  "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  --data-urlencode "url=https://YOUR-NGROK-HOST/api/telegram/webhook" \
  --data-urlencode 'allowed_updates=["message","callback_query"]'
```

Keep both Spring Boot and ngrok running. Register the webhook again whenever the public hostname changes. For a deployed service, use its public HTTPS hostname instead and configure the proxy to reach the backend.

### 5. Populate location data

If the village directory has not been populated:

```bash
curl -X POST http://localhost:9999/villages/scrape
```

`VillageDataScraper` reads district, taluka, and village dropdowns from the portal and stores their labels. It skips talukas with existing village records and attempts up to 10 browser sessions, waiting 30 seconds after session failures. A partially populated taluka can be skipped because the check only tests whether any records exist.

This is a synchronous, potentially long-running request. Inspect logs for completion: the controller's success response does not distinguish exhaustion of the scraper's retry loop.

### 6. Subscribe and check notices

Send `/start` to the bot and choose **Subscribe**. After subscribing, wait for the scheduled check or trigger it manually:

```bash
curl -X POST http://localhost:9999/notices/trigger
```

This fetches notices and can send alerts to all affected subscribers.

## Swagger UI and OpenAPI

After starting the application, open http://localhost:9999/swagger-ui.html to browse
all seven API operations, request schemas, and examples. Use **Try it out** and
**Execute** to call an endpoint; subscription changes, Telegram replies, and scraping
run against the actual service.

On Northflank, redeploy this version and open
`https://<your-service-domain>/swagger-ui.html`. No additional port is needed.
The API server URL is relative (`/`), so requests use the same host and HTTPS scheme
as the documentation page.

- JSON specification: `/v3/api-docs`
- YAML specification: `/v3/api-docs.yaml`

To save a local copy of the generated specification:

```bash
curl -fsS http://localhost:9999/v3/api-docs.yaml -o openapi.yaml
```

Springdoc 3.0.3 generates documentation from the controllers for Spring Boot 4.0.x.
To disable documentation in a deployment, set both
`SPRINGDOC_API_DOCS_ENABLED=false` and `SPRINGDOC_SWAGGER_UI_ENABLED=false`.

## API endpoints

All paths below use `http://localhost:9999` during local development.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/telegram/webhook` | Receive Telegram messages and callbacks |
| POST | `/notices/trigger` | Check all subscribed villages |
| POST | `/notices/trigger/{villageId}` | Check one stored village; returns 404 if missing |
| POST | `/villages/scrape` | Populate the location directory |
| POST | `/api/subscriptions` | Create a subscription |
| GET | `/api/subscriptions/{chatId}` | List subscriptions for a chat |
| DELETE | `/api/subscriptions` | Remove a subscription |

Subscription POST and DELETE requests accept JSON:

```json
{
  "chatId": 123456789,
  "district": "<district label>",
  "taluka": "<taluka label>",
  "village": "<village label>"
}
```

Use the portal-exact location labels stored in the database. The subscription API does not perform the bot's Gemini matching step. Notice triggers and village scraping execute synchronously; they do not enqueue background jobs.

## Data and source layout

| Entity | Stored information |
|---|---|
| `Village` | District, taluka, village; unique location combination |
| `Subscription` | Telegram chat linked to a village |
| `UserSession` | Chat ID, conversation state, selected district and taluka |
| `Notice` | Mutation and registration details, dates, survey number, URL, summary, screenshot, processing time |
| `NoticeReminder` | Chat linked to a notice and the `reminded` flag |

Notice screenshots are stored as database blobs. Raw notice `bodyText` is transient and is not persisted. Captcha screenshots are written to the local `screenshots/` directory, so scraping still requires filesystem access.

```text
src/main/java/com/example/aapliChawdi/
├── AapliChawdiApplication.java          # startup and scheduling enablement
├── controller/                        # webhook, subscriptions, manual scrape APIs
├── service/
│   ├── TelegramBotService.java        # commands, callback actions, notice browsing
│   ├── TelegramMessageService.java    # small text-message wrapper
│   ├── UserSessionService.java        # persisted conversation state
│   ├── SubscriptionService.java       # subscription management
│   ├── NoticeOrchestrationService.java# scraper → processor → cleanup
│   ├── NoticeProcessingService.java   # summaries, persistence, subscriber alerts
│   ├── NoticeCleanupService.java      # retain 20 processed notices per village
│   ├── NoticeMonitorService.java      # empty placeholder
│   └── NotificationService.java       # empty placeholder
├── scraper/                           # notice scraper and location-directory loader
├── client/                            # Gemini and Telegram API calls
├── scheduler/                         # daily notice and reminder jobs
├── entity/                            # JPA entities
├── repository/                        # Spring Data JPA queries
├── dto/                               # subscription request body
└── enums/                             # conversation states
```

## Troubleshooting missing Telegram updates

The webhook controller logs `Received Telegram update` before handling a message or callback. If it is absent:

1. Confirm Spring Boot finished starting on port 9999 and inspect its application console.
2. Confirm ngrok forwards to **9999**, not 8080.
3. Inspect the registered webhook:

   ```bash
   curl -sS "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getWebhookInfo"
   ```

4. Check `url`, `pending_update_count`, `last_error_message`, and `allowed_updates`. Ensure both `message` and `callback_query` updates are enabled.
5. Re-register the webhook if its hostname or path is stale.

To test the local route without sending a Telegram message or changing a subscription:

```bash
curl -i -X POST http://localhost:9999/api/telegram/webhook \
  -H 'Content-Type: application/json' \
  -d '{}'
```

The current controller returns HTTP 200 and logs receipt for this empty update. This checks the local route only, not public webhook delivery.

## Current implementation limits

- Duplicate detection uses a globally unique mutation number, rather than a village-and-mutation combination.
- The in-memory running-village guard prevents overlapping work for the same village in one process only. The scraper has shared mutable browser fields, so concurrent requests for different villages are not safely isolated.
- Notice captcha solving is configured for one attempt per fetch.
- Notices are saved before subscriber delivery. There is no persistent delivery queue or per-subscriber retry tracking, and Telegram API error responses are logged without explicit success validation.
- Cleanup deletes older notices without explicitly removing linked reminders, which can cause foreign-key failures.
- The application currently has no endpoint authentication or Telegram webhook secret validation; these are not implemented deployment protections.

## Tests

```bash
mvn test
```

The existing test only checks whether the Spring application context loads. It needs suitable startup configuration and database access; there are no dedicated automated tests for bot conversations, scraper behavior, or notification delivery.

## Author

**Akshay Reddy** — [GitHub](https://github.com/akreddy051) · [LinkedIn](https://www.linkedin.com/in/akshay-reddy-singadiwar-4b78201aa)

This project is for educational and personal use. It is not affiliated with the Government of Maharashtra.

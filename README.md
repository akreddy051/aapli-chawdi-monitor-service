# 🌾 Aapli Chawdi Digital Alert

> A real-time land mutation notice monitoring platform for Maharashtra, built on top of the [Aapli Chawdi](https://digitalsatbara.mahabhumi.gov.in/aaplichawdi) government portal.

---

## 📌 The Problem

Every day, land mutation notices are published on Maharashtra's Aapli Chawdi portal — recording sales, inheritances, partitions, government acquisitions, and legal transfers. Most people never see them because:

- They don't know the portal exists
- They don't check it regularly
- Families living in cities miss notices about ancestral land
- **Objection deadlines are missed permanently**

Bhumi Alert solves this by delivering instant, AI-summarized alerts directly to users on Telegram — no app download, no registration.

---

## 🏗️ Architecture

```
Telegram Users
      │
      ▼
Telegram Bot (Webhook)
      │
      ▼
Spring Boot Backend
      │
      ├── Subscription Service     → manages user-village subscriptions
      ├── Notice Orchestration     → coordinates scraping + processing
      ├── Gemini AI Client         → captcha solving, summarization, validation
      ├── Playwright Scraper       → automates Aapli Chawdi portal
      ├── Notice Processing        → AI summary + subscriber notifications
      ├── Reminder Scheduler       → objection deadline reminders
      └── Cleanup Service          → keeps last 20 notices per village
            │
            ▼
        MySQL Database
```

---

## ✨ Features

### For Users (via Telegram Bot)
- **Subscribe** to any village in Maharashtra using natural language — type in English, Marathi, or even with spelling mistakes
- **Instant notifications** when a new mutation notice appears for a subscribed village
- **AI-generated summaries** of notices — mutation type, parties involved, land details, financial details, key dates
- **Objection deadline reminders** — opt-in per notice, reminded 3 days before deadline
- **View notice history** — browse last 20 processed notices per village with screenshots
- **Manage subscriptions** — subscribe, view, and unsubscribe via interactive Telegram buttons
- **No typing required** — district and taluka selected via inline keyboard buttons

### Platform Capabilities
- Monitors **any village** across all 36 districts of Maharashtra
- **Concurrent-safe** — prevents duplicate processing when manual and scheduled triggers overlap
- **Captcha solving** using Gemini Vision AI
- **Fuzzy village matching** — Gemini matches user input against portal-exact names from the database
- **Auto-resume** village data scraping after rate-limit failures
- **Screenshot storage** in database (no filesystem dependency)
- **Manual trigger API** to check notices for any village on demand
- **Village data population** via dedicated API — pulls all districts, talukas, villages from portal

---

## 🤖 How the AI is Used

| Task | Model | Purpose |
|------|-------|---------|
| Captcha solving | Gemini Vision | Extracts digits from captcha image |
| Notice summarization | Gemini Flash | Summarizes Marathi land notices into structured English |
| Village validation | Gemini Flash | Matches user input (any language/spelling) to exact portal names |

The summarization prompt identifies: mutation type, parties involved, survey numbers, transaction amounts, important dates, and required actions — returning `DATA_NOT_FOUND` for error pages.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 4 |
| Database | MySQL 8 |
| Browser Automation | Microsoft Playwright |
| AI | Google Gemini 2.5 Flash |
| Messaging | Telegram Bot API |
| Scheduling | Spring `@Scheduled` (cron) |
| Build | Maven |

---

## 📁 Project Structure

```
src/main/java/com/example/aapliChawdi/
├── controller/
│   ├── TelegramWebhookController.java   # receives Telegram updates
│   ├── NoticeController.java            # manual trigger endpoints
│   └── VillageDataController.java       # village data population
├── service/
│   ├── TelegramBotService.java          # bot brain — commands + callbacks
│   ├── SubscriptionService.java         # subscribe/unsubscribe logic
│   ├── NoticeOrchestrationService.java  # coordinates scraper + processor
│   ├── NoticeProcessingService.java     # AI summary + notifications
│   ├── NoticeCleanupService.java        # keeps last 20 notices per village
│   └── UserSessionService.java          # conversational state management
├── scraper/
│   ├── AapliChawdiScraper.java          # Playwright portal automation
│   └── VillageDataScraper.java          # one-time village data loader
├── client/
│   ├── GeminiClient.java                # all Gemini AI interactions
│   └── TelegramClient.java              # Telegram API interactions
├── entity/
│   ├── Notice.java                      # land mutation notice
│   ├── Village.java                     # district/taluka/village
│   ├── Subscription.java                # user-village subscription
│   ├── UserSession.java                 # conversational session state
│   └── NoticeReminder.java              # objection deadline reminders
├── repository/                          # Spring Data JPA repositories
├── enums/
│   └── SessionState.java                # IDLE, WAITING_FOR_DISTRICT, etc.
└── scheduler/
    ├── NoticeScheduler.java             # daily 9 AM scrape trigger
    └── ReminderScheduler.java           # daily 8 AM deadline check
```

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- MySQL 8+
- Maven
- Google Gemini API key
- Telegram Bot token (from [@BotFather](https://t.me/botfather))
- Playwright browsers installed

### Environment Variables

```bash
DB_PASSWORD=your_mysql_password
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
GOOGLE_API_KEY=your_gemini_api_key
```

### Register Telegram Webhook

```bash
curl "https://api.telegram.org/bot{TOKEN}/setWebhook?url=https://yourdomain.com/api/telegram/webhook"
```

### Populate Village Data (run once)

```bash
curl -X POST http://localhost:8080/villages/scrape
```

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/telegram/webhook` | Telegram webhook receiver |
| `POST` | `/notices/trigger` | Trigger scrape for all subscribed villages |
| `POST` | `/notices/trigger/{villageId}` | Trigger scrape for a specific village |
| `POST` | `/villages/scrape` | Populate village data from portal |

---

## 💬 Bot Commands

| Command / Button | Action |
|-----------------|--------|
| `/start` | Show main menu with keyboard buttons |
| `Subscribe` | Start subscription flow (district → taluka → village) |
| `My Subscriptions` | List active subscriptions |
| `My Notices` | Browse recent notices for subscribed villages |
| `Unsubscribe` | Remove a subscription |
| `Help` | Show available commands |
| `/cancel` | Cancel current operation |

---

## 🔑 Key Design Decisions

**Why Telegram?**
No app development, no Play Store, no user registration. Telegram becomes the frontend — users already know how to use it.

**Why Gemini for village validation?**
Maharashtra has 44,000+ villages. Instead of strict matching, Gemini matches user input (any language, any spelling) against portal-exact names from the database. Fuzzy, forgiving, and accurate.

**Why store screenshots in the database?**
Filesystem storage doesn't survive redeployment. Database storage is self-contained and portable — one less external dependency.

**Why session state in the database?**
Telegram messages arrive independently with no connection between them. Database-backed sessions allow multi-turn conversations across any number of messages and server restarts.

**Concurrent run protection**
A `ConcurrentHashMap` prevents the daily scheduler and manual API trigger from processing the same village simultaneously — avoiding duplicate notices and Telegram messages.

---

## 📸 Screenshots

> *[img.png](img.png)*

---

## 👤 Author

**Akshay Reddy**  
[GitHub](https://github.com/akreddy051) · [LinkedIn](https://www.linkedin.com/in/akshay-reddy-singadiwar-4b78201aa)

---

## 📄 License

This project is for educational and personal use. Not affiliated with the Government of Maharashtra.

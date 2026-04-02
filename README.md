# Personal SMS App

A minimal, battery-efficient Android SMS replacement app with API message forwarding, built with MVVM + Repository pattern.

---

## Features

### Core SMS

- Full default SMS app — send, receive, delete messages
- Thread-based conversation view with Material 3 chat bubbles
- Contact name resolution from the Android contacts database
- Search conversations by contact name or phone number
- Mark read/unread, delete individual messages or entire threads
- Compose new messages with contact autocomplete
- Quick-reply via system call screen (`RESPOND_VIA_MESSAGE`)

### API Integration

- Every incoming SMS is POSTed to a configurable endpoint
- Optional `X-API-Key` header for authentication
- API can tag messages (shown as chips), flag as important (red dot), or delete
- WorkManager-powered background sync — battery safe, waits for network, deduplicates rapid messages

### UI / UX

- Material Design 3 with day/night themes (Indigo brand)
- Chat bubbles: light purple incoming (left), indigo outgoing (right)
- Unread count badges, per-contact colored avatars (hashed from name)
- Timestamp formatting: today → HH:mm, this week → day name, older → MM/DD/YY
- Edge-to-edge display with system insets handling

### Other

- GitHub Releases auto-update checker (prompts to download + install new APK)
- Encrypted SharedPreferences (AES-256-GCM) for API key and settings
- Network security config allows cleartext for localhost/LAN (dev only)
- First-launch import of all existing SMS from the system content provider

---

## Architecture

**MVVM + Repository** — no DI framework, manual singletons.

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Activities + ViewModels + Adapters)          │
│  LiveData observation, ViewBinding, DiffUtil lists      │
├─────────────────────────────────────────────────────────┤
│  SmsRepository (single source of truth)                 │
│  Coordinates Room DB ↔ System ContentProvider           │
├─────────────────┬───────────────────────────────────────┤
│  Room Database   │  ApiService (OkHttp)                 │
│  sms_app.db      │  JSON POST → parse response          │
├─────────────────┴───────────────────────────────────────┤
│  Background: SmsReceiver → ApiSyncWorker (WorkManager)  │
└─────────────────────────────────────────────────────────┘
```

**Data flow — incoming SMS:**

1. System broadcasts `SMS_DELIVER` → `SmsReceiver`
2. Receiver writes to system SMS store + Room DB via `SmsRepository.insertIncoming()`
3. Enqueues one-time `ApiSyncWorker` (constraint: `CONNECTED`)
4. Worker batch-processes all unprocessed messages → `ApiService.forwardMessage()`
5. API response applied: tag / important / delete via `SmsRepository.applyApiResult()`

**Key decisions:**

- No Retrofit — direct OkHttp for maximum control, minimal dependencies
- No Jetpack Compose — traditional XML layouts + ViewBinding
- No Hilt/Dagger — manual singleton pattern (thread-safe double-checked locking)
- WorkManager `KEEP` policy deduplicates rapid incoming messages into one batch job
- Room queries exposed as `LiveData` for reactive UI updates

---

## Project Structure

```
app/src/main/java/com/personal/smsapp/
├── SMSApplication.java               — App init, notification channels, first-launch sync
├── data/
│   ├── local/
│   │   ├── Message.java              — Room entity (apiTag, isImportant, apiProcessed)
│   │   ├── Conversation.java         — Room entity for thread summaries
│   │   ├── MessageDao.java           — Room DAO (LiveData queries, batch ops)
│   │   ├── ConversationDao.java      — Room DAO (search, tag updates, archive)
│   │   ├── AppDatabase.java          — Room DB singleton (sms_app.db)
│   │   └── SmsRepository.java        — Single source of truth (DB + ContentProvider)
│   └── remote/
│       └── ApiService.java           — OkHttp POST + JSON response parsing
├── receiver/
│   ├── SmsReceiver.java              — SMS_DELIVER broadcast → insert + enqueue worker
│   └── MmsReceiver.java              — WAP_PUSH_DELIVER (required for default app role)
├── worker/
│   └── ApiSyncWorker.java            — WorkManager: batch-process pending messages
├── service/
│   ├── RespondViaMessageService.java — System quick-reply from call screen
│   └── HeadsUpService.java           — Stub service required for default SMS role
├── util/
│   ├── Prefs.java                    — EncryptedSharedPreferences wrapper (AES-256)
│   ├── NotificationHelper.java       — Channel management + incoming SMS alerts
│   ├── PhoneUtils.java               — Timestamps, default-app helpers
│   └── UpdateChecker.java            — GitHub Releases auto-update checker
└── ui/
    ├── ConversationListActivity.java  — Main screen: list, search, permissions
    ├── ConversationListViewModel.java — LiveData + switchMap search
    ├── ConversationAdapter.java       — DiffUtil, hashed avatar colors
    ├── MessageThreadActivity.java     — Chat thread: messages, send, delete
    ├── MessageThreadViewModel.java    — LiveData messages, mark-read
    ├── MessageAdapter.java            — Dual viewType (incoming/outgoing bubbles)
    ├── ComposeActivity.java           — New message with contact autocomplete
    └── SettingsActivity.java          — API URL, key, toggle, test sync
```

---

## Tech Stack

| Category         | Library / Version                                            |
| ---------------- | ------------------------------------------------------------ |
| Language         | Java 17                                                      |
| Min SDK          | 26 (Android 8.0)                                             |
| Target / Compile | 36                                                           |
| UI               | Material 3 (`1.12.0`), ConstraintLayout `2.2.1`              |
| Database         | Room `2.7.0`                                                 |
| Background       | WorkManager `2.10.0`                                         |
| Lifecycle        | ViewModel + LiveData `2.9.0`                                 |
| HTTP             | OkHttp `4.12.0`                                              |
| JSON             | org.json `20231013`                                          |
| Security         | security-crypto `1.1.0-alpha06` (EncryptedSharedPreferences) |
| Views            | RecyclerView `1.4.0`, SwipeRefreshLayout `1.1.0`             |
| Build plugin     | AGP `8.9.1`                                                  |

---

## Database Schema

### `messages`

| Column        | Type    | Notes                             |
| ------------- | ------- | --------------------------------- |
| id            | INTEGER | PK, auto-generated                |
| thread_id     | LONG    | FK to Telephony threads (indexed) |
| address       | TEXT    | Phone number (indexed)            |
| body          | TEXT    | Message content                   |
| date          | LONG    | Epoch millis                      |
| type          | INTEGER | 1 = inbox, 2 = sent, 3 = draft    |
| read          | INTEGER | Boolean                           |
| status        | INTEGER | 0 = pending, 1 = sent, 2 = failed |
| api_tag       | TEXT    | API-assigned label                |
| is_important  | INTEGER | API-assigned importance flag      |
| api_processed | INTEGER | Sync marker (indexed)             |
| system_sms_id | LONG    | Reference to system SMS store     |

### `conversations`

| Column        | Type    | Notes                              |
| ------------- | ------- | ---------------------------------- |
| thread_id     | LONG    | PK (from Telephony.Threads)        |
| display_name  | TEXT    | Resolved from Contacts             |
| address       | TEXT    | Phone number                       |
| snippet       | TEXT    | Last message preview (≤ 80 chars)  |
| date          | LONG    | Last message timestamp             |
| unread_count  | INTEGER |                                    |
| message_count | INTEGER |                                    |
| last_tag      | TEXT    | API tag from most recent message   |
| has_important | INTEGER | Thread contains important messages |
| is_archived   | INTEGER |                                    |

---

## Setup

1. Open in Android Studio (Hedgehog or newer)
2. Build → Run on a physical device (emulators can't receive real SMS)
3. Grant SMS, contacts, and notification permissions when prompted
4. Accept the default SMS app prompt
5. Go to **Settings** → enter your API URL (and optional key) → Save

`minSdk 26` · `targetSdk 36` · Java 17

---

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (minified with ProGuard)
./gradlew assembleRelease

# Lint report
./gradlew lintDebug
# → app/build/reports/lint/lint-results.html
```

Debug builds use application ID suffix `.debug` so they can be installed alongside release.

Version code and name are injected via Gradle properties:

```bash
./gradlew assembleRelease -PversionCode=5 -PversionName=1.2.0
```

---

## API Contract

### Request (POST to your endpoint)

```json
{
    "message_id": 42,
    "sender": "+1234567890",
    "body": "Your OTP is 9876",
    "timestamp": 1710000000000
}
```

Header: `X-API-Key: <your_secret>` (if configured in Settings)

### Response

```json
{
    "action": "keep",
    "tag": "otp",
    "important": false
}
```

| Field       | Values                | Meaning                                        |
| ----------- | --------------------- | ---------------------------------------------- |
| `action`    | `"keep"` / `"delete"` | Keep the message or delete it from DB + system |
| `tag`       | any string            | Label shown as a chip on the message/thread    |
| `important` | `true` / `false`      | Red dot indicator on conversation row          |

**Error handling:** if your API is unreachable or returns non-2xx, the message is kept as-is and marked processed. No infinite retries.

**OkHttp timeouts:** connect 10 s, read 15 s, write 10 s; retryOnConnectionFailure enabled.

---

## Permissions

| Permission                 | Purpose                                  |
| -------------------------- | ---------------------------------------- |
| `RECEIVE_SMS`              | Receive incoming SMS broadcasts          |
| `READ_SMS`                 | Read system SMS store for initial sync   |
| `SEND_SMS`                 | Send messages and quick-replies          |
| `RECEIVE_MMS`              | Required for default SMS app role        |
| `READ_PHONE_STATE`         | Required for default SMS app role        |
| `READ_CONTACTS`            | Contact name resolution + autocomplete   |
| `INTERNET`                 | API forwarding + update checks           |
| `ACCESS_NETWORK_STATE`     | WorkManager network constraint           |
| `POST_NOTIFICATIONS`       | Incoming SMS notifications (Android 13+) |
| `VIBRATE`                  | Notification vibration                   |
| `REQUEST_INSTALL_PACKAGES` | Auto-update APK installation             |

---

## Power Efficiency

| Concern                | Solution                                                                           |
| ---------------------- | ---------------------------------------------------------------------------------- |
| API calls on every SMS | WorkManager with `NETWORK_CONNECTED` constraint — deferred until network available |
| Burst messages         | `ExistingWorkPolicy.KEEP` — multiple arrivals = one job execution                  |
| No polling             | Event-driven only (receiver → worker → done)                                       |
| Background limits      | WorkManager handles Android Doze / battery saver transparently                     |
| DB queries             | Room LiveData; no manual polling loops                                             |

---

## API Server Example (Python / Flask)

```python
from flask import Flask, request, jsonify
import re

app = Flask(__name__)

@app.route('/sms-webhook', methods=['POST'])
def sms_webhook():
    data = request.json
    body = data.get('body', '')

    # Auto-tag OTPs
    if re.search(r'\b\d{4,8}\b', body) and any(w in body.lower() for w in ['otp','code','verify']):
        return jsonify(action='keep', tag='otp', important=True)

    # Delete obvious spam
    if any(w in body.lower() for w in ['click here', 'free prize', 'won a']):
        return jsonify(action='delete', tag='spam', important=False)

    return jsonify(action='keep', tag='', important=False)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

Point the app's API URL to `http://YOUR_SERVER_IP:5000/sms-webhook`.

---

## n8n / Home Assistant Webhook

Since you run n8n, you can use an n8n webhook node as the API endpoint:

- Webhook URL → set as API URL in app
- Process body in n8n function node
- Return JSON: `{ "action": "keep", "tag": "...", "important": false }`

---

## Known Limitations

- MMS is received via system stack (displayed by system apps); full MMS compose not implemented
- No group SMS UI (messages still receive/send correctly as individual threads)
- Contact photo not shown (letter avatar only)
- No message search (thread search only)

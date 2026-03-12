# Personal SMS App

A minimal, battery-efficient Android SMS replacement app with API message forwarding.

---

## Features

- Full default SMS app (send, receive, delete, thread view)
- Contact name resolution
- Search conversations
- **API forwarding**: every incoming message is POSTed to your endpoint
- **API actions**: your API can tag, mark important, or delete messages
- WorkManager-powered background sync (battery safe, runs on network reconnect)
- Material 3 UI with chat bubbles, unread badges, tag chips

---

## Project Structure

```
app/src/main/java/com/personal/smsapp/
├── SMSApplication.java          — App init, notification channels, first-launch sync
├── data/
│   ├── local/
│   │   ├── Message.java         — Room entity (+ apiTag, isImportant, apiProcessed)
│   │   ├── Conversation.java    — Room entity for thread summaries
│   │   ├── MessageDao.java      — Room DAO
│   │   ├── ConversationDao.java — Room DAO
│   │   ├── AppDatabase.java     — Room database singleton
│   │   └── SmsRepository.java  — Single source of truth (DB + ContentProvider)
│   └── remote/
│       └── ApiService.java      — OkHttp client, JSON POST + response parsing
├── receiver/
│   ├── SmsReceiver.java         — SMS_DELIVER broadcast → insert + enqueue worker
│   └── MmsReceiver.java         — WAP_PUSH_DELIVER (stub, required for default app)
├── worker/
│   └── ApiSyncWorker.java       — WorkManager worker: batch-processes pending msgs
├── util/
│   ├── Prefs.java               — SharedPreferences wrapper
│   ├── NotificationHelper.java  — Notification channel + incoming alerts
│   └── PhoneUtils.java          — Timestamp formatting, default app helpers
└── ui/
    ├── ConversationListActivity.java  — Main screen
    ├── ConversationListViewModel.java
    ├── ConversationAdapter.java
    ├── MessageThreadActivity.java     — Thread / chat view
    ├── MessageThreadViewModel.java
    ├── MessageAdapter.java            — Incoming/outgoing chat bubbles
    ├── ComposeActivity.java           — New message
    ├── SettingsActivity.java          — API config
    └── HeadsUpService.java            — Required stub for default SMS role
```

---

## Setup

1. Open in Android Studio (Hedgehog or newer)
2. `minSdk 26` (Android 8) — tested up to SDK 34
3. Build → Run on device (emulators can't receive real SMS)
4. Grant all SMS + contacts permissions when prompted
5. Set as default SMS app when prompted
6. Go to **Settings** → enter your API URL → Save

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

Header: `X-API-Key: your_secret` (if configured)

### Response

```json
{
  "action": "keep",
  "tag": "otp",
  "important": false
}
```

| Field       | Values                     | Meaning                                      |
|-------------|---------------------------|----------------------------------------------|
| `action`    | `"keep"` / `"delete"`    | Keep the message or delete it from DB + system |
| `tag`       | any string                | Label shown as a chip on the message/thread  |
| `important` | `true` / `false`          | Shows red dot indicator on conversation row  |

**Error handling**: if your API is unreachable or returns non-2xx, the message is kept as-is and marked processed to avoid infinite retries.

---

## Power Efficiency

| Concern | Solution |
|---------|----------|
| API calls on every SMS | WorkManager with `NETWORK_CONNECTED` constraint — deferred until network available |
| Burst messages | `ExistingWorkPolicy.KEEP` — multiple arrivals = one job execution |
| No polling | Event-driven only (receiver → worker → done) |
| Background limits | WorkManager handles Android Doze / battery saver transparently |
| DB queries | Room LiveData; no manual polling loops |

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

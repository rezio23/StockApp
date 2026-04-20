# Firebase Setup Instructions

## Step 1 — Create a Firebase Project
1. Go to https://console.firebase.google.com/
2. Click **Add project** → name it `ChatApp` → Continue
3. Disable Google Analytics (optional) → **Create project**

## Step 2 — Add Android App
1. In your Firebase project, click the **Android icon** (</>)
2. Package name: `com.example.chatapp`
3. App nickname: `ChatApp`
4. Click **Register app**
5. Download `google-services.json`
6. Place it in: `app/google-services.json` (replace the placeholder file)

## Step 3 — Enable Email/Password Authentication
1. In Firebase Console → **Authentication** → **Get started**
2. Click **Sign-in method** tab
3. Enable **Email/Password** → Save

## Step 4 — Enable Realtime Database
1. In Firebase Console → **Realtime Database** → **Create database**
2. Choose your region (e.g. us-central1)
3. Start in **Test mode** (allows read/write for 30 days)
4. Click **Enable**

## Step 5 — Set Database Rules (for production use later)
Paste these rules in **Realtime Database → Rules**:
```json
{
  "rules": {
    "users": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "messages": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

## Step 6 — Open in Android Studio
1. Open Android Studio → **Open** → select the `ChatApp` folder
2. Wait for Gradle sync to complete
3. Replace `app/google-services.json` with the one you downloaded
4. Click **Run** (green play button)

## How It Works
- Register **User 1** on Device/Emulator 1
- Register **User 2** on Device/Emulator 2 (or another emulator)
- Each user sees the other in the user list
- Tap a user to open the chat — messages sync in real-time via Firebase

## Database Structure
```
Firebase Realtime Database
├── users/
│   ├── {uid1}/  → { uid, name, email }
│   └── {uid2}/  → { uid, name, email }
└── messages/
    └── {uid1_uid2}/
        ├── {msgId1}/ → { messageId, senderId, text, timestamp }
        └── {msgId2}/ → { messageId, senderId, text, timestamp }
```

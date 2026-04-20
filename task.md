# ChatApp - Android Kotlin + Firebase

## Requirements
- Android app with XML Views
- Firebase Auth (Email & Password - register/login)
- Firebase Realtime Database for chat messages
- 2 users can chat with each other in real-time
- App name: ChatApp

## Pages
1. LoginActivity (XML + Kotlin)
2. RegisterActivity (XML + Kotlin)
3. MainActivity / UserListActivity - show the other user to chat with
4. ChatActivity - real-time chat UI (RecyclerView + send message)

## Firebase
- Auth: Email/Password
- Realtime DB: /messages/{chatId}/{messageId} -> {senderId, text, timestamp}
- Users: /users/{uid} -> {name, email}

## Structure
- app/src/main/java/com/example/chatapp/
  - LoginActivity.kt
  - RegisterActivity.kt
  - UserListActivity.kt
  - ChatActivity.kt
  - adapters/MessageAdapter.kt
  - adapters/UserAdapter.kt
  - models/Message.kt
  - models/User.kt
- app/src/main/res/layout/
  - activity_login.xml
  - activity_register.xml
  - activity_user_list.xml
  - activity_chat.xml
  - item_message_sent.xml
  - item_message_received.xml
  - item_user.xml

## Status
- [ ] Project structure
- [ ] Models
- [ ] Layouts
- [ ] Activities
- [ ] Adapters
- [ ] build.gradle
- [ ] Firebase setup instructions

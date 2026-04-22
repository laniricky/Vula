# Vula — Android App Implementation Plan (Phase 1 MVP)

Build a hybrid social + messaging Android app with three pillars: Global feed, Local Wi-Fi social layer, and Chat — all backed by Firebase on the Spark plan.

## User Review Required

> [!IMPORTANT]
> **Push Notification Limitation**: Firebase Spark plan does NOT support Cloud Functions. True background push notifications (when app is closed) require the Blaze plan. Phase 1 will implement **in-app real-time messaging via Firestore listeners** and **FCM topic-based notifications** for basic foreground alerts. Full background push requires a future Blaze upgrade.

> [!IMPORTANT]
> **Username Auth Workaround**: Firebase Auth doesn't natively support username+password login. We'll use `{username}@vula.local` as the internal email address for Firebase Auth, with username displayed in the UI. This is a common pattern but means password reset via email won't work until Phase 2 adds real email.

> [!WARNING]
> **Local Mode Requires Internet**: Option A (backend-tagged) means local mode posts go through Firebase. Users need internet access (they're on Wi-Fi, so this is expected). True offline/P2P local mode is deferred to Phase 2.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Navigation | Compose Navigation |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Notifications | Firebase Cloud Messaging (FCM) |
| Image Loading | Coil |
| Async | Kotlin Coroutines + Flow |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Hashing | `java.security.MessageDigest` (Kotlin-native SHA-256) |

---

## Project Structure

```
Vula/
├── app/
│   ├── src/main/java/com/vula/app/
│   │   ├── VulaApplication.kt
│   │   ├── MainActivity.kt
│   │   ├── navigation/
│   │   │   └── VulaNavGraph.kt
│   │   ├── core/
│   │   │   ├── model/        (User, Post, Message, etc.)
│   │   │   ├── di/           (Hilt AppModule)
│   │   │   ├── util/         (HashUtils, AliasGenerator, Constants)
│   │   │   └── ui/
│   │   │       ├── theme/    (Color, Theme, Type, Shape)
│   │   │       └── components/ (Shared composables)
│   │   ├── auth/
│   │   │   ├── data/         (AuthRepositoryImpl)
│   │   │   └── ui/           (Login/Register screens + ViewModel)
│   │   ├── global/
│   │   │   ├── data/         (Post/Comment/Follow repositories)
│   │   │   └── ui/           (Feed, Profile, CreatePost screens)
│   │   ├── local/
│   │   │   ├── data/         (LocalRepository, WifiDetector)
│   │   │   └── ui/           (LocalFeed, PeopleHere screens)
│   │   └── chat/
│   │       ├── data/         (ChatRepository)
│   │       └── ui/           (ChatList, Conversation screens)
│   └── src/main/res/
├── build.gradle.kts (project)
├── app/build.gradle.kts
└── google-services.json (from Firebase console)
```

---

## Proposed Changes

### Phase 1A — Project Setup & Core Module
- Project initialization
- Firebase setup
- Core theme
- Core models
- Core utilities
- Shared UI components
- DI module
- Navigation

### Phase 1B — Authentication Module
- AuthRepositoryImpl
- LoginScreen
- RegisterScreen
- AuthViewModel

### Phase 1C — Global Module (Instagram-like Feed)
- PostRepositoryImpl & FollowRepositoryImpl
- FeedScreen & FeedViewModel
- CreatePostScreen
- ProfileScreen

### Phase 1D — Local Mode Module (Wi-Fi Social Layer)
- WifiDetector
- LocalRepositoryImpl
- LocalModeScreen
- LocalFeedScreen
- PeopleHereList
- LocalViewModel

### Phase 1E — Chat Module (Telegram-like)
- ChatRepositoryImpl
- ChatListScreen
- ConversationScreen
- Chat UI components

### Phase 1F — Push Notifications & Polish
- FCM integration
- Safety & Privacy constraints
- Polish & error handling

---

## Implementation Order

| Step | Module | Est. Files | Description |
|------|--------|-----------|-------------|
| 1 | Setup | ~5 | Project creation, Gradle config, Firebase setup |
| 2 | Core | ~15 | Theme, models, utils, DI, shared components |
| 3 | Auth | ~5 | Login, register, auth repository |
| 4 | Navigation | ~2 | Nav graph, bottom bar, auth flow routing |
| 5 | Global | ~12 | Feed, posts, comments, likes, profile, follows |
| 6 | Local | ~8 | Wi-Fi detection, local feed, presence, aliases |
| 7 | Chat | ~10 | Chat list, conversations, message requests |
| 8 | Notifications | ~3 | FCM service, token management |
| 9 | Safety | ~3 | Rate limiting, block, report |
| 10 | Polish | ~5 | Splash, empty states, error handling |
| **Total** | | **~68 files** | |

---

## Open Questions Resolved
- **Profile Pictures in Phase 1**: Allowed during edit-only to simplify registration.
- **Image Compression**: Client-side compression (max 1080px, 80% JPEG) to conserve Spark plan storage.

# Vula — Project Decisions & Context

> This file tracks all architectural decisions, user preferences, and clarifications
> gathered during project planning and development.

---

## App Overview
- **Name**: Vula
- **Type**: Hybrid social + messaging Android app
- **Core Pillars**: Global feed (Instagram-like), Local Wi-Fi social layer, Chat (Telegram-like)
- **Backend**: Firebase (Spark / free plan)
- **Primary Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture

---

## Confirmed Decisions (2026-04-21)

### 1. Firebase Configuration
- **Firebase project**: Set up from scratch (no existing project)
- **Plan**: Spark (free tier)
- **Spark plan limits to be aware of**:
  - Firestore: 50K reads/day, 20K writes/day, 20K deletes/day
  - Storage: 5GB total, 1GB/day download
  - Auth: 10K verifications/month
  - NO Cloud Functions on Spark plan
- **Services to use**:
  - Firebase Authentication (username + password for Phase 1)
  - Cloud Firestore (all data: users, posts, follows, chat, local posts)
  - Firebase Storage (profile pics, post images)
  - Firebase Cloud Messaging (push notifications — limited without Cloud Functions on Spark)

### 2. UI Framework
- **Jetpack Compose** — modern declarative UI

### 3. Color Scheme / Branding
- **Brown theme** — warm earthy palette
- Planned palette:
  - Primary: Rich Brown `#6D4C41`
  - Primary Dark: `#3E2723`
  - Primary Light: `#D7CCC8`
  - Accent/Secondary: Warm Amber `#FFB300`
  - Background (dark mode): `#1A1210`
  - Surface: `#2C2018`
  - On-Primary: `#FFFFFF`
  - On-Surface: `#F5E6D3` (warm cream)
  - Error: `#CF6679`

### 4. Local Mode Architecture
- **Option A (MVP)**: Local posts stored in Firestore, tagged with `network_id`
- Rationale: Simpler, works immediately, no P2P complexity, suitable for Spark plan
- NOTE: Requires internet connection for local mode (acceptable for MVP since users are on Wi-Fi)

### 5. NDK / C++ Module
- **Deferred to Phase 2**
- Phase 1 will use Kotlin-native `java.security.MessageDigest` for SHA-256 hashing
- NDK module planned for Phase 2: optimized hashing, encryption, optional UDP multicast discovery

### 6. Authentication
- **Phase 1**: Username + Password
  - Implementation: Firebase Auth uses email under the hood
  - Convention: `{username}@vula.local` as the internal Firebase auth email
  - User-facing login: username + password only
  - Username uniqueness enforced via Firestore
- **Phase 2**: Add proper email, email verification, password reset

### 7. Chat Backend
- **Firestore with snapshot listeners** (single database for all data)
- Rationale: Simpler architecture, one set of security rules, consistent querying
- Can migrate to Realtime Database for chat in Phase 2 if latency is a concern

### 8. Push Notifications
- **Yes, include in Phase 1**
- ⚠️ LIMITATION: Spark plan does NOT support Cloud Functions
  - Cannot trigger server-side push notifications on message receive
  - Options for Phase 1:
    1. In-app real-time via Firestore listeners (works when app is open)
    2. FCM topic subscriptions for basic notifications
    3. Full push notifications require Blaze plan (future upgrade)

### 9. Target API Levels
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34

### 10. Deployment
- **Development and testing only** for now
- No Google Play Store release planned yet
- Debug APK builds for testing

### 11. Monetization
- **None planned** for now
- Architecture will be kept flexible but no monetization modules included

---

## Decisions Log

| Date       | Decision                              | Rationale                                    |
|------------|---------------------------------------|----------------------------------------------|
| 2026-04-21 | Firebase as backend                   | User requirement                             |
| 2026-04-21 | Spark (free) plan                     | User preference                              |
| 2026-04-21 | Jetpack Compose for UI                | Modern, recommended                          |
| 2026-04-21 | Brown earthy theme                    | User preference                              |
| 2026-04-21 | Local Mode Option A (backend-tagged)  | MVP simplicity, no P2P complexity            |
| 2026-04-21 | Defer NDK to Phase 2                  | Kotlin crypto sufficient for MVP             |
| 2026-04-21 | Username + Password auth              | User preference, email in Phase 2            |
| 2026-04-21 | Firestore for chat                    | Single database, simpler architecture        |
| 2026-04-21 | Push notifications in Phase 1         | User requirement (limited by Spark plan)     |
| 2026-04-21 | Min SDK API 26 (Android 8.0)          | ~95% device coverage                         |
| 2026-04-21 | Dev/testing only deployment           | No Play Store release yet                    |
| 2026-04-21 | No monetization                       | Not planned yet                              |

---

## Notes
- Phase 1 MVP scope per prompt: Auth, Global posts (text + optional image), Basic chat, Local mode (Wi-Fi detect, join network, post text, view local feed)
- NO reels, stories, video streaming, or complex ranking algorithms in Phase 1
- Firebase Auth doesn't natively support username-only login — we use `{username}@vula.local` convention internally
- True background push notifications require Blaze plan (Cloud Functions needed for server-side triggers)

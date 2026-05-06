# Vula — Session Context & Handoff File
> Generated: 2026-05-06 | Conversation ID: ac24c086-814f-4b3f-b030-2686d232d39c
> GitHub: https://github.com/laniricky/Vula  |  Branch: `main`  |  Last commit: `f34a778`

---

## 📱 Project Overview

**Vula** is a contacts-based Android social messenger (Kotlin + Jetpack Compose) backed by a **self-hosted Dockerized Go backend** with MinIO object storage and PostgreSQL. It is transitioning away from Firebase toward a fully self-hosted architecture.

- **Android**: Kotlin, Jetpack Compose, Hilt DI, Retrofit, CameraX, ExoPlayer, Material3
- **Backend**: Go (Gorilla Mux/WebSocket), PostgreSQL, MinIO, Docker Compose
- **Auth**: JWT (replacing Firebase Auth)
- **Media storage**: MinIO (replacing Firebase Storage)

---

## 🖥️ Local Development Environment (Source PC)

| Setting | Value |
|---|---|
| OS | Windows 11 |
| Project path | `C:\DEV\Vula` |
| Backend | Docker Compose (`docker-compose.yml` in project root) |
| Go backend port | `8080` |
| MinIO port | `9000` |
| Host LAN IP | `192.168.137.1` (WSA gateway / hot-spot IP) |
| Android emulator | WSA (Windows Subsystem for Android) |
| Physical test device | Samsung Galaxy A03s (SM-A037U, Android 13) |
| ADB connection | WiFi — `adb connect <phone-ip>:5555` or mDNS |

---

## 🔌 Network / Backend Connectivity

### `NetworkModule.kt` — Dynamic BASE_URL resolution
```
Emulator (10.0.2.2)  → http://10.0.2.2:8080/
WSA detected         → http://192.168.137.1:8080/
Physical device      → http://192.168.137.1:8080/   (same LAN via hotspot)
```
WSA is detected by checking `Build.MODEL.contains("Subsystem for Android", ignoreCase = true)`.

### Backend start command
```bash
cd C:\DEV\Vula
docker-compose up -d
```

### Push to physical device (WiFi ADB)
```powershell
# Connect (find phone IP in Settings > About > Status)
adb connect <phone-ip>:5555
# Or if already paired via mDNS:
adb devices   # find the mDNS transport ID
adb -s <transport-id> install -r app\build\outputs\apk\debug\app-debug.apk
# Or just:
.\gradlew.bat installDebug
```

---

## ✅ What Was Built This Session (UI Modernization Sprint)

### 1. ProfileScreen (`global/ui/profile/ProfileScreen.kt`)
- Parallax cover photo header
- Glassmorphism collapsing top bar
- Glowing avatar with gradient ring
- Story highlights row
- Content tabs: Posts / Clips / Saved
- Haptic feedback on interactions
- **Kebab (⋮) menu** opens a `ModalBottomSheet`:
  - **Own profile**: Settings & Privacy, Share/QR, Archive, Analytics, Log Out
  - **Other profile**: Share, Mute, Block, Report, About this Account

### 2. Navigation (`navigation/VulaNavGraph.kt`)
- Removed global `ModalNavigationDrawer` (hamburger menu)
- Logout wired through Profile bottom sheet → `AuthViewModel.logout()`
- `hiltViewModel()` call correctly scoped inside composable

### 3. VulaTopBar (`core/ui/components/VulaTopBar.kt`)
- Added `showStats: Boolean = true` parameter
- Title bumped to ExtraBold 22sp
- Stats (followers/views) now optional per-screen

### 4. ChatListScreen / Activity Feed (`chat/ui/ChatListScreen.kt`)
- Renamed to **"Recent Activity"** top bar
- Top bar actions: ✔✔ Mark All Read + ⚙️ Settings (replacing follower/view stats)
- **Grouped avatar clusters** with overlapping circles (e.g. "alex.j, maria.ss and 14 others")
- **Unread states**: translucent primary background + blue dot indicator
- **Rich comment previews**: vertical indent bar + actual comment text
- **Quick Reply** button inline on comment notifications
- **Animated Follow-Back button**: morphs to ✓ checkmark with haptic feedback
- **Creator Milestone Widget**: circular progress ring + goal text
- **Filter tabs**: All / Mentions / Likes / Comments / Follows

### 5. CreatePostScreen (`global/ui/post/CreatePostScreen.kt`) — Camera-First Redesign
- **Live CameraX viewfinder** fills 65% of screen on open
- Content type switcher: POST / STORY / CLIP (overlay, no extra screen)
- Controls: 🔄 Flip · ⚪ Shutter (tap=photo, hold=video) · 🖼 Gallery
- Flash toggle in top bar
- **Real `MediaStore` gallery row** — actual device photos, not placeholders
- After capture/select → **`ModalBottomSheet`** slides up:
  - Thumbnail preview + caption field side-by-side
  - `@mention` / `#hashtag` styled placeholder
  - Character count ring (turns orange → red near limit)
  - 🌍 Everyone / 👥 Contacts / 🔒 Private audience picker
  - Share button (morphs to spinner during upload)

### 6. AndroidManifest.xml
Added permissions:
```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

---

## 📦 Key Dependencies (app/build.gradle.kts)

```kotlin
// CameraX 1.3.2
implementation("androidx.camera:camera-core:1.3.2")
implementation("androidx.camera:camera-camera2:1.3.2")
implementation("androidx.camera:camera-lifecycle:1.3.2")
implementation("androidx.camera:camera-video:1.3.2")
implementation("androidx.camera:camera-view:1.3.2")
implementation("androidx.camera:camera-extensions:1.3.2")

// Compose Material3, Hilt, Retrofit, Coil, ExoPlayer — all present
```

---

## 🗂️ Key File Locations

```
app/src/main/java/com/vula/app/
├── auth/
│   ├── ui/          AuthViewModel, LoginScreen, RegisterScreen
│   └── data/        AuthRepositoryImpl (JWT-based)
├── chat/
│   └── ui/          ChatListScreen (Activity), ConversationScreen, ChatViewModel
├── contacts/
│   └── ui/          RadarScreen (local Wi-Fi social)
├── core/
│   ├── di/          NetworkModule (BASE_URL logic), AppModule
│   ├── model/       User, Post, ChatRoom
│   └── ui/
│       ├── camera/  CameraCaptureScreen (standalone, reusable)
│       ├── components/ VulaTopBar, UserAvatar
│       └── theme/   Color, Type
├── global/
│   ├── ui/
│   │   ├── feed/    FeedScreen, FeedViewModel
│   │   ├── post/    CreatePostScreen, CreatePostViewModel
│   │   └── profile/ ProfileScreen, ProfileViewModel
│   └── data/        PostRepository, FollowRepository
├── local/           LocalMode (Wi-Fi P2P feed)
└── navigation/      VulaNavGraph, Screen (sealed class with routes)

backend/             Go backend (main.go, hub.go, models.go, handlers)
docker-compose.yml   PostgreSQL + MinIO + Go app
.env                 DB credentials, JWT secret, MinIO keys
```

---

## 🔜 Next Steps / Pending Work

### High Priority
1. **Wire Reply button** on Activity feed → navigate to `ConversationScreen` or open inline input
2. **QR Code screen** (Profile bottom sheet "Share/QR" action) → generate QR from `@username`
3. **MinIO upload** in `CreatePostViewModel.createPost()` → multipart upload to Go backend `/api/posts`
4. **WebSocket real-time chat** → replace REST polling with `OkHttp WebSocket` + JWT auth header

### Medium Priority
5. **Mark all as read** action on Activity top bar → API call + local state reset
6. **Notification settings** screen (Activity top bar ⚙ button)
7. **Analytics screen** (Profile bottom sheet) → charts for post reach/engagement
8. **Story viewer** → full-screen 9:16 ephemeral content with progress bar

### Known Issues
- `INSTALL_FAILED_INSUFFICIENT_STORAGE` may appear on low-storage test devices — clear cache first
- WSA requires the PC hotspot / bridged network to be active for backend connectivity
- `LocalLifecycleOwner` deprecation warning in `CreatePostScreen` — non-breaking, can migrate to `androidx.lifecycle.compose` later

---

## 🚀 Quick Start on a New PC

```bash
# 1. Clone
git clone https://github.com/laniricky/Vula.git
cd Vula

# 2. Start backend
docker-compose up -d

# 3. Update host IP if different from 192.168.137.1
# Edit: app/src/main/java/com/vula/app/core/di/NetworkModule.kt
# Change the WSA/physical device URL to your LAN IP

# 4. Build & install
./gradlew installDebug

# 5. Connect device
adb connect <device-ip>:5555
```

---

## 🔑 Environment Variables (.env — DO NOT COMMIT)

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=vula
DB_USER=<user>
DB_PASSWORD=<password>
JWT_SECRET=<secret>
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=<key>
MINIO_SECRET_KEY=<secret>
MINIO_BUCKET=vula-media
```

---

*This file was auto-generated. Commit it to the repo so it travels with the code.*

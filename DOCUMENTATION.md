# Vula Technical Documentation

## 1. Architecture Overview

Vula follows a clean MVVM (Model-View-ViewModel) architecture tailored for Jetpack Compose. The application relies on unidirectional data flow, where the ViewModel exposes UI state as `StateFlow` and the UI communicates events back to the ViewModel.

### Modules

The project is structured into logical feature modules (packages) within the `app` module:
- **`core/`**: Shared models, utility functions, theme definitions, and reusable UI components (e.g., `ShimmerBrush`, `PostCard`).
- **`auth/`**: Authentication flows using Firebase Auth (Phone Number verification) and user registration.
- **`contacts/`**: Reading the local Android address book and syncing it against Firestore to find "On Vula" contacts.
- **`global/`**: Internet-dependent features like the main Feed, Stories, and Profiles.
- **`chat/`**: Real-time messaging implementation using Firestore real-time listeners.
- **`local/`**: Offline/mesh networking features using Android Network Service Discovery and C++ NDK Wi-Fi SSID hashing.

## 2. Core Components

### Database Strategy (Firestore)
- **Users (`users/{uid}`)**: Stores profile metadata, display names, and bio.
- **Posts (`posts/{postId}`)**: Contains image URLs, captions, author details, and `likedBy` arrays. We heavily utilize denormalization (e.g., storing `authorProfileImageUrl` directly on the post) to minimize read operations.
- **Chats (`chatRooms/{roomId}`)**: Tracks participants and metadata.
- **Messages (`chatRooms/{roomId}/messages/{msgId}`)**: Individual messages with read status and timestamps.

### Offline Persistence
Firestore offline caching is globally enabled in `VulaApplication.kt` via `PersistentCacheSettings`. This allows the feed, chats, and profiles to load instantly from disk when the user is offline, syncing changes once connectivity is restored.

### Dependency Injection
Hilt is used for DI. Modules like `ContactsModule` and standard Firebase modules bind repositories and Firebase instances, making ViewModels easily testable.

## 3. UI and UX Patterns

- **Theme:** "Emerald Green" branding. We enforce a light theme globally by manipulating the window status bar and navigation bar colors in `Theme.kt`.
- **Navigation:** Handled by a centralized `VulaNavGraph.kt` using Jetpack Navigation for Compose. The bottom navigation bar utilizes spring-based scaling animations for selection feedback.
- **Image Loading:** Handled exclusively by `Coil` with `AsyncImage` for memory-efficient loading of remote URLs.
- **Pagination:** Infinite scrolling in the feed is powered by Google's **Paging 3** library, utilizing a custom `FeedPagingSource` hooked into `limit()` and `startAfter()` Firestore queries.

## 4. Local Mode Networking
For mesh/local communication without internet:
- The app uses `WifiManager` to obtain the SSID/BSSID.
- A high-performance C++ module via JNI (`sha256.h`) securely hashes network identifiers.
- Users broadcast their presence using standard Android NSD or via specialized Firestore local presence documents when on the same Wi-Fi.

## 5. Security & Permissions
- `READ_CONTACTS`: Requested at runtime to populate the contacts list and match users.
- `ACCESS_FINE_LOCATION`: Required by Android for accessing Wi-Fi SSID/BSSID details for Local Mode.
- No direct database access is allowed without authentication; Firestore Security Rules handle data segregation.

## 6. Performance & Production Configuration
- **Minification & R8**: The `release` build uses `isMinifyEnabled = true` and `isShrinkResources = true`.
- **ProGuard**: Custom rules (`proguard-rules.pro`) ensure that Firebase data models (`com.vula.app.core.model.**`) and Dagger/Hilt dependency injection maps are not obfuscated, preventing runtime crashes.
- **Accessibility (A11y)**: Key UI components like `StoryCard` and `PostCard` utilize Jetpack Compose `semantics` to dynamically merge nodes (`mergeDescendants = true`) and provide context-aware content descriptions for Android TalkBack.

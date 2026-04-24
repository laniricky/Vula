# Vula Social Messenger

Vula is a modern, contacts-based social messenger application designed for Android. It bridges the gap between local, offline communication (mesh networking/Wi-Fi) and global internet-based interactions, creating a seamless and resilient social experience.

## Overview

- **Contacts-Based:** Built around your phone's address book. Hide your username from strangers; you are identified by your phone number to the people who matter.
- **Global Feed:** Share photos and captions with followers around the globe in an infinitely scrolling feed.
- **Stories:** View and post temporary updates that disappear after 24 hours.
- **Messaging:** Fast, real-time direct messages with read receipts and typing indicators.
- **Local Mode:** Discover and chat anonymously with people on the same Wi-Fi network without needing internet connectivity.

## Technology Stack

- **Architecture:** Clean MVVM (Model-View-ViewModel)
- **UI Framework:** Jetpack Compose (100% Kotlin)
- **Dependency Injection:** Hilt / Dagger
- **Backend & Database:** Firebase (Auth, Firestore, Storage, Messaging)
- **Local Networking:** Android NSD (Network Service Discovery) & C++ NDK for Wi-Fi hashing
- **Image Loading:** Coil
- **Pagination:** Paging 3

## Getting Started

1. **Prerequisites:** Android Studio Iguana or newer.
2. **Setup:** Clone the repository.
3. **Firebase:** Connect your Firebase project and ensure the `google-services.json` file is placed in the `app/` directory.
4. **Emulators:** For local development, the app connects to the Firebase Emulator Suite (Auth: `9099`, Firestore: `8088`, Storage: `9199`).

## Documentation

For a detailed technical overview of the architecture, components, and design decisions, please refer to the `DOCUMENTATION.md` file in the root directory.

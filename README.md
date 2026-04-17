# AloHi Android App 🚀

Welcome to **AloHi**, a modern, real-time messaging application for Android built with Kotlin and Jetpack Compose. This application is inspired by Zalo's User Experience, providing high-performance chat, robust offline capabilities, and seamless user interaction.

## ✨ Features

* **Real-time Messaging:** Powered by Socket.io for instant text, image, and voice message delivery.
* **Offline-First Architecture:** Leverages Room Database to cache conversations and messages locally, ensuring the app loads instantly without waiting for network requests.
* **Modern UI:** Built entirely with Android Jetpack Compose, featuring a dynamic, responsive, and beautiful Zalo-inspired design.
* **Voice & Video Calling (WebRTC):** Integrated with ZegoCloud Prebuilt Call Kit (and fully compatible with Firebase Cloud Messaging API V1 for background wake-up).
* **Media Handling:** Robust image picking, cropping, and voice recording capabilities.
* **Conversation Management:** Mute, Pin, Block, and Delete conversations matching Zalo's behavior.
* **Push Notifications:** Firebase Cloud Messaging (FCM) integration capable of reading both `notification` and `data-only` payloads, allowing seamless offline message and call alerting.

## 🛠️ Technology Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material Design 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Local Database:** Room (SQLite)
* **API Client:** Retrofit2 with OkHttp3
* **Real-time Communication:** Socket.io-client (v2.1.0)
* **Video/Voice Calling:** ZEGOCLOUD UIKits Prebuilt Call
* **Push Notifications:** Firebase Cloud Messaging (FCM)
* **Image Loading:** Coil
* **Asynchronous Programming:** Kotlin Coroutines & Flow

## 📋 Prerequisites

To run this application locally, you will need:
- Android Studio Iguana / Jellyfish (or newer)
- Gradle 8.x
- Minimum SDK: API 26 (Android 8.0)
- Target SDK: API 34 (Android 14)

## 🚀 Getting Started

1. **Clone the repository:**
   *(Clone the Git repository to your local machine)*

2. **Open the project in Android Studio:**
   - Select `File > Open` and choose the `AloHi` folder.

3. **Sync Gradle:**
   - Click "Sync Project with Gradle Files" to download all dependencies.

4. **Configure API Endpoints:**
   - Ensure the IP address in `ApiClient.kt` and `SocketManager.kt` matches your backend's local/public IP address.

5. **Run the Application:**
   - Connect your Android device or start an emulator.
   - Click the green `Play` button (Run 'app') in Android Studio.

## 📁 Project Structure

```text
com.example.alohi
├── data/
│   ├── local/        # Room DAOs, Entities, Database, DataStore (Token)
│   ├── remote/       # Retrofit Services, SocketManager, API Client
│   └── model/        # Kotlin Data Classes representing Backend APIs
├── service/          # Android Services (AlohiFirebaseMessagingService)
├── ui/
│   ├── components/   # Reusable Compose UI Widgets (Chat Bubble, Headers, etc)
│   ├── navigation/   # Jetpack Navigation Compose Routes
│   ├── screens/      # Full-page Compose Screens (Home, Chat, Profile)
│   ├── theme/        # Color, Typography, Shapes
│   └── viewmodel/    # MVVM ViewModels handling business logic
└── MainActivity.kt
```

## 🔒 Security Note

- The backend API (`alohi-api`) is explicitly separated and excluded from this repository via `.gitignore` to maintain structural separation and prevent leaked server secrets.

## 👥 Developers
Developed as a Graduation Capstone Project.

# 🏃‍♂️ Strava Native Android App

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/API-24%2B-3DDC84.svg?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A native Android application built using modern Android development practices to interact with the **Strava v3 API**. This app allows users to authenticate via OAuth2, view athlete profiles, browse activity feeds, track detailed activity stats, and analyze performance metrics.

---

## 🚀 Features

- **🔐 OAuth 2.0 Authentication**: Secure login and authorization workflow using Strava's API.
- **📊 Activity Feed**: View recent activities with distance, duration, elevation, and average speed/pace.
- **👤 Athlete Profile**: Display user details, total statistics, and achievements.
- **🗺️ Activity Detail View**: Comprehensive breakdown of individual workouts, including route maps, elevation profiles, and heart rate data (when available).
- **🌗 Clean UI / UX**: Material Design 3 guidelines with smooth interactions and responsive layouts.

---

## 🛠️ Tech Stack & Architecture

This application is built using modern Android architecture patterns and recommended Android Jetpack libraries:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles
- **UI**: Jetpack Compose / ViewBinding & XML
- **Asynchronous & Reactive**: Kotlin Coroutines & `StateFlow` / `LiveData`
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + [Moshi](https://github.com/square/moshi) / [Gson](https://github.com/google/gson)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) / [Koin](https://insert-koin.io/)
- **Image Loading**: [Coil](https://github.com/coil-kt/coil) / [Glide](https://github.com/bumptech/glide)
- **Local Storage / Caching**: [Room Database](https://developer.android.com/training/data-storage/room) & `EncryptedSharedPreferences` / DataStore

---

## ⚙️ Setup & Configuration

To run this project locally, you will need a **Strava Developer Account** to register an API Application.

### 1. Prerequisites
- Android Studio Hedgehog (or newer)
- JDK 17
- Android SDK with minimum API level 24 (Android 7.0)

### 2. Strava API Registration
1. Log in to your Strava account and navigate to [Strava API Settings](https://www.strava.com/settings/api).
2. Register a new application:
   - **Application Name**: `Strava Native Android (Dev)`
   - **Category**: `App` or `Visualizer`
   - **Authorization Callback Domain**: `localhost` (or your app's custom deep-link domain, e.g., `stravanative://redirect`)
3. Note down your **Client ID** and **Client Secret**.

### 3. Clone & Configure Project

Clone the repository:
```bash
git clone https://github.com/Vasik96/Strava-native-android.git
cd Strava-native-android
```

Add your API credentials to the `local.properties` file (located in the root folder):

```properties
STRAVA_CLIENT_ID="YOUR_CLIENT_ID"
STRAVA_CLIENT_SECRET="YOUR_CLIENT_SECRET"
STRAVA_REDIRECT_URI="stravanative://redirect"
```

*Note: Make sure `local.properties` is listed in your `.gitignore` so your secrets are not committed.*

---

## 📱 Screenshots

| Login / OAuth | Activity Feed | Activity Detail | Profile |
| :---: | :---: | :---: | :---: |
| *Add Screenshot* | *Add Screenshot* | *Add Screenshot* | *Add Screenshot* |

---

## 🧪 Running Tests

Run unit tests:
```bash
./gradlew test
```

Run UI / Instrumented tests:
```bash
./gradlew connectedAndroidTest
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git checkout -b feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

# Project Overview

This project is an Android TV application named "TV Sleep". Its primary purpose is to monitor the currently focused application and prevent access to the Android TV settings application (`com.android.tv.settings`). It functions as a security feature, force-stopping the settings app if it's opened.

The application is built with Kotlin and uses the following key technologies:
*   **Android SDK:** The core platform for building the application.
*   **Jetpack Compose:** For building the user interface.
*   **Kotlin Coroutines:** For asynchronous programming, particularly for the window monitoring service.
*   **Ktor:** For networking capabilities, likely for remote control or communication.
*   **Dadb:** A library for ADB (Android Debug Bridge) communication, used to monitor and control the device.

# Building and Running

To build and run this project, you should use Android Studio or the Gradle wrapper.

**Build the project:**
```bash
./gradlew build
```

**Install the application on a connected device:**
```bash
./gradlew installDebug
```

**Run the application:**
The application can be launched from the Android TV launcher after installation.

**Run tests:**
```bash
./gradlew test
```

# Development Conventions

The project follows standard Android development conventions.
*   **Language:** The primary language is Kotlin.
*   **Asynchronous Programming:** The project uses Kotlin Coroutines for background tasks, as detailed in the `WINDOW_MONITOR_SPECIFICATION.md`. The window monitoring service is a key part of the application and is implemented using a long-running coroutine.
*   **Dependency Management:** Dependencies are managed using Gradle and the `libs.versions.toml` file.
*   **User Interface:** The UI is built with Jetpack Compose for TV.
*   **Architecture:** The application uses a ViewModel-based architecture, as indicated by the presence of the `lifecycle-viewmodel-ktx` dependency.

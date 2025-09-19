# TV Timer+

**TV Timer+** is an Android TV application designed to help you manage your TV viewing time effectively. Its core feature is to put your device to sleep after a set timer finishes. With upcoming features, it aims to provide more control over your TV experience, including remote management and application access control during active timer sessions.

## ✨ Features

**Currently Implemented:**
*   **Sleep Timer:** Set a timer, and TV Timer+ will automatically put your Android TV device to sleep when the time is up.
*   **User-Friendly Interface:** Built with Jetpack Compose for a modern and intuitive TV experience.
*   **Basic Timer Controls:** Start, stop, and reset timer functionality.

**Planned Features:**
*   **Local Network Remote Control:** Manage TV Timer+ (and potentially basic TV functions) from another device on your local network via an integrated Ktor-based web server.
*   **Application Watchlist:** Define which applications are allowed or blocked while a timer is active.
*   **Customizable Timer Actions:** More options beyond just putting the device to sleep.
*   **Persistent Settings:** Save your preferences and timer presets.

## 🚀 Why TV Timer+?

In a world full of content, TV Timer+ helps you take control of your screen time. Whether it's ensuring the TV doesn't stay on all night or managing viewing habits, TV Timer+ aims to be a simple yet powerful tool for your Android TV. The upcoming remote control and app restriction features will further enhance its utility, making it a comprehensive solution for mindful TV usage.

## 🛠️ Technology Stack

*   **Platform:** Android TV
*   **Language:** Kotlin
*   **UI:** Jetpack Compose for TV
*   **Asynchronous Programming:** Kotlin Coroutines
*   **Networking (for planned remote control):** Ktor
*   **Build System:** Gradle

## ⚙️ Setup and Installation

### Prerequisites
*   Android Studio (latest stable version recommended)
*   An Android TV device or emulator

### Building the App
1.  Clone the repository:
    ```bash
    git clone [Your Repository URL]
    cd [Your Project Directory]
    ```
2.  Build the project using Gradle:
    ```bash
    ./gradlew assembleDebug
    ```
    (Or build directly from Android Studio)

### Installing the App
Connect your Android TV device or start an emulator. Then, you can install the app using Gradle:
```bash
./gradlew installDebug
```
(Or run directly from Android Studio)

## 🎮 Usage

1.  Launch TV Timer+ from your Android TV's app launcher.
2.  Navigate to the timer setup screen.
3.  Set your desired duration.
4.  Start the timer.
5.  The app will display the remaining time and put the TV to sleep once the timer elapses.

## 🤝 Contributing

Contributions are welcome! If you'd like to help improve TV Timer+ or add new features, please follow these steps:

1.  **Fork** the repository on GitHub.
2.  Create a new **branch** for your feature or bug fix: `git checkout -b feature/your-feature-name` or `bugfix/issue-description`.
3.  Make your changes and **commit** them with clear, descriptive messages.
4.  **Push** your changes to your forked repository.
5.  Open a **Pull Request** to the `main` branch of this repository.

Please ensure your code adheres to the project's coding style and conventions. If you're planning a significant change, please open an issue first to discuss it.

## 📄 License

This project will be open source. Please choose a license that fits your needs (e.g., MIT, Apache 2.0) and add a `LICENSE` file to the repository. For example:

```
This project is licensed under the MIT License - see the LICENSE.md file for details.
```

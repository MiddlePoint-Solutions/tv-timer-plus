# TV Timer+

TV Timer+ helps you manage your TV with a simple countdown timer that automatically puts your device to sleep.
Perfect for Android TV based devices and set-top boxes, it’s the easiest way to prevent late-night streaming or manage children’s screen time.

## ⚙️ How it works

TV Timer+ communicates directly with your device through the built-in local ADB service on Android TV.
This allows the app to send secure commands that manage TV functions — such as sleep mode today, and more parental control options in future updates.

## ✨ Features

- ⏱️ **Preset timer selection** – quickly choose a preset duration.
- ⏳ **Countdown timer** – puts the TV to sleep automatically when finished.
- 🔒 **Completely local** – no internet connection required for core functionality.

## 📊 Analytics and Crash Reporting

To help improve the app, TV Timer+ uses Firebase for basic, anonymous analytics and crash reporting. This includes:
- **Analytics**: Understanding which features are used most often.
- **Crashlytics**: Identifying bugs and crashes to make the app more stable.
  This data is completely anonymous and helps prioritize future development.

## 🔥 Firebase Setup

This project uses Firebase to handle analytics and crash reporting. If you are cloning or forking this repository, you will need to set up your own Firebase project.

1.  Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2.  Add an Android app to your Firebase project with the your package name.
3.  Download the `google-services.json` file provided during the setup process.
4.  Place the `google-services.json` file in the `androidMain/` directory of this project.

## 🛠️ Roadmap

Planned features for future releases:
- 📡 **Local network remote control** (bundled server).
- ✅ **Allow-list for apps** – restrict which apps can run when the timer is active.
- ⏰ **Custom timer durations** – add your own time intervals.
- 🛑 **More actions than sleep** – additional options once the timer ends.

## 🤝 Contributing

Contributions are welcome! Feel free to open issues, suggest features, or submit pull requests.

## 🙏 Acknowledgments

This project was inspired by the excellent open-source project:
- [LADB](https://github.com/tytydraco/LADB)

## 🔒 Privacy Policy

TV Timer+ is built with **privacy first**.
- ✅ All core functionality happens **locally on your device**.
- 🚫 No personal data is collected or transmitted.
- 📈 Anonymous analytics and crash reports are collected via Firebase to help improve the application. This data does not contain any personally identifiable information.

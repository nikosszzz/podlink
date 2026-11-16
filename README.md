<h1 align="center"> Podlink </h1>
<div align="center">🎧⚡ An Android companion app for Apple AirPods with Material 3 Expressive UI, direct Bluetooth L2CAP/AACP control ⚡🎧</div>
<br>

<div align="center">
    <img alt="GitHub Repo stars" src="https://img.shields.io/github/stars/nikosszzz/podlink?style=flat-square&label=%F0%9F%8C%9F%20Stars&labelColor=3c3836&color=1d2021">
    <img alt="GitHub forks" src="https://img.shields.io/github/forks/nikosszzz/podlink?style=flat-square&label=%F0%9F%94%97%20Forks&labelColor=3c3836&color=1d2021">
    <img alt="GitHub Issues" src="https://img.shields.io/github/issues/nikosszzz/podlink?style=flat-square&label=%E2%9A%A0%EF%B8%8F%20Issues&labelColor=3c3836&color=1d2021">
    <img alt="GitHub commit activity (branch)" src="https://img.shields.io/github/commit-activity/t/nikosszzz/podlink/main?style=flat-square&label=%F0%9F%A7%BE%20Commits&labelColor=3c3836&color=1d2021">
</div>

<hr>

## ✨ Podlink Features

- Live battery percentages & charging animation for Left, Right, Case, and single-bud connections
- Pixel-style expandable Device Card battery notification + Android 17 Live Update chip option
- Noise control switching (Off, Transparency, Adaptive Audio w/ strength slider, Noise Cancellation) inside the app, Quick Settings tile, popup Quick Panel, and home screen widgets
- Configurable stem press actions (single, double, triple, long press) & long-press cycle modes
- In-ear detection with automatic media pause/resume, Conversation Awareness (voice detection with media volume ducking or pausing), Personalized Volume, and Loud Sound Reduction
- Transparency Mode tuning (amplification, balance, tone, ambient noise reduction, conversation boost, own-voice boost) plus Hearing Protection & Hearing Aid controls over BLE ATT
- 3-band Custom Equalizer + Headphone Accommodations
- Head Gestures & real-time head tracking preview
- Minimal background battery drain using Android's `CompanionDeviceService` - wakes up only when your AirPods connect and goes back to sleep when they disconnect

## ❓ How to run

- Install Android 13 or newer (built against SDK 33 min / SDK 37 target).
- Pair your AirPods in Android Bluetooth settings first.
- Open Podlink, grant Nearby Bluetooth and notification permissions, and link your AirPods in the companion prompt so battery notifications trigger automatically on connect.
- If you want Android Bluetooth settings/widgets to show pod & case battery natively, install and start **Shevery**, then turn on **Report battery to Android (Shevery)** in settings.
- To build a release APK yourself:
    - Install JDK 21 and Android SDK (Platform 37).
    - Run `./android/gradlew -p android assembleRelease`.
    - Grab the APK from `android/app/build/outputs/apk/release/`.

## ❓ Extra information

- Supported models: `AirPods 1`, `AirPods 2`, `AirPods 3`, `AirPods 4`, `AirPods 4 (ANC)`, `AirPods 5`, `AirPods 5 (Wireless Charging Case)`, `AirPods Pro 1`, `AirPods Pro 2 (Lightning & USB-C)`, and `AirPods Pro 3`.
- Works without root on Android builds whose Bluetooth stack allows L2CAP PSM `0x1001` (such as Android 16 QPR3+ on Pixel, OxygenOS/ColorOS 16, OneUI 9, and Android 17). On OEM builds that restrict hidden BluetoothSocket APIs or older Fluoride stacks, ART hidden API access or a Bluetooth hook may be required.

## 🛠️ Contributions

If you think you can improve Podlink, submit a pull request with clean and efficient code. Unoptimized or bloated changes will be rejected or asked to be rewritten.

## 🤝 Support

Open an issue on [GitHub](https://github.com/nikosszzz/podlink/issues) and attach diagnostics copied from `Settings -> Live diagnostics -> Copy report`.

## 🌟 Credits

- Maintained by [@nikosszzz](https://github.com/nikosszzz).

- [HmnDev-Tech/Shevery-API](https://github.com/HmnDev-Tech/Shevery-API) for the privileged Shizuku-compatible Android battery metadata bridge.
- [LSPosed/AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) and everyone in the open-source Apple accessory protocol RE community.

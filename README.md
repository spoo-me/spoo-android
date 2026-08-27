<h3 align="center">spoo.me for Android</h3>
<p align="center">Shorten, manage, and analyze your spoo.me links from your pocket 🚀</p>

<p align="center">
    <a href="#-features"><kbd>🔥 Features</kbd></a>
    <a href="#-getting-started"><kbd>🚀 Getting Started</kbd></a>
    <a href="#-widgets"><kbd>📊 Widgets</kbd></a>
    <a href="#-development"><kbd>🛠️ Development</kbd></a>
    <a href="#-contributing"><kbd>🤝 Contributing</kbd></a>
</p>

<p align="center">
<a href="https://spoo.me"><img src="https://img.shields.io/badge/spoo.me-6a5cf4?logo=https://spoo.me/static/images/favicon.png" alt="spoo.me"></a>
<a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.4-6a5cf4?logo=kotlin&logoColor=white" alt="Kotlin"></a>
<a href="https://spoo.me/discord"><img src="https://img.shields.io/discord/1192388005206433892?logo=discord" alt="Discord"></a>
<a href="https://github.com/spoo-me/spoo-android/blob/main/LICENSE"><img src="https://img.shields.io/static/v1.svg?style=flat&label=License&message=AGPL-3.0&colorA=363a4f&colorB=b7bdf8" alt="License"></a>
</p>

# 🔥 Features

- `Share to Shorten` - Send any link from any app's share sheet and it lands in the create sheet, ready to go ⚡
- `Quick Settings Tile` - Shorten whatever is on your clipboard without opening the app 📋
- `Emoji Aliases` - Pick from the server's emoji set with a built-in picker, rendered in Fluent 3D instead of your keyboard's glyphs 🎨
- `Link Management` - Passwords, click limits, expiry with a real date and time picker, private stats, and bot blocking, all editable after the fact 🔧
- `Analytics` - Clicks over time, plus breakdowns by country, browser, OS, and referrer, filterable and scoped per link 📈
- `Home-screen Widgets` - Six chart styles, configured per instance, cached so they render stale data instead of spinners 📊
- `Styled QR Codes` - Circle-module QR with the spoo mark in the middle, shareable as an image or saved to your gallery 🔳
- `Material You` - Dynamic color from your wallpaper, or a seed color of your choosing, across the app and its widgets 🎭
- `Linked App Auth` - Sign in through your browser with PKCE. No API keys to paste 🔑

# 🚀 Getting Started

The app is not on the Play Store yet. Until it is, build it yourself:

```bash
git clone https://github.com/spoo-me/spoo-android.git
cd spoo-android
./gradlew installDebug
```

You need JDK 21 and an Android device or emulator running API 26 or newer.

> [!NOTE]
> Debug builds point at a local backend. To run against production, change `SPOO_BASE_URL` in `app/build.gradle.kts` or install a release build.

Sign in from the Links tab. The app opens a Custom Tab, you approve there, and the session comes back to the app and refreshes itself from then on.

# 📊 Widgets

Three shells show up in the widget picker, and each one opens a configuration screen where the real choice happens: chart style, metric, time range, and whether the widget tracks all your links or a single one. Breakdown charts also take dimension filters, the same vocabulary the Analytics tab uses.

| Style | Shows |
| --- | --- |
| `Wave` | Clicks over time as a filled curve, with the total on top |
| `Bars` | Clicks over time as bars, peak accented |
| `Number` | The total alone, in Roboto Flex, Serif, or Mono |
| `Treemap` | Top values in a dimension, sized by share |
| `Bubbles` | The same breakdown as packed circles |
| `Map` | Clicks by country on a world map |

> [!TIP]
> Widgets cache their last fetch per instance, so they show the numbers you saw last rather than a spinner while they refresh. Long-press to reconfigure one at any time.

# 🛠️ Development

Kotlin and Jetpack Compose, with Material 3 Expressive. The API layer is the [official Kotlin SDK](https://central.sonatype.com/artifact/me.spoo/spoo), so nothing in this repo hand-rolls HTTP.

```bash
./gradlew installDebug          # build and install the debug variant
./gradlew lintDebug             # Android Lint, warnings fail the build
pre-commit run --all-files      # ktlint, autofix on
```

Settings has a mock-data switch in debug builds. Turn it on to browse a generated set of links and stats with no backend at all, which is the fastest way to work on UI.

> [!IMPORTANT]
> Widget code runs in a different process than the app and stays frozen while cached, so anything a widget renders has to come from its own Glance state rather than from values captured when it was created.

# 🤝 Contributing

Issues and pull requests are welcome. Open one at [issues](https://github.com/spoo-me/spoo-android/issues/new) or [pulls](https://github.com/spoo-me/spoo-android/pulls), or come talk it through on [Discord](https://spoo.me/discord) first if it's a big change.

For anything else, reach us at <kbd>[✉️ support@spoo.me](mailto:support@spoo.me)</kbd>.

---

<h6 align="center">
<img src="https://spoo.me/static/images/favicon.png" height=30 title="Spoo.me Copyright">
<br>
© spoo.me . 2026

All Rights Reserved</h6>

<p align="center">
 <a href="https://github.com/spoo-me/spoo-android/blob/main/LICENSE"><img src="https://img.shields.io/static/v1.svg?style=for-the-badge&label=License&message=AGPL-3.0&logoColor=d9e0ee&colorA=363a4f&colorB=b7bdf8"/></a>
</p>

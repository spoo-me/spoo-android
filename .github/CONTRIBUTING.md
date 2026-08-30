# Contributing Guidelines

Thanks for considering a contribution to the spoo.me Android app. Every fix helps.

This repo is the Android client only. Anything about the API, redirects, the database or self-hosting the backend belongs in [spoo-me/spoo](https://github.com/spoo-me/spoo), and the web app lives in [spoo-me/frontend](https://github.com/spoo-me/frontend).

## Quick Start

You do not need a backend to work on the UI. The app ships a mock data source.

```bash
# Fork and clone
git clone https://github.com/YOUR_USERNAME/spoo-android.git
cd spoo-android

./gradlew installDebug
```

You need JDK 21 and an Android device or emulator running API 26 or newer.

In a debug build, open Settings and turn on the mock-data switch in the Developer section. The app fills with a generated set of links, stats and widgets with no backend and no account, which is the fastest way to work on UI.

To run against a real backend, debug builds point at a local one; change `SPOO_BASE_URL` in `app/build.gradle.kts` to use production.

## Development Workflow

1. Branch off `main`: `git checkout -b feat/my-change`
2. Make the change
3. Run the checks below
4. Push and open a pull request

## Checks CI runs

`.github/workflows/ci.yml` runs these on every pull request. Run them locally first.

```bash
./gradlew lintDebug             # Android Lint, warnings fail the build
./gradlew testDebugUnitTest     # unit tests
pre-commit run --all-files      # ktlint, autofix on
```

## Conventions

- **Kotlin and Jetpack Compose only.** No XML layouts, no Fragments, no AppCompat.
- **Material 3 Expressive.** Motion comes from `MaterialTheme.motionScheme` springs; a hardcoded `tween()` is a review flag. Components come from the pinned material3 alpha, so read its release notes before touching that version.
- **The API layer is the [Kotlin SDK](https://central.sonatype.com/artifact/me.spoo/spoo).** Nothing in this repo hand-rolls HTTP against the backend.
- **Widgets read their state from Glance.** Widget code runs in a different process than the app and stays frozen while cached, so anything a widget renders has to come from its own Glance state rather than from values captured when it was created.
- **Comments explain why.** State the constraint the code cannot show. Do not narrate what the code already says.

## Pull Request Guidelines

- Keep the PR focused on one change
- All CI checks must pass
- Screenshots or a short clip for anything visual, in both light and dark theme
- [Conventional commit](https://www.conventionalcommits.org/) messages: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`. These are load-bearing here: commit types on `main` decide the next release version, see [docs/releasing.md](../docs/releasing.md)

## Getting Help

- [Discord](https://spoo.me/discord) for real-time help
- [GitHub Issues](https://github.com/spoo-me/spoo-android/issues) for bugs and feature requests
- [Documentation](https://docs.spoo.me) for API details

## License

AGPL-3.0. See [LICENSE](../LICENSE). By contributing you agree your work is licensed the same way.

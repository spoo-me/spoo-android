# Security Policy

## Supported Versions

Security fixes are applied to the latest version. Install the newest release from [GitHub Releases](https://github.com/spoo-me/spoo-android/releases/latest), or use Obtainium so updates arrive on their own.

## Reporting a Vulnerability

If you discover a security vulnerability, please report it by emailing [security@spoo.me](mailto:security@spoo.me). Do not open a public issue. We will respond as quickly as possible.

Please include:

- A description of the vulnerability and its impact.
- Steps to reproduce it, including the app version and Android version.
- Any potential fixes or mitigations you have identified.

Findings that affect the API, redirects or the database belong in [spoo-me/spoo](https://github.com/spoo-me/spoo/security), but the same address works for both. When in doubt, send it here.

## Scope Notes

- The app signs in through the browser with PKCE and holds scoped tokens only. There are no API keys or account passwords stored on the device.
- Revealed passwords in the link editor mark the window secure, so they stay out of screenshots, screen recordings and the Recents thumbnail. Stats screens deliberately stay capturable.
- Every release ships a `.sha256` checksum next to the APK, and all APKs are signed with the same certificate. An update that Android refuses to install over the existing app did not come from us.

## Security Updates

We announce security updates through the [GitHub repository](https://github.com/spoo-me/spoo-android) and the [Discord server](https://spoo.me/discord). Watch the repo to stay informed.

## Contact

Questions or anything else: [security@spoo.me](mailto:security@spoo.me).

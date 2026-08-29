# Releasing

Releases publish themselves. There is no tag to push and no APK to upload.

## How a release happens

Every push to `main` runs the `release` workflow, which reads the commits
since the last tag:

| Commits since the last tag | Result |
| --- | --- |
| a `feat:` | minor bump |
| a `fix:` or `perf:` | patch bump |
| a `!` marker or `BREAKING CHANGE:` | major bump |
| anything else (`chore:`, `docs:`, `test:`) | no release |

When there is something to release, CI computes the version, builds and
signs the APK, creates the tag and the GitHub release, and attaches the APK
with its SHA-256. To release without a qualifying commit, run the workflow
manually from the Actions tab and pick a bump.

The git tag is the version. `versionName` comes from it at build time and
`versionCode` is derived (`1.2.3` becomes `10203`), so the code can only
ever increase and nothing is committed back to the repo.

## Installing

Grab the APK from [the latest release][latest], or track the repo in
[Obtainium][obtainium] to get updates automatically: add an app, paste this
repository's URL, and Obtainium follows every release from then on.

Verify a download before installing it:

```bash
sha256sum -c spoo-v0.1.0.apk.sha256
```

[latest]: https://github.com/spoo-me/spoo-android/releases/latest
[obtainium]: https://github.com/ImranR98/Obtainium

## One-time setup: the signing key

CI needs an upload key before it can publish anything. Create one, then
store it and its passwords in the `release` environment:

```bash
# macOS ships a keytool stub with no JDK behind it; call the real one.
KEYTOOL=$(/usr/libexec/java_home 2>/dev/null)/bin/keytool
[ -x "$KEYTOOL" ] || KEYTOOL=/opt/homebrew/opt/openjdk@21/bin/keytool

mkdir -p ~/.spoo
"$KEYTOOL" -genkeypair -keystore ~/.spoo/upload-key.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=spoo.me, O=spoo.me, C=IN"

gh api -X PUT repos/spoo-me/spoo-android/environments/release
base64 < ~/.spoo/upload-key.jks | tr -d '\n' \
  | gh secret set KEYSTORE_BASE64 --env release
gh secret set KEYSTORE_PASSWORD --env release
gh secret set KEY_ALIAS --env release --body upload
gh secret set KEY_PASSWORD --env release
```

Passwords must be at least six characters; `keytool` rejects shorter ones.
Keep the keystore outside the repo, and do this **before** merging anything
releasable, or the first release fails on the missing key.

> [!IMPORTANT]
> The upload key is the permanent identity of the app. Every future update
> must be signed with the same key, so back up the keystore and its
> passwords somewhere durable. Losing it means existing installs can never
> be updated, only uninstalled and replaced.

## Play Store, later

Play wants an AAB rather than an APK, and Play App Signing means Google
holds the final key while yours becomes the upload key. Keep using the same
upload key there, otherwise the Play build and the GitHub build are
different apps to Android and cannot upgrade across each other.

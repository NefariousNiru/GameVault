# GameVault 1.0.0 — signed APK release checklist

This source snapshot fixes the bottom-nav corner artifact and prepares a **signed GitHub APK release**. The APK itself is intentionally **not included**: the permanent private signing keystore, Firebase app configuration, and Android SDK were not supplied to the packaging environment. Never sign the first public version with an expendable key or share your key in a ZIP.

## 1. Keep your existing installation/data safe

1. In the existing app, export a JSON backup using **Settings → Export Library** (and save it somewhere private). Verify the export opens as JSON before continuing.
2. Verify that any pending offline changes have synchronized to Firestore. The Android package ID stays `com.gamevault.app`; database schema is unchanged.
3. A debug-signed APK **cannot** be upgraded in place to one signed by your new release key. Android rejects updates signed with a different certificate. You will need to uninstall the debug version and install the signed release; uninstalling erases that device's app-local caches, keys and locally saved data. After installing, sign into the same Google account, enter RAWG/SteamGridDB keys again and verify Firestore data; use JSON import only if necessary.
4. **Never uninstall an app with pending offline writes** until they are synced or backed up.

## 2. Create a permanent signing key (on your own trusted workstation)

```bash
bash tools/create_release_keystore.sh
# Defaults to ~/.config/gamevault/gamevault-release.p12
# Prompts you for a password; choose a strong one.
```

**Back up the keystore and password offline. Losing either means you cannot create compatible APK updates signed with the original certificate. Do not commit or send the keystore, password, or encoded versions to anyone.**

Use this to view your signing certificate fingerprints:

```bash
keytool -list -v -keystore "$HOME/.config/gamevault/gamevault-release.p12" -alias gamevault
```

In Firebase Console → Project Settings → Your apps → Android `com.gamevault.app`, add the **release certificate's SHA-1 and SHA-256** fingerprints. This is particularly important for Google Sign-In. Download the updated `google-services.json` and place it at `app/google-services.json` on your PC. It is gitignored.

## 3. Publish source to your GitHub repository

Push **this complete source** to [NefariousNiru/GameVault](https://github.com/NefariousNiru/GameVault), including `.github/workflows/android-release.yml`, `app/build.gradle.kts`, and `tools/release_helpers.sh`. Keep the `.gitignore`. Add actual screenshots to `screenshots/library.png` and `screenshots/queue.png` when ready.

If you previously committed `app/google-services.json`, `keystore.properties` or other secrets, stop and remove them from Git history before making the repository public. `.gitignore` does not retroactively remove tracked files.

## 4. Add the five GitHub Actions secrets

Open **Repository → Settings → Secrets and variables → Actions → New repository secret** and supply:

| Name | Value |
|---|---|
| `GV_KEYSTORE_BASE64` | Base64-encoded bytes of your permanent `.p12` keystore |
| `GV_KEYSTORE_PASSWORD` | Permanent keystore password |
| `GV_KEY_ALIAS` | `gamevault` |
| `GV_KEY_PASSWORD` | Key password (usually identical for PKCS12) |
| `GV_GOOGLE_SERVICES_JSON_BASE64` | Base64-encoded updated `app/google-services.json` |

On Linux generate base64 without exposing the secret to the console:

```bash
base64 -w0 "$HOME/.config/gamevault/gamevault-release.p12" | gh secret set GV_KEYSTORE_BASE64
base64 -w0 app/google-services.json | gh secret set GV_GOOGLE_SERVICES_JSON_BASE64
```

The `gh secret set` commands require GitHub CLI authenticated to the correct repo. You can also use the GitHub website to paste values securely. Set the passwords using the GitHub Secrets website or `gh secret set GV_KEYSTORE_PASSWORD`, etc.; don't pass passwords as shell arguments or commit them. Base64 is **not encryption**; treat encoded values as credentials.

## 5. Build & publish

From the GitHub repository's **Actions** tab:

1. Select **Android release APK** → **Run workflow**.
2. Enter tag `v1.0.0` and leave **publish = false** for the first run. The workflow runs unit tests, builds the release, verifies the APK's signing certificate, and exposes the signed APK as a downloadable workflow artifact for device testing.
3. Install that APK on a clean test device, sign in, check sync, play queue, backup/import, offline mode, and both Settings links. A different release signing key from the old debug version requires uninstall first (see step 1).
4. Once the signed build is confirmed, rerun the workflow with **publish = true**, tag `v1.0.0`. It publishes **two direct Release assets** to [GitHub Releases](https://github.com/NefariousNiru/GameVault/releases): `GameVault-v1.0.0.apk` (installable APK, not ZIP) and `SHA256SUMS.txt`. The Release description also shows the APK's actual SHA-256 hash and a direct APK download link. Repeated use of the same release tag is intentionally blocked; bump `versionCode` and `versionName` for the next version.

Workflow needs Actions enabled, repository write access, and permission to create GitHub releases. If repository policy prevents `GITHUB_TOKEN` from creating releases, download the signed APK artifact and publish it manually as a release asset.

### If Android SDK setup fails on GitHub Actions

The release workflow uses `android-actions/setup-android@v4` with `packages: 'platform-tools'`.
Older `setup-android@v3` workflows may fail with `Warning: Failed to find package 'tools'` because
Google retired that SDK package. The current action installs command-line tools and licenses without
requesting `tools`. To apply the fix to your existing repository, replace
`.github/workflows/android-release.yml` with the version in this source package, commit, and push it,
then rerun the workflow **without publishing** first. This is an SDK setup failure, not a Kotlin compile error.

### Android 17 SDK package naming (API 37.0)

Android API 37's SDK platform is distributed as **`platforms;android-37.0`**, not
`platforms;android-37`. A GitHub Actions run using the old SDK package string
fails with `Failed to find package 'platforms;android-37'` before Gradle starts.

The workflow installs `sdkmanager --install 'platforms;android-37.0'`, and the app's
`compileSdk` explicitly requests the minor release:

```kotlin
compileSdk { version = release(37) { minorApiLevel = 0 } }
```

Both changes are necessary: merely installing 37.0 while leaving `release(37)`
without the `minorApiLevel` selector can make Gradle look for `android-37`.
No downgrade of AGP, Kotlin, Java, or Android SDK is required. An SDK Manager
message stating that `sdkmanager` is deprecated is a **warning**, not the failure.

### Gradle configuration-cache compatibility (release signing guard)

If the GitHub build reaches `:app:assembleRelease` but fails with
`Task :app:checkReleaseSigning ... cannot serialize Gradle script object references`,
update `app/build.gradle.kts` and add `tools/check_release_signing.sh` from this
source package. The previous custom `doLast` task captured the Kotlin build script,
which Gradle 9.6 cannot store in its configuration cache. The replacement uses
Gradle's built-in, configuration-cache-compatible `Exec` task to run the signing
preflight before final assembly. It still checks that the required signing
variables are set and the keystore exists. **Do not disable the configuration
cache or remove the signing guard.**

### Build locally, instead

Once your working project has Android SDK, Gradle (or a real Gradle wrapper JAR), a permanent keystore, and `app/google-services.json`:

```bash
bash tools/build_release_local.sh
# Output: app/build/outputs/apk/release/app-release.apk
```

The script asks for the signing path/passwords without embedding them in tracked files, runs JVM unit tests and `assembleRelease`, and verifies the signature with Android `apksigner`. Use the **same keystore** for CI and every later release.

### APK download versus workflow artifact

GitHub Actions **run artifacts** under the Actions tab may be downloaded as ZIP archives. Those are for testing; they are **not** the public installer. With **publish = true**, the workflow attaches the actual `GameVault-v1.0.0.apk` file directly to the GitHub Release, alongside `SHA256SUMS.txt`. GitHub also displays automatically generated "Source code (zip)" and "Source code (tar.gz)" links on release pages; these are source snapshots, not the installable Android app.

From the folder containing the two downloaded release files, check the APK integrity using:

```bash
sha256sum -c SHA256SUMS.txt
```

The same SHA-256 is printed in the Release description. The workflow verifies the signed APK with `apksigner` before uploading and double-checks that both Release assets are present.

**If you already published `v1.0.0`:** The publish workflow intentionally refuses to replace an existing release. Running the new workflow again with `publish = true` will not overwrite it. To add the new checksum/direct APK to an existing release, inspect the current assets first and explicitly upload the verified files with `gh release upload v1.0.0 ... --clobber`, then edit the release description. Do not publish a second APK signed with a different certificate.

## 6. Release acceptance / future updates

- Confirm APK signature: `apksigner verify --verbose --print-certs app-release.apk` (Android SDK Build Tools).
- Confirm app displays **1.0.0** in Settings; package ID remains `com.gamevault.app`.
- Verify Google sign-in with the release certificate registered in Firebase.
- Verify game data, search, queue reordering, backup/restore and offline cache on the real phone.
- Keep your permanent keystore safe. For 1.0.1+ bump `versionCode` (must strictly increase) and `versionName`, then update the workflow tag. Future releases signed with the **same key** install as upgrades without deleting the app's data.

## Environment limitations

The generated source ZIP does not bundle `gradle/wrapper/gradle-wrapper.jar` (previous source archives omitted it). CI intentionally installs Gradle 9.6.0 with `gradle/actions/setup-gradle`, so the GitHub workflow does not require the wrapper. Existing local GameVault projects generally already have a Gradle wrapper JAR; otherwise install Gradle 9.6.0 locally or use the CI workflow. The Android app requires its `google-services.json` to build and must not be packaged with a private signing key.

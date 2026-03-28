# Quartz Mobile Android

Android foundation for Quartz Platform (Kotlin + Compose + MVVM + Clean Architecture + Hilt + Room + WorkManager).

## Structure

- `app/src/main/java/com/quartz/platform/presentation`: Compose UI, navigation, ViewModels
- `app/src/main/java/com/quartz/platform/domain`: domain models, repository contracts, use cases
- `app/src/main/java/com/quartz/platform/data`: Room, offline-first repositories, sync worker, demo snapshot source
- `app/src/main/java/com/quartz/platform/device`: device integration (network monitor)
- `app/src/main/java/com/quartz/platform/core`: logging, dispatcher abstractions, DI core

## MVP slice currently implemented

- `SiteListScreen`: local cached site list with loading/empty/error states.
- `HomeMapScreen`: map-centered home shell with local site markers, search/filter, selected-site navigation, and recenter baseline.
- `SiteDetailScreen`: technical site snapshot (`sectors/antennas/cells`) + local draft list + sector entry point to guided XFeeder/MixFeeder sessions.
- `XfeederGuidedSessionScreen`: local-first guided shell for sector workflow (session history, resume latest or start new, checklist step status, completion guard on required steps, sector outcome, structured closure evidence summary, notes and result summary), explicitly non-automated radio logic.
- `ReportDraftScreen`: offline draft editing + sync state visibility (`LOCAL_ONLY`, `PENDING`, `SYNCED`, `FAILED`) + local projection of guided-session closure evidence (sector outcome / related sector / unreliable reason / observed sector count).
- `ReportListScreen`: local-first list of site reports/drafts with sync-state visibility and failed-sync retry action.
- Sync traceability is persisted per draft job: last attempt timestamp, retry count, and short failure reason.
- MVP UI text/labels/messages are centralized in `app/src/main/res/values/strings.xml` with a consistent terminology baseline (sync states/actions, empty/error states, report actions).
- Debug builds expose an explicit `DEBUG ONLY - Simulation Sync` panel in `ReportDraftScreen` to force:
  - normal success
  - next retryable failure
  - fail once then success
  - next terminal failure
- Release builds do not include this simulation controller binding, so normal stub success behavior is preserved.
- Debug builds also expose a `DEBUG DEMO SCRIPT - Sync Flow` card in `ReportDraftScreen` with step-by-step verification scenarios:
  - success path
  - retryable failure path
  - terminal failure path with manual retry
- Room persists drafts (`report_drafts`) across app restarts.
- Current sync transport is a demo stub (`StubSyncGateway`) that always returns success.
- Sync enqueue triggers an immediate WorkManager pass (plus recurring schedule) for faster demo feedback.
- Report sync enqueue is blocked when local edits are unsaved, to prevent syncing stale revisions.

## First-run behavior

- No hidden fake production seed is injected.
- If local cache is empty, the site list explicitly explains the situation.
- For local demo runs, a user-visible action is available: `Load local demo snapshot`.
- This demo snapshot path is isolated and clearly demo-only.

## Android Studio setup

1. Open `mobile-android/` in Android Studio.
2. Copy `local.properties.example` to `local.properties`.
3. Set `sdk.dir` to your local Android SDK path.
4. Sync Gradle project.

## Quick terminal bootstrap (macOS + Homebrew)

```bash
brew install --cask android-commandlinetools
yes | sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
cat > local.properties <<'EOF'
sdk.dir=/opt/homebrew/share/android-commandlinetools
EOF
```

For connected instrumentation tests, also install:

```bash
yes | sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools \
  "emulator" "system-images;android-35;default;arm64-v8a"
```

## Useful commands

- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`

## Sequential quality gate (recommended)

Run from repository root:

1. `./scripts/android-quality-gate.sh`
2. Optional connected run (device/emulator online): `./scripts/android-quality-gate.sh --with-connected`

What it executes in deterministic order:
- `:app:assembleDebug`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:compileDebugAndroidTestKotlin`
- `:app:connectedDebugAndroidTest` (only with `--with-connected`)

Notes:
- The script fails fast on low disk space and missing SDK path.
- It runs Gradle with `--no-daemon` by default for stability.
- It does not suppress lint checks.

## Connected instrumentation tests (VS Code + terminal)

Run from repository root:

1. `./scripts/android-connected-tests.sh --preflight`
2. `./scripts/android-connected-tests.sh`

What the preflight checks:
- `mobile-android/local.properties` and `sdk.dir`
- Android SDK visibility
- `adb` availability
- at least one online Android target (`adb devices`)

If no target is connected, the script prints exact next commands to:
- install emulator/system image packages
- create an AVD
- start an emulator headless from terminal

Common failure taxonomy:
- `No online Android device/emulator`: machine runtime issue (start emulator or plug a device)
- `IO exception while downloading manifest` (sdkmanager): external network/artifact fetch issue
- Gradle dependency download timeout from `dl.google.com`: external artifact/network issue
- test assertion failure in `androidTest`: code/test issue

## Notes

- `local.properties` is intentionally ignored by git.
- The project requires a valid local Android SDK to compile and run tests.
- If `sdk.dir` is missing, Gradle fails with `SDK location not found`.
- `StubSyncGateway` is demo-only and must be replaced by a real API adapter for production sync behavior.
- If you hit stale resource merge issues, run `./gradlew clean` once and retry.

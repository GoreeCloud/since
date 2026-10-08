# GoreeCloud Since

GoreeCloud Since is a native, privacy-first Android app for elapsed-time trackers, streaks, goals, and milestones.

## Source ownership and status

The owner directed independent development in GoreeCloud/since on October 8, 2026. This pull-request candidate extracts the implementation and its Git history from GoreeCloud/android-app-defaults/apps/since/. The migration is not complete until exact-head validation, acceptance on the standalone default branch, and separate protected monorepo retirement.

**Development** — not Stable or production-qualified. Android 16 emulator tests are not representative-device acceptance.

## Technical baseline

Java 17, Kotlin 2.1.21, Jetpack Compose, AGP 8.10.1, Gradle 8.11.1 (CI-provisioned), Android SDK 36 (minimum API 29), Room3 3.0.3, SQLite 2.7.1. The app stays local-only with no Internet/location permissions and automatic backup disabled. Application ID com.goreecloud.since; side-by-side Development ID com.goreecloud.since.dev. Canonical artwork provenance is documented in BRANDING.md.

## Build and tests

Run the manifest check, unit tests, lint, and APK assembly with Java 17, Android SDK 36, and Gradle 8.11.1:

    python3 scripts/check_since_manifest.py
    gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest

CI verifies local-only boundaries, the committed Room schema, build/lint/tests, and Android 16 runtime instrumentation plus rendered evidence. A persistent protected Development signer and verified Gradle wrapper remain pending. No in-place update continuity is claimed from CI-debug signing.

## Records

- IMPLEMENTED-FEATURES.md — verified historical Since Development evidence
- PLANNED-FEATURES.md — open requirements and acceptance
- CHANGELOGS.md — Since change history
- BRANDING.md — approved canonical identity
- docs/MIGRATION.md — exact-source and history provenance

Glaze and all applicable Integral Platform Systems are evaluated separately; migration is not platform acceptance.

# GoreeCloud Since — Planned/Open Work

Historical owner requirements and acceptance obligations at migration; reconcile against verified current standalone source.

## GoreeCloud Since

### M0 — Foundation stabilization

**State:** Implemented on `main` for the bounded Development foundation; broader release acceptance remains open.

- PR #2 merged as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`.
- Exact candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`.
- Main-branch readback verified the merged source and fail-closed manifest.
- A verified Gradle Wrapper remains desirable when its binary/provenance can be introduced safely; current CI explicitly bootstraps Gradle 8.11.1.
- M0 completion is Development-only and does not imply Release Candidate, production, or Stable qualification.

### M1 — Persistent tracker fundamentals

**State:** In progress — persistence, create/Dashboard/Details, custom-start/Edit, and automated accessibility evidence are verified and merged; remaining M1 support/acceptance work stays open.

Verified on `main`:
- Room schema v1 for `tracked_events`, `event_periods`, and `event_goals`, with committed compiler-generated schema and CI drift detection.
- Exactly one open current period per tracker enforced at SQLite boundary.
- Permanent-event single-period, period chronology, and streak-only positive-goal invariants.
- Transactional tracker + initial period + optional goal creation.
- Repository/domain mapping boundary and Android 16 runtime invariant tests.
- PR #4 persistent user flow merged as `3cc9788513c100e0ca3fedb336dd57a1b3b87e84` after exact candidate `be534fc98e1fc7020296779f0e71cd4d48ae6778` passed run `35956066300` with `SinceDatabaseRuntimeTest` `OK (8 tests)`.
- Tracker Type Chooser, validated Create Tracker and persistent save.
- Reactive populated Dashboard and tracker-card Details navigation.
- Tracker Details backed by persisted tracker/period/goal state and derived `TimeEngine`.
- Persisted display-format changes from Details.
- Shared application clock plus lifecycle-aware minute refresh for visible elapsed values.

Verified on `main` through PR #6:
- User-selected past local date/time with explicit IANA zone editing.
- Deterministic DST gap/overlap resolution.
- Edit Tracker for title, note, default display format, and open current-period start/date/time/zone.
- Closed streak history remains immutable; an edited current start cannot precede the latest closed-period end.
- Exact candidate `89775713a83726df94e9592bf81358d81772d62f` passed run `35959431091` with Android 16 `SinceDatabaseRuntimeTest` `OK (10 tests)` and was squash-merged as `48d7c8342ad17e860b521690df5c817d84d92b7b`.

Verified on `main` through PR #9:
- Explicit screen heading semantics.
- Coherent TalkBack grouping for tracker cards.
- Full-row accessible display-format radio choices and streak-goal toggle target.
- Assertive error live regions.
- One shared lifecycle-aware Dashboard minute ticker rather than one ticker coroutine per card.
- Android Compose accessibility instrumentation including the custom-past-start Create → Details → Edit persistence flow.
- Android 16 runtime instrumentation remains mandatory with KVM detection and software-acceleration fallback.
- Exact candidate `b0ccc4e1f00c089ad8257f50ffb167df4b9e7af6` passed run `35963071718` with Android 16 instrumentation `OK (13 tests)` and was squash-merged as `5b14c58af68b8748e71f6af0c462650d06e852d6`.

Verified on `main` through PR #12:
- 2× font-scale Create-editor reachability instrumentation.
- Forced RTL Create-editor reachability and heading instrumentation.
- Exact candidate `17464fd4c8fa13ed0b3123ae26b28effa01fee57` passed run `35965422146` with Android 16 instrumentation `OK (15 tests)` and was squash-merged as `634623f03954bcb1e1d53107e5187b7c724b40c4`.

Verified on `main` through PR #16 and PR #19:
- Arabic translations for the complete current 51-string UI surface, English/Arabic locale metadata, and Android RTL/resource instrumentation (`OK (16 tests)` on PR #16 exact candidate).
- Automated keyboard-focus evidence verifying Cancel → Tab → Save focus movement and Enter activation (`OK (17 tests)` on PR #19 exact candidate and exact merged-main run `35968638222`).

Still open within M1:
- Curated per-tracker icon and approved accent selection. This remains blocked on an approved Since-specific GLAZE tracker icon/accent key mapping; no product key catalog was invented. The canonical Since application launcher identity is implemented separately through GoreeCloud Branding Assets.
- Preferences DataStore is implemented on `main` only for the System/Light/Dark theme preference integrated by PR #39; additional preferences remain gated on real implemented behavior.
- Representative TalkBack and large-font visual acceptance.
- Automated keyboard-focus traversal/activation evidence is verified on `main` through PR #19; representative physical-keyboard/external-switch acceptance remains open.
- Arabic localization for the current 51-string UI surface plus packaged RTL/resource verification is verified on `main` through PR #16; native-language translation review, future-feature localization, Arabic TalkBack, and representative-device RTL acceptance remain open.
- Automated Android 16 light/dark rendered evidence and human review now exist for the principal Since flow; representative physical-device/OEM visual acceptance remains required before Stable qualification.

### M2 — Streak reset, history, and goals

**State:** In progress — goal editing/progress is integrated on `main`; reset/history/statistics remain open.

- Atomic close-current/create-next streak reset at one reset instant.
- Preserved read-only history and reset reason/note.
- Derived longest streak and reset count.
- Goal editor, current-period progress, and estimated completion: integrated by PR #32 as the bounded first M2 slice; reset/history/statistics remain open.

### M3 — Local management, portability, and recovery

**State:** Planned.

- Search, sorting, archive/restore, and delete safeguards remain planned. PR #39 integrated the bounded top-level Settings/About baseline (theme, fail-closed Backup/Restore entries, privacy/security information, app version/build status) while validated portability/recovery behavior remains open.
- Versioned JSON export using Android Storage Access Framework.
- Fail-closed validated import with review before mutation; prefer Replace import until safe merge semantics are fully designed.
- Room migration tests using committed historical schema files.
- Tested process-death/reboot behavior and recovery evidence sufficient for the implemented scope.

### M4 — Post-MVP Android experience

**State:** Planned / post-MVP.

- Privacy-safe home-screen widgets.
- User-selected milestone notifications with battery-conscious scheduling.
- Optional text sharing and refined adaptive layouts.

### M5 — GoreeCloud platform integration

**State:** Blocked pending implementation and accepted product-local evidence.

Evaluate and integrate all nine Integral Platform Systems as applicable: GoreeCloud Manager, Privacy Shield, Wardveil Security, Everkeep, Glaze UI, GoreeCloud Mesh, GoreeCloud Identity, GoreeCloud Policy, and GoreeCloud Observability.

The current dedicated Since Compose theme is an application-local presentation mapping informed by the Stable GLAZE UI V1.6 direction, with bounded Android-emulator rendered evidence. It is **not** by itself downstream GLAZE UI consumer conformance. The applicable approved Android mapping, human/device evidence, and repository-local conformance requirements must still be verified before any conformance claim.

### M6 — Release acceptance

**State:** Planned.

- Accessibility and rendered visual acceptance.
- Representative-device functional, performance, battery, lifecycle, and recovery evidence.
- Dependency/security review, signing, artifact provenance, rollback, complete applicable platform-system assessment, Release Candidate gates, production readiness, and Stable qualification.


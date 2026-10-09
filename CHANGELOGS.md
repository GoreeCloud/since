# GoreeCloud Since — Changelog

## 2026-10-08 — Independent repository migration complete (Development)

Imported original source history and prepared a standalone Android build/CI. No release lifecycle promotion is claimed.

Protected standalone `main` integrated PRs #5–#8 for import review, bounded export, transactional snapshots, and batched export reads. Their exact-head required CI and post-merge Android 16 checks passed (latest: workflow 37863968428, commit `5d3019d`). Restore/recovery, signer continuity, physical-device acceptance, and platform qualification remain open.

## 2026-09-24 — Since dark-mode system-bar contrast correction integration

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #43

### Fixed

- Synchronize Android status-bar and navigation-bar icon appearance with the resolved Since System/Light/Dark theme instead of leaving icon contrast tied to the device appearance selected when edge-to-edge was enabled.
- Keep the system-bar surfaces aligned with the active Since background while preserving edge-to-edge layout.
- Move system-bar appearance handling into production `SinceTheme` so in-app theme changes update the operating-system chrome immediately.
- Remove the visual-test-only system-bar override. Rendered evidence now exercises the production system-bar behavior rather than compensating for it inside the test harness.

### Reason

User-provided runtime screenshots of the Development build showed black status-bar time and status icons against the near-black Since Dark background. Source review confirmed the production theme did not update system-bar icon appearance when the app-selected theme differed from the device theme, while the visual-evidence test applied its own correct override and therefore masked the defect.

### Verification and integration

Exact candidate `13480c0c64c4611edb0f2d84ef91263bd22b3831` passed Android Development Foundation run `36077943497` / #159. PR #43 then merged to `main` as `60031a0632ae169c22c8a2a52eed6b5678646f0d`. Exact merged-main Android Development Foundation run `36079029190` / #160 passed and produced the recorded runtime and rendered-evidence artifacts. Human readback of the merged-main dark Dashboard evidence confirmed light, readable status-bar time/status icons against the dark Since surface.

### Boundary

This correction does not change tracker data, Room schema, network/privacy permissions, recovery behavior, Release Candidate, production, or Stable status. Representative physical-device/OEM system-bar behavior, navigation-mode acceptance, assistive-technology acceptance, downstream Glaze consumer acceptance, release, and Stable qualification remain separate gates.


## 2026-09-24 — Since Home, Achievements, and Settings navigation integration

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #39

### Added

- Persistent phone bottom navigation for Home, Achievements, and Settings.
- Local-only deterministic achievement presentation for first tracker, first streak, first goal, seven-day streak, and thirty-day streak, using the existing calendar-aware Since time model.
- Persisted System/Light/Dark theme preference.
- Settings sections for appearance, data/recovery, privacy/security, and About/application version.
- Visible Backup and Restore entry points that remain explicitly Planned and fail closed until validated recovery exists.
- English and Arabic strings plus Compose top-level navigation/settings coverage.
- Light/dark rendered evidence for Achievements and Settings, including recovery and About/privacy/security views.

### Verification

Final exact candidate `d11a3aabfe9aac3f8fbdae05558e026b87c50156` passed Android Development Foundation run `36072370515` / #152, including build/JVM/lint/schema/manifest checks, Android 16 instrumentation `OK (21 tests)`, and rendered-evidence upload. PR #39 was squash-merged as `43cdfc8659de25e0d8829fed593d7f0a914e5965`. Exact merged-main run `36073050400` / #153 also passed with Android 16 instrumentation `OK (21 tests)`, producing `since-runtime-apks` artifact `10838389459` (`sha256:bf1b34d54c453d24d57a28c2b2d6ec12577fa043e2ddb621d21927913b1027cb`) and 20-scene `since-rendered-ui` artifact `10838444438` (`sha256:51275298f6efed8246718ee67f9fac35a8b379195c02a32275b9cd1ed55443df`). Exact-main Settings, Data & recovery, About dark, and Achievements dark scenes were visually reviewed for application content, selected navigation state, clipping, and Planned Backup/Restore treatment. Later runtime feedback exposed a separate production system-bar contrast defect when the app-selected Dark theme differs from the device theme; the rendered-evidence test had applied a test-only system-bar override, so those screenshots were not valid proof of production system-bar icon contrast.

### Boundary

Backup and Restore are not implemented recovery operations. This integration does not add network access, advertising, analytics, a cloud dependency, or new dangerous permissions. GitHub records show no submitted review on PR #39 before merge, so independent-review process acceptance remains open. Representative-device, assistive-technology, downstream GLAZE UI consumer, Release Candidate, production, and Stable acceptance also remain open.

This repository-local record is the authoritative human-readable change history for `GoreeCloud/android-app-defaults`.


## 2026-09-24 — Since searchable time-zone picker integration

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #37

### Changed

- Replaced manual IANA time-zone typing in Create/Edit Tracker with a full-width selectable Time zone control.
- Added a searchable picker across available IANA zones, matching city/region identifiers and UTC offsets.
- Shows each zone's UTC offset for the selected local start time and provides a one-tap device-time-zone option.
- Preserved the existing local date/time + explicit ZoneId storage contract, deterministic DST handling, `Use now`, persistence semantics, and closed-history protections.
- Added English/Arabic picker resources, Date/Time/Zone Compose interaction coverage, and light/dark rendered evidence for the new picker surface.

### Verification

PR #37 was squash-merged to `main` as `700d6084f5f9268727a17ad593fab5412feab801` after exact-head Android Development Foundation run `36063174272` / #132 passed on candidate `ef3e3fa84aa7d3975c8cb087c423f88e1d158d52`, including build, JVM tests, Android lint, Room schema verification, manifest/privacy guard, APK assembly, Android 16 runtime instrumentation, and rendered UI evidence.

Post-merge Android Development Foundation run `36063794046` / #133 then passed on the merged `main` commit, including the same build/runtime gates and rendered visual evidence.

### Boundary

This does not change the Room schema, chronology rules, network/privacy permissions, representative-device acceptance, Release Candidate, production, or Stable status.


## 2026-09-24 — Since deterministic visual evidence follow-up

**Lifecycle:** Development  
**Tracking:** PR #35; follow-up to PR #34 post-merge validation

### Changed

- Reworked Since rendered visual evidence so light/dark screenshots switch the Compose theme in-place instead of recreating the instrumentation host Activity.
- Moved the reusable fake tracker repository into shared Android-test support so accessibility and visual tests exercise the same deterministic repository contract.
- Added an explicit optional theme selector to `SinceTheme` for deterministic test rendering while preserving system-theme behavior by default.

### Reason

PR #34 merged successfully after exact-head validation, but its first post-merge Android Development Foundation run exposed an intermittent visual-evidence timeout during Android night-mode Activity recreation. The functional picker and repository tests were not the failing gate; the failure was isolated to rendered-evidence theme switching.

### Verification

PR #35 was squash-merged to `main` as `0996b3294e54a855e354cba70b6d95e9b48fc695`. Android Development Foundation run `36054602137` then passed on that merged `main` commit, including build/unit/lint/schema/manifest checks, Android 16 runtime instrumentation, and rendered visual evidence.

### Boundary

This follow-up does not change production data, chronology, network/privacy permissions, persisted schema, release classification, Production acceptance, or Stable status.


## 2026-09-24 — Since date and time picker integration

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #34

### Changed

- Replaced manual `YYYY-MM-DD HH:MM` editing in Create/Edit Tracker with separate Date and Time picker controls.
- Preserved the stored local start plus explicit IANA time-zone contract, including existing DST validation and persistence behavior.
- Time selection follows the device 12/24-hour preference.
- Added English and Arabic picker labels and Compose coverage for picker reachability.

### Verification

PR #34 was squash-merged to `main` as `287987eac4d11fe837d4b201f58f5b31fa38cccf` after exact-head Android Development Foundation validation. Its first post-merge run exposed only an intermittent rendered-evidence night-mode recreation timeout; PR #35 isolated and corrected that evidence path without changing tracker behavior. The resulting merged `main` commit `0996b3294e54a855e354cba70b6d95e9b48fc695` passed Android Development Foundation run `36054602137`, including Android 16 instrumentation and rendered UI evidence.

### Boundary

This does not change the Room schema, chronology rules, network/privacy permissions, representative-device acceptance, Release Candidate, production, or Stable status.


## 2026-09-24 — Since M2 goal progress and editor candidate

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #32

### Added

- Calendar-aware Days/Weeks/Months/Years goal target estimation from the current period start and stored IANA zone.
- Current-period goal percent, progress indicator, >=100% completion state, and estimated completion.
- Streak-only goal add/edit/remove persistence without a Room schema change.
- Preservation of goal creation timestamp across edits and period-history preservation on goal removal.
- English/Arabic goal editor and progress resources.
- JVM, Android runtime, and Compose UI coverage for the new goal behavior.

### Verification

PR #32 carries this Development slice. Exact-head validation for the final merge candidate is authoritative in the GitHub pull-request/workflow record; this changelog does not pin a moving pre-merge head. Any candidate-head change requires a fresh exact-head run before merge.

### Boundary

Atomic reset, History, longest-streak/reset-count statistics, representative-device acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable remain open.


## 2026-09-23 — GoreeCloud Since Development foundation

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #2

### Added

- Android Gradle Kotlin DSL monorepo foundation with the first independent module at `apps/since`.
- GoreeCloud Since Android application shell using product application ID `com.goreecloud.since` and Development suffix `.dev`.
- Local-only manifest boundary with no `INTERNET` or location permission.
- Automatic Android backup disabled pending approved Since recovery integration; cleartext traffic disabled.
- Core tracker/period/goal domain representations and calendar-aware `TimeEngine`.
- Tracker-draft validation and focused JVM regressions.
- Initial Compose Dashboard empty state.
- Baseline Android CI and repository-native feature/changelog records.

### Verification and integration

Exact candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`. PR #2 was guarded-squash merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`, followed by merged-source readback.


## 2026-09-23 — Since Room persistence foundation

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #3

### Added

- Room 3.0.3 / SQLite 2.7.1 persistence baseline, schema export, committed compiler-generated v1 schema, and CI drift enforcement.
- Schema-v1 entities for `tracked_events`, `event_periods`, and `event_goals` with cascading foreign keys and unique per-event period sequence.
- App-owned SQLite one-open-period partial unique index and triggers for chronology, permanent-event single-period behavior, and streak-only positive goals.
- Transactional tracker aggregate creation and repository/domain mapping.
- Lazy application database/repository wiring.
- Android 16 runtime coverage for the critical database invariants and persistent close/reopen behavior.
- CI transport of the exact build-lane app/test APK pair into a KVM-accelerated Android 16 runtime lane.

### Verification and integration

Exact candidate `9a4268c08b5d442d858cebaf2d4e06a4bb552a34` passed Android Development Foundation run `35954179931`, including build/lint/unit/schema-drift validation and `SinceDatabaseRuntimeTest` with `OK (4 tests)`. PR #3 was squash-merged to `main` as `2de009f3c3b1288eef9374d702198438eeead731`, and merged-main readback verified the expected persistence/runtime source.

Historical failed workflow attempts remain part of the change history for the concrete defects they exposed; they are not reused as final-head evidence.


## 2026-09-23 — Since persistent creation, Dashboard, and Details

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #4

### Added

- Reactive aggregate observation across active trackers, periods, and goals.
- Tracker Type Chooser for Permanent Event and Streak.
- Create Tracker screen with validation before mutation, Start = Now/current IANA ZoneId, display format, and optional streak goal.
- Atomic repository-backed tracker/current-period/goal persistence.
- Populated Dashboard cards with lifecycle-aware minute-updated calendar-aware elapsed summaries.
- Tracker Details with selectable elapsed hero, persisted start date/time/zone, note, goal summary, and deliberate persisted display-format changes.
- Shared application clock across repository and UI calculation boundaries.
- Kotlin validated-draft copy-visibility hardening.
- Android runtime coverage for validated creation, aggregate reactivity, and display-format persistence.

### Verification and integration

Exact candidate `be534fc98e1fc7020296779f0e71cd4d48ae6778` passed Android Development Foundation run `35956066300`, including manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and KVM-accelerated Android 16 `SinceDatabaseRuntimeTest` with `OK (8 tests)`. PR #4 was squash-merged to `main` as `3cc9788513c100e0ca3fedb336dd57a1b3b87e84`, followed by merged-source readback.

### Boundary

Custom past-start editing, icon/accent selection, Edit, streak reset/history/statistics, full goal progress/editor, archive/search/settings, portability/recovery, GLAZE UI consumer acceptance, complete platform-system conformance, representative-device acceptance, Release Candidate, production, and Stable remain open.



## 2026-09-23 — Since custom start and Edit Tracker

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #6

### Added

- Strict local date/time plus explicit IANA ZoneId input for tracker starts.
- Explicit DST resolver: nonexistent gap times fail closed; ambiguous fall-back times use the earlier valid offset.
- Create Tracker support for user-selected past start/date/time and zone.
- Edit Tracker path from Details for title, note, current start/date/time/zone, and default display format.
- Transactional edit boundary that updates only tracker metadata and the one open current period.
- Closed-history protection preventing a current streak start from moving before the latest closed period end.
- JVM resolver regressions plus Android runtime edit/history integrity tests.

### Deferred

Icon/accent selection remains open. GLAZE UI V1.6.0 is the verified current Stable shared target, but no approved Since-specific icon/accent key catalog was verified. The candidate therefore does not invent application-specific GLAZE keys or claim downstream GLAZE consumer conformance.

### Verification and integration

Exact candidate `89775713a83726df94e9592bf81358d81772d62f` passed Android Development Foundation run `35959431091` on attempt 2, including the local-only manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 `SinceDatabaseRuntimeTest` with `OK (10 tests)`. Attempt 1 failed before tests because one hosted runner did not expose readable/writable `/dev/kvm`; the exact same source was rerun without weakening validation. PR #6 was then squash-merged to `main` as `48d7c8342ad17e860b521690df5c817d84d92b7b`, followed by merged-source readback.

### Boundary

Icon/accent selection, remaining M1 accessibility/preferences work, and M2–M6 remain open. This does not establish GLAZE UI consumer acceptance, representative-device acceptance, Release Candidate, production, or Stable status.



## 2026-09-24 — Since editor accessibility and UI evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #9

### Added

- Explicit accessibility heading semantics on primary Since screens.
- Merged clickable tracker-card semantics so visible title/type/elapsed/goal content is exposed as one coherent accessible unit.
- Full-row selectable display-format choices and a full-row streak-goal toggle, while suppressing redundant child radio/switch focus stops.
- Assertive live-region semantics for validation, start-input, save, and display-format update errors.
- Compose UI instrumentation coverage for headings, tracker-card click semantics, selectable display-format rows, scroll-reachable Save action, assertive validation errors, and custom-past-start Create → Details → Edit persistence.
- One shared lifecycle-aware Dashboard minute ticker replacing per-card ticker coroutines.
- Compose UI test dependencies scoped to Android instrumentation/debug builds.
- Android 16 runtime CI acceleration detection: KVM is used when available; otherwise the required instrumentation suite runs with software acceleration instead of being skipped.

### Verification and integration

Exact candidate `b0ccc4e1f00c089ad8257f50ffb167df4b9e7af6` passed Android Development Foundation run `35963071718`, including the local-only manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (13 tests)`. The runtime job now keeps instrumentation mandatory while selecting KVM when available or software acceleration otherwise. PR #9 was squash-merged to `main` as `5b14c58af68b8748e71f6af0c462650d06e852d6`; draft PR #8 was closed unmerged after its non-overlapping useful work was consolidated.

### Boundary

Representative-device TalkBack/large-font/keyboard/switch-access, localization/RTL, approved Since-specific icon/accent mapping, downstream GLAZE UI consumer acceptance, and all later M2–M6 work remain open.



## 2026-09-24 — Since large-font and RTL automated evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #12

### Added

- Compose instrumentation at 2× font scale verifying primary Create-editor fields and Save remain scroll-reachable.
- Forced RTL layout-direction instrumentation verifying the Create heading, title, zone field, and Save action remain reachable.
- Coverage remains in the mandatory Android 16 instrumentation lane.

### Verification and integration

Exact candidate `17464fd4c8fa13ed0b3123ae26b28effa01fee57` passed Android Development Foundation run `35965422146`, including manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (15 tests)`. PR #12 was squash-merged to `main` as `634623f03954bcb1e1d53107e5187b7c724b40c4`.

### Boundary

This is CI-level Development evidence only. It does not establish representative-device TalkBack, visual-regression, keyboard/switch-access, translation/localization, downstream GLAZE consumer acceptance, Release Candidate, production, or Stable status.



## 2026-09-24 — Since Arabic localization

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #16

### Added

- Arabic translations for all current Since Android string resources, with the product name **Since** preserved as the product identity.
- Android locale metadata declaring English and Arabic.
- Android instrumentation that verifies an Arabic configuration resolves RTL layout direction and representative localized resources.

### Verification and integration

Exact candidate `3867afe88e6570f647b65dfa224c5b67708d998c` passed Android Development Foundation run `35967471510` with Android 16 instrumentation `OK (16 tests)`. PR #16 was squash-merged to `main` as `d204cbd4464539a15b8f1ed6e0d9e9dfef4ddbb7`.

### Boundary

Native-language translation review, representative-device RTL visual acceptance, Arabic TalkBack acceptance, physical keyboard/external-switch acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable remain open.



## 2026-09-24 — Since keyboard-focus automated evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #19

### Added

- Compose keyboard input mode instrumentation for the Create editor.
- Focus assertion on Cancel.
- Tab traversal verification from Cancel to Save.
- Enter activation verification on focused Save with the existing validation error as the observable result.

### Verification and integration

Exact candidate `bdc89ec81fae8a195e7f611efa4ca0f81c614650` passed Android Development Foundation run `35968237474` with Android 16 instrumentation `OK (17 tests)`. PR #19 was squash-merged to `main` as `a24942ce244b2182d72aadde0f57eb10bebc8f5c`. Exact merged-main run `35968638222` also passed `OK (17 tests)` and produced unexpired Development artifact `since-runtime-apks` ID `10794409287`.

### Boundary

This is automated emulator evidence only. Representative physical-keyboard/external-switch acceptance, representative-device TalkBack or large-font visual acceptance, native-language Arabic review, representative-device RTL visual acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable remain open.


## 2026-09-24 — Since visual interface refinement

**Lifecycle:** Development  
**Tracking:** GitHub PR #21

### Improved

- Replaced the temporary stock Material 3 visual substrate with a dedicated GoreeCloud Since light/dark color and shape theme informed by the current Stable Glaze UI V1.6 direction.
- Reworked the Dashboard empty state to remove the large dead-space presentation and establish a clearer first-run hierarchy.
- Redesigned tracker cards with calmer layered surfaces, tracker-kind treatment, stronger elapsed-time hierarchy, and compact goal presentation.
- Reorganized Tracker Details around a primary elapsed-time hero that keeps the persisted start date/time and IANA zone visible with the tracker state.
- Replaced long vertical display-format radio lists with accessible two-column selection surfaces.
- Reworked the Tracker Type Chooser and Create/Edit screens into larger, grouped, touch-friendly surfaces with clearer information hierarchy.
- Removed implementation/development deferral notes from ordinary user-facing screens while preserving the Development boundaries in repository records.
- Enabled Android edge-to-edge system-bar handling so status/navigation icon appearance follows the light/dark environment.

### Verification

Exact visual implementation candidate `41a61992a76a8c7475c98666d8db5993913b0c50` passed Android Development Foundation run `35973932878` / run #67: manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 instrumentation with all 17 tests passing.

### Boundary

This is a Development visual refinement. It does not establish representative physical-device visual acceptance, complete downstream Glaze UI consumer conformance, Release Candidate, production, or Stable status. Tracker persistence, time calculation, validation, and schema behavior are unchanged.


## 2026-09-24 — Since rendered visual evidence and surface hierarchy

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #22

### Added

- Android 16 full-device screenshot capture for the empty Dashboard, tracker-type chooser, Create Streak, Tracker Details, and populated Dashboard.
- CI extraction and publication of the rendered PNGs as the `since-rendered-ui` artifact.

### Improved

- Defined the complete light/dark Material 3 surface-container hierarchy used by Since so cards, dialogs, and editor sections no longer inherit unrelated default pink/purple surface tones.
- Kept the deep-teal application identity while moving durable content surfaces toward a calm neutral Glaze-compatible hierarchy.

### Verification

Exact source-bearing candidate `7c77ab28811b4aedecc733e0479dd5b57edda0d4` passed Android Development Foundation run `35976727631` / #73. The build lane passed the local-only manifest guard, JVM tests, Android lint, APK assembly, and Room-schema drift verification. Android 16 runtime instrumentation passed and uploaded rendered artifact `since-rendered-ui` ID `10798890114`, digest `sha256:c6f865e01296ed28da2a7c2059e60bf4af367e6220dacce88b570df526928cc5`.

### Boundary

Rendered emulator evidence is not human visual acceptance or representative physical-device/OEM acceptance. Downstream GLAZE UI V1.6 consumer acceptance, assistive-technology acceptance, Release Candidate, production, and Stable remain open.


## 2026-09-24 — Since canonical application identity

**Lifecycle:** Development  
**Tracking:** GitHub issue #1

### Added

- Canonical GoreeCloud Since application identity provenance through `apps/since/BRANDING.md`.
- Android adaptive launcher icon packaging for normal and round presentation.
- Android 13+ monochrome/themed launcher-icon support.
- Manifest/build guard requirements that prevent the Since APK from silently reverting to a generic or placeholder launcher identity.

### Canonical source

The authoritative source is `GoreeCloud/branding-assets/products/since/app-icon.svg`, exact Git blob `a107f860759e745ff16f2b5bf1954b93fbb17937`, integrated to canonical branding main as `8c27acbdf7c83624a7396fdb4d7589f0643b4cc0` through branding PR #25. Post-merge branding validation run #93 / `35979255165` passed.

### Visual review

The canonical source was rendered and reviewed at 512 px, 128 px, 48 px, 32 px, and in grayscale before publication. The elapsed-ring/start-node geometry remained recognizable without clipping, distortion, text dependence, or color-only differentiation.

### Boundary

This change establishes and packages the application launcher identity. Per-tracker icon/accent selection remains a separate Development feature. Consumer APK/runtime verification remains governed by the exact android-app-defaults candidate and CI evidence.


## 2026-09-24 — Since visual hierarchy refinement

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #24

### Improved

- Increased primary-action emphasis for **Add tracker** on the Dashboard.
- Separated the **Elapsed** label from the value and reduced value scale on populated tracker cards.
- Replaced the large accent-filled Details hero with a neutral durable surface and focused teal state/value emphasis.
- Made Permanent Event and Streak chooser options visually neutral and equivalent so the UI does not imply that either type is preselected or recommended.
- Applied consistent rounded Glaze geometry to Create/Edit text fields.

### Verification and integration

Exact candidate `ee3d69886644840c00aff2df9b5f72962ed6109b` passed Android Development Foundation run `35981477112` / #81 with Android 16 instrumentation `OK (18 tests)` and exact-head rendered visual evidence. Human review of all five rendered scenes found no clipping, text corruption, unintended pink/purple Material inheritance, broken field geometry, or loss of content hierarchy.

PR #24 was squash-merged as `7862a4de6ecebbebbd5466f82d8a2867e7be49e8`. Exact merged-main run `35982116803` / #82 passed Android 16 instrumentation `OK (18 tests)`, producing `since-runtime-apks` artifact ID `10800732670` with digest `sha256:7abf1a90e5881a9804d213effb45f68256c5d23ee585559395949f6670d2c0b2` and `since-rendered-ui` artifact ID `10800344407` with digest `sha256:be77a211a844a1d84b2d3028721e85009f5694e58e957aa380fea752e3f4777d`.

### Boundary

This is Development presentation refinement and emulator evidence, not representative physical-device/OEM, dark-mode, assistive-technology, complete downstream GLAZE UI consumer acceptance, Release Candidate, production, or Stable acceptance.




## 2026-09-24 — Since dual-theme rendered evidence

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #27

### Added

- Android 16 dark-mode rendered evidence for the empty Dashboard, tracker-type chooser, Create Streak, Tracker Details, and populated Dashboard.
- Stable tracker-type test tags so evidence automation targets explicit chooser actions rather than ambiguous text.
- CI extraction now requires all ten light/dark principal-flow PNG scenes.

### Verification and integration

Exact PR #27 final head `2b387c57fbb603857bfbe5dc70c909acea483377` passed Android Development Foundation run `35986151222` / #91 with Android 16 instrumentation `OK (18 tests)` and rendered-evidence upload. The PR was squash-merged as `eb8975dca84d2db613b05af07f7b4b9d3f1ed027`.

Exact merged-main run `35986755499` / #92 also passed with Android 16 instrumentation `OK (18 tests)`, producing runtime artifact `10802144523` with digest `sha256:6d2271641128def6c8d846b00bf0295ca34ae77fc9321975120caf91a012a034` and rendered artifact `10802244368` with digest `sha256:1e4ecc14b94128dc1849311a1decb52b7ccdcf0c240994d8aae279822f51e866`.

Human review of all ten exact-main scenes found no clipping, text corruption, unintended pink/purple Material inheritance, or unreadable dark-mode contrast.

### Boundary

This closes the bounded Android-emulator dark-mode rendered-evidence gap. Representative physical-device/OEM, native-language, assistive-technology, downstream GLAZE UI consumer, Release Candidate, production, and Stable acceptance remain open.


## 2026-09-24 — Since empty Dashboard first-run refinement

**Lifecycle:** Development  
**Tracking:** GitHub issue #1, PR #30

### Improved

- Replaced the empty Dashboard's detached floating `Add tracker` action with a full-width primary action inside the first-run surface.
- Added the canonical Since launcher mark to the empty-state card.
- Promoted `No trackers yet` from status-chip treatment to a true empty-state heading.
- Rebalanced supporting copy and removed unused floating-action bottom clearance from the empty state.
- Preserved the floating `Add tracker` action for populated dashboards.

### Verification and integration

Exact PR #30 candidate `9b23193e733a350866511e29596b9339d1999823` passed Android Development Foundation run `35989103579` / #97 with Android 16 instrumentation `OK (18 tests)` and all ten light/dark rendered scenes. Visual review of the empty Dashboard evidence confirmed the intended branded hierarchy and found no clipping, text corruption, or unreadable dark-mode contrast.

PR #30 was squash-merged as `979972f2564b6ebd63ed59aeb658cbf4cd261eb1`. Exact merged-main run `35989719467` / #98 also passed with Android 16 instrumentation `OK (18 tests)`, producing runtime artifact `10803354250` with digest `sha256:180b24d5661191c8e60a33b31946ab2a1050a5f514350399ef08b09796330a73` and rendered artifact `10803653756` with digest `sha256:d316677947be78aace4d6462b55e4e1e31963e9d4136e88a265b6ca0a2e99dc3`.

### Boundary

This is a presentation-only Development refinement. Tracker semantics, persistence, permissions, networking, time calculations, and lifecycle state are unchanged. Representative physical-device/OEM, assistive-technology, downstream GLAZE UI consumer, Release Candidate, production, and Stable acceptance remain open.




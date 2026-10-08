# GoreeCloud Since — Implemented Development Features

Historical evidence migrated from exact monorepo source 9097c8cb7adecc3c969f162fc33a9e8fdde20cc4. Standalone acceptance is not implied.

## Verified Development baseline on `main`

PR #2 was squash-merged to `main` as `8afd4eecc1a374443f7a7b7da72eb19e79dfc4e1`.

The exact pre-merge candidate `a945cee18ef0924d7ef5360e5f252231c446753d` passed Android Development Foundation run #4 / `35947248482`, including the local-only manifest guard, JVM tests, Android lint, and Debug assembly.

PR #3 was squash-merged to `main` as `2de009f3c3b1288eef9374d702198438eeead731`. Its exact candidate `9a4268c08b5d442d858cebaf2d4e06a4bb552a34` passed Android Development Foundation run `35954179931`, including build/lint/unit/schema-drift checks and KVM-accelerated Android 16 execution of `SinceDatabaseRuntimeTest` with `OK (4 tests)`. Merged-main readback verified the Room source, committed schema, fail-closed manifest, and runtime workflow.

PR #4 was squash-merged to `main` as `3cc9788513c100e0ca3fedb336dd57a1b3b87e84`. Its exact candidate `be534fc98e1fc7020296779f0e71cd4d48ae6778` passed Android Development Foundation run `35956066300`, including manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and KVM-accelerated Android 16 execution of `SinceDatabaseRuntimeTest` with `OK (8 tests)`. Merged-main readback verified the persistent create/Dashboard/Details source landed.

This is Development evidence only. It does not establish representative-device acceptance, GLAZE UI consumer conformance, complete Integral Platform System acceptance, Release Candidate, production, or Stable qualification.

## Repository Development foundation

- Gradle Kotlin DSL Android monorepo skeleton with `:apps:since` as the first independent application module.
- Java 17 / Kotlin / Jetpack Compose Android baseline using compile/target SDK 36 and minimum SDK 29.
- Pull-request/main Android CI covering manifest policy, JVM tests, Android lint, application/instrumentation assembly, Room-schema drift, and Android 16 runtime invariants.
- Required repository-native feature-state and changelog records.

## GoreeCloud Since — implemented Development source

### Independent Android application shell and privacy boundary

- Application identity `com.goreecloud.since`, with side-by-side Development package `com.goreecloud.since.dev`.
- User-visible installed application label **Since**.
- No manifest `INTERNET`, location, advertising, analytics, account, or background-service permission.
- Automatic Android backup is disabled pending an approved Since Everkeep/system-backup policy.
- Cleartext traffic is disabled.
- Single-activity Jetpack Compose foundation.

### Core tracker/time domain

- Permanent Event and Streak tracker kinds.
- Days, Weeks, Months, and Years display-format identities.
- Domain representations for trackers, periods, optional goals, and aggregate invariants.
- Tracker-draft validation for normalized titles/notes, IANA ZoneId validation, future-start rejection with a bounded race tolerance, streak-only goals, and bounded goal amounts.
- `@ConsistentCopyVisibility` hardening for the validated-draft constructor/copy boundary.
- Injected `Clock` and calendar-aware `TimeEngine`; elapsed values are derived from persisted instants rather than stored counters.
- JVM regression coverage for daylight-saving, month-end, leap-day, week/day/hour, validation, and reversed-clock behavior.

### Verified Room persistence foundation

- Room 3.0.3 database `since.db`, schema version 1, with compiler-generated schema committed for migration review and CI drift detection.
- `tracked_events`, `event_periods`, and `event_goals` with cascading foreign keys and unique per-event period sequence.
- App-owned SQLite setup for the one-open-period partial unique index that Room's current `Index` annotation cannot express.
- Database triggers rejecting invalid period chronology, additional periods for permanent events, and goals attached to non-streak trackers.
- Transactional aggregate creation of tracker + initial open period + optional goal with stable manual sort ordering.
- Repository mapping that keeps Room entities out of the domain/UI contract.
- Lazy application-level database/repository wiring.
- Android runtime coverage for critical invariants and persistent close/reopen behavior.

### Verified persistent create, Dashboard, and Details flow

Current `main` additionally implements:

- Reactive active-tracker aggregate observation across tracker, period, and goal tables.
- Tracker Type Chooser for Permanent Event vs Streak.
- Create Tracker UI for normalized title/note, Start = Now/current IANA ZoneId, display format, and optional streak goal.
- Validation before mutation and transactional persistent save.
- Populated Dashboard cards with locally derived elapsed summaries and optional goal summaries.
- Tracker-card navigation into persisted Tracker Details.
- Details hero elapsed value, stored start date/time/zone, note, optional goal summary, and deliberate persisted Days/Weeks/Months/Years display-format selection.
- Lifecycle-aware minute-aligned ticker because visible elapsed summaries include minutes; updates stop when the screen lifecycle is inactive, and no persistent timer or background ticker is introduced.
- Shared application clock across repository and UI time derivation.
- Fail-closed clock inconsistency, save failure, and display-format update states.
- Android runtime coverage for validated repository creation, aggregate reactivity, and persisted display-format changes.


### Verified M2 goal progress and editor

PR #32 integrated the bounded first M2 slice without changing the Room v1 schema or the local-only permission boundary:

- calendar-aware goal target estimation for Days, Weeks, Months, and Years using the persisted current-period start and IANA zone;
- current goal percent/progress display that may truthfully exceed 100% while elapsed tracking continues;
- localized estimated completion in the current period zone;
- streak-only goal add/edit/remove persistence with amount range 1–100,000;
- preserved goal creation timestamp across edits and goal removal that does not alter tracker period history;
- fail-closed clock inconsistency and untrusted/invalid amount handling;
- English and Arabic goal-progress/editor resources;
- JVM coverage for DST, month-end, leap-day, completion, >100%, reset/new-period progress, and backward-clock behavior;
- Android runtime persistence/reactivity coverage plus Compose goal editor update/remove coverage.

PR #32 was merged to `main` as `d6ad738ebdd559f8275a85ff2e8c4952c339f2a8`; PR #33 then stabilized the rendered-evidence synchronization path and merged as `a9d1101e964eb16b0718c4a3fc857f33dcc1ae9c`. The goal progress/editor functionality is current Development source.

This remains Development evidence. Atomic streak reset, preserved History, longest-streak/reset-count statistics, representative-device acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable qualification remain open.


## Material limitations

Icon/accent selection, streak reset/history/statistics, archive/search, export/import, validated backup/restore implementation, widgets, milestone notifications, representative accessibility/visual acceptance, approved GLAZE UI consumer mapping, and accepted Integral Platform System integrations remain open in `PLANNED-FEATURES.md`. Custom past-start selection, Edit Tracker, and the integrated PR #32 goal progress/editor functionality are not represented as open limitations.


### Integrated date and time picker flow

Current `main` replaces manual local date/time typing in the Create and Edit Tracker surfaces with separate Material 3 Date and Time picker controls while preserving the existing local-time plus explicit IANA ZoneId storage and validation contract. The time picker respects the device 12/24-hour preference; English and Arabic labels and Compose picker-reachability coverage are included.

PR #34 was squash-merged as `287987eac4d11fe837d4b201f58f5b31fa38cccf`. PR #35 then stabilized deterministic light/dark rendered evidence and was squash-merged as `0996b3294e54a855e354cba70b6d95e9b48fc695`. Android Development Foundation run `36054602137` passed on that merged `main` commit, including build/unit/lint/schema/manifest checks, Android 16 runtime instrumentation, and rendered UI evidence.

This remains Development evidence only; it does not establish a release, deployment, Production acceptance, or Stable status.

### Integrated searchable time-zone picker

Current `main` removes the remaining manual IANA time-zone entry from Create and Edit Tracker. The stored ZoneId is presented through a full-width picker that supports search by IANA city/region identifier and UTC offset, shows the offset applicable to the selected local start time, and offers the device time zone as a one-tap choice. The underlying validated local date/time + explicit ZoneId model and DST rules remain unchanged. English/Arabic resources, deterministic Compose interaction coverage, and light/dark rendered picker evidence are included.

PR #37 was squash-merged as `700d6084f5f9268727a17ad593fab5412feab801`. Exact candidate `ef3e3fa84aa7d3975c8cb087c423f88e1d158d52` passed Android Development Foundation run `36063174272` / #132, and merged `main` then passed run `36063794046` / #133, including build/JVM/lint/schema/manifest checks, APK assembly, Android 16 runtime instrumentation, and rendered UI evidence.

This remains Development evidence only; it does not establish a release, deployment, Production acceptance, or Stable status.


### Verified top-level navigation, Achievements, and Settings integration

PR #39 integrated the owner-requested top-level Android surfaces without changing the Room v1 schema or local-only permission boundary:

- persistent phone bottom navigation for **Home**, **Achievements**, and **Settings**;
- local-only deterministic achievements for first tracker, first streak, first goal, seven-day streak, and thirty-day streak, using the existing calendar-aware Since time semantics;
- a persisted Preferences DataStore theme preference with **System**, **Light**, and **Dark** choices;
- visible **Backup** and **Restore** settings that remain explicitly Planned/fail-closed until a validated recovery format, integrity checks, and restore acceptance exist;
- **Privacy**, **Security**, **App version**, and Development build-status information;
- English and Arabic resources for the new navigation, achievement, and settings surfaces;
- Compose coverage for top-level reachability, theme selection, fail-closed Backup/Restore dialogs, privacy/security visibility, and app-version visibility;
- light/dark Android-rendered evidence for Achievements and Settings, including recovery and About/privacy/security views.

Final exact candidate `d11a3aabfe9aac3f8fbdae05558e026b87c50156` passed Android Development Foundation run `36072370515` / #152 with Android 16 instrumentation `OK (21 tests)` and rendered-evidence upload. PR #39 was then squash-merged as `43cdfc8659de25e0d8829fed593d7f0a914e5965`; exact merged-main run `36073050400` / #153 also passed with Android 16 instrumentation `OK (21 tests)`. The exact-main run produced `since-runtime-apks` artifact `10838389459` (`sha256:bf1b34d54c453d24d57a28c2b2d6ec12577fa043e2ddb621d21927913b1027cb`) and 20-scene `since-rendered-ui` artifact `10838444438` (`sha256:51275298f6efed8246718ee67f9fac35a8b379195c02a32275b9cd1ed55443df`). GitHub records show no submitted review on PR #39 before merge, so independent-review process acceptance remains open even though the source is integrated and CI-verified.

This remains Development evidence. Backup/Restore implementation, representative-device and assistive-technology acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable qualification remain open.

### Verified custom-start and Edit Tracker flow

Current `main` additionally implements:

- picker-based local start date/time selection backed by the existing validated local date/time representation plus an explicit IANA ZoneId;
- deterministic DST handling that rejects nonexistent gap times and resolves fall-back overlaps to the earlier valid offset;
- Create Tracker support for user-selected past start date/time and zone;
- Edit Tracker from Details for title, note, current open-period start/date/time/zone, and default display format;
- atomic persistence updates that never rewrite closed streak history and reject moving the open current period before the latest closed-period end;
- JVM coverage for ordinary, gap, overlap, malformed date/time, and invalid-zone resolution;
- Android runtime coverage for successful current-period edits and closed-history conflict rejection.

PR #6 was squash-merged to `main` as `48d7c8342ad17e860b521690df5c817d84d92b7b`. Exact candidate `89775713a83726df94e9592bf81358d81772d62f` passed Android Development Foundation run `35959431091` on attempt 2, including the local-only manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 `SinceDatabaseRuntimeTest` with `OK (10 tests)`. Attempt 1 failed before tests because that hosted runner did not expose readable/writable `/dev/kvm`; no source change or validation weakening was used before the successful exact-head rerun.

Icon/accent selection remains unimplemented because the current GLAZE UI authority exposes shared icon/color contracts but no approved Since-specific icon/accent key catalog was verified.


### Verified editor accessibility and UI evidence

Current `main` additionally implements:

- explicit heading semantics for Dashboard, Create, Edit, and Tracker Details headings;
- coherent merged tracker-card semantics so title, tracker type, elapsed summary, and optional goal content present through one clickable accessibility unit;
- full-row selectable display-format options with the visual radio control removed as a redundant accessibility focus stop;
- full-row toggleable streak-goal activation with the visual switch removed as a redundant accessibility focus stop;
- assertive live-region semantics for validation, start-input, save, and display-format failure messages;
- Android Compose instrumentation coverage for heading semantics, coherent clickable tracker cards, full-row selectable format behavior, scroll-reachable editor actions, assertive validation errors, Date/Time picker reachability, and Create → Details → Edit persistence through the repository contract.
- one shared lifecycle-aware Dashboard minute ticker instead of one ticker coroutine per tracker card; Details retains its own ticker only while the Details screen is active.
- Android 16 runtime CI keeps instrumentation mandatory while using KVM when available and software emulator acceleration when the hosted runner lacks usable `/dev/kvm`.
- Rendered light/dark visual evidence uses deterministic in-place Compose theme selection rather than Activity recreation; persistence and recreation behavior remain the responsibility of dedicated runtime tests.

PR #9 was squash-merged to `main` as `5b14c58af68b8748e71f6af0c462650d06e852d6`. Exact candidate `b0ccc4e1f00c089ad8257f50ffb167df4b9e7af6` passed Android Development Foundation run `35963071718`: local-only manifest guard, JVM tests, Android lint, application/instrumentation APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (13 tests)`. The runtime gate now detects emulator acceleration and keeps instrumentation mandatory, using KVM when available and software acceleration otherwise. Draft PR #8 was closed unmerged after its useful non-overlapping work was consolidated into PR #9.

Representative TalkBack device acceptance, large-font visual acceptance, switch-access/keyboard acceptance, localization/RTL acceptance, approved Since-specific icon/accent mapping, and complete downstream GLAZE UI consumer acceptance remain separate release obligations.


### Verified large-font and RTL automated evidence

Current `main` adds CI-level accessibility/layout evidence without claiming representative-device acceptance:

- forced 2× font-scale Compose instrumentation verifies the Create editor keeps the title, start date/time, zone field, and Save action reachable through its scroll container;
- forced RTL layout-direction instrumentation verifies the Create heading, title field, zone field, and Save action remain reachable;
- these tests run in the existing mandatory Android 16 instrumentation lane.

PR #12 was squash-merged to `main` as `634623f03954bcb1e1d53107e5187b7c724b40c4`. Exact candidate `17464fd4c8fa13ed0b3123ae26b28effa01fee57` passed Android Development Foundation run `35965422146`, including manifest guard, JVM tests, Android lint, app/test APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (15 tests)`.

Automated large-font and forced-RTL evidence is not representative-device TalkBack, visual-regression, keyboard/switch-access, translation/localization, or downstream GLAZE consumer acceptance.


### Verified Arabic localization and RTL resource evidence

Current `main` adds bounded Android localization support without changing persistence, permissions, networking, schema, or tracker behavior.

- provides Arabic translations for the complete current 51-string Since UI resource surface while preserving the product name **Since**;
- declares English and Arabic through Android locale metadata;
- retains `android:supportsRtl="true"`;
- adds Android instrumentation that creates an Arabic configuration context, verifies RTL layout direction, and verifies representative Arabic resource resolution.

PR #16 was squash-merged to `main` as `d204cbd4464539a15b8f1ed6e0d9e9dfef4ddbb7`. Exact candidate `3867afe88e6570f647b65dfa224c5b67708d998c` passed Android Development Foundation run `35967471510`, including the local-only manifest guard, JVM tests, Android lint, app/test APK assembly, Room-schema drift verification, and Android 16 instrumentation `OK (16 tests)` with Arabic RTL/resource verification.

This does not establish native-language translation review, representative-device RTL visual acceptance, Arabic TalkBack acceptance, physical keyboard/external-switch acceptance, complete localization coverage for future features, or downstream GLAZE UI consumer acceptance.


### Verified keyboard-focus automated evidence

Current `main` also includes focused Android Compose keyboard-navigation evidence:

- instrumentation explicitly requests keyboard input mode;
- the Create editor Cancel action receives focus;
- Tab advances focus from Cancel to Save;
- Enter activates the focused Save action and surfaces the existing validation error;
- only the focused test opts into the experimental Compose testing APIs required by the current Compose UI dependency.

PR #19 was squash-merged to `main` as `a24942ce244b2182d72aadde0f57eb10bebc8f5c`. Exact candidate `bdc89ec81fae8a195e7f611efa4ca0f81c614650` passed Android Development Foundation run `35968237474` with Android 16 instrumentation `OK (17 tests)`. Exact merged-main run `35968638222` also passed `OK (17 tests)` and produced unexpired Development artifact `since-runtime-apks` ID `10794409287`.

This is automated emulator evidence only. Representative physical-keyboard/external-switch acceptance, representative-device TalkBack/large-font visual acceptance, native-language Arabic review, representative-device RTL acceptance, downstream GLAZE UI consumer acceptance, Release Candidate, production, and Stable remain open.

### Verified Since visual interface refinement

Current Development source additionally includes a dedicated GoreeCloud Since Android presentation layer informed by the current Stable Glaze UI V1.6 direction without claiming downstream consumer conformance:

- explicit light and dark Since color schemes with deep-teal primary identity, restrained amber goal accents, neutral layered surfaces, and continuous rounded geometry;
- a compact Dashboard first-run state rather than a vertically centered empty canvas;
- redesigned tracker cards with grouped tracker type, elapsed-time, and goal hierarchy;
- a Details elapsed-time hero that also keeps the persisted start date/time and IANA zone visible;
- grouped information surfaces for display format, goal, and notes;
- two-column selectable display-format controls that preserve radio-button semantics and touch targets;
- larger tracker-type option surfaces and clearer Create/Edit section grouping;
- normal user-facing screens no longer expose implementation/development deferral copy;
- Android edge-to-edge layout with app-theme-aware status/navigation system-bar icon appearance integrated by PR #43.

Exact source candidate `41a61992a76a8c7475c98666d8db5993913b0c50` passed Android Development Foundation run #67 / `35973932878`, including Android 16 instrumentation with all 17 tests passing.

Representative physical-device visual acceptance and full downstream GLAZE UI consumer acceptance remain open release obligations.

### Verified app-theme system-bar synchronization

PR #43 corrected the owner-reported Dark-mode system-bar contrast defect without changing tracker persistence, Room schema, local-only permissions, or chronology behavior. Exact candidate `13480c0c64c4611edb0f2d84ef91263bd22b3831` passed Android Development Foundation run `36077943497` / #159 and merged to `main` as `60031a0632ae169c22c8a2a52eed6b5678646f0d`. Exact merged-main run `36079029190` / #160 also passed.

Merged-main rendered review confirmed the reported dark-mode status-bar defect was corrected in Android 16 evidence. Representative physical-device/OEM system-bar behavior, gesture/three-button navigation-mode acceptance, and full downstream Glaze consumer acceptance remain open.

### Verified rendered emulator visual evidence

The current Development source includes an Android-rendered visual-evidence lane for the principal Since flow:

- launches the real Since Compose UI in an Android 16 instrumentation host using the production `SinceTheme`; this rendered-evidence test does not independently prove `MainActivity` startup behavior;
- captures the empty Dashboard, tracker-type chooser, Create Streak, Tracker Details, and populated Dashboard as full-device PNG evidence;
- extracts the evidence from the target app sandbox after instrumentation succeeds;
- publishes the images as the `since-rendered-ui` GitHub Actions artifact;
- verifies that the exact rendered source uses an explicit neutral surface-container hierarchy rather than inheriting unrelated Material defaults that previously introduced an unintended pink/purple cast into cards and editor sections.
- Prior rendered evidence applied a test-only system-bar appearance override, so it did not validate production status/navigation icon contrast. PR #43 removed that override and moved the behavior into production `SinceTheme`.

Exact source-bearing candidate `7c77ab28811b4aedecc733e0479dd5b57edda0d4` passed Android Development Foundation run #73 / `35976727631`, including Android 16 instrumentation and successful rendered-evidence upload. Artifact `since-rendered-ui` ID `10798890114` has digest `sha256:c6f865e01296ed28da2a7c2059e60bf4af367e6220dacce88b570df526928cc5`.

This establishes bounded Android-emulator rendered evidence only. Human visual acceptance, representative physical-device/OEM rendering, TalkBack/assistive-technology acceptance, and full downstream GLAZE UI consumer acceptance remain open.

### Verified canonical application identity source

GoreeCloud Since now packages a traceable adaptive Android launcher identity derived from the approved canonical source in `GoreeCloud/branding-assets`:

- canonical source: `products/since/app-icon.svg`;
- canonical source Git blob: `a107f860759e745ff16f2b5bf1954b93fbb17937`;
- canonical branding integration: `8c27acbdf7c83624a7396fdb4d7589f0643b4cc0` / branding PR #25;
- canonical branding post-merge validation: run #93 / `35979255165`;
- Android adaptive normal and round launcher declarations;
- Android 13+ monochrome/themed icon support;
- consumer-repository `BRANDING.md` provenance contract and manifest/build guard coverage.

The identity uses an open elapsed-time ring, start-point node, and restrained clock hands as the product identity lock. Canonical artwork was reviewed at 512 px, 128 px, 48 px, 32 px, and grayscale before publication.

Per-tracker icon/accent selection remains a separate Development feature and remains blocked on an approved Since-specific GLAZE mapping. Application launcher identity does not satisfy or bypass that tracker-level mapping requirement.

### Verified Since visual hierarchy refinement

The current Development source includes the follow-up Android presentation refinement integrated through PR #24:

- the Dashboard `Add tracker` action uses primary emphasis instead of a low-emphasis container treatment;
- populated tracker cards separate the **Elapsed** label from the time value and reduce elapsed-value scale for calmer hierarchy;
- Tracker Details uses a neutral durable content surface rather than a large accent-filled panel while retaining teal emphasis for the tracker type and elapsed value;
- Permanent Event and Streak options in the tracker-type chooser use equal neutral surfaces so accent color does not imply a preselected or recommended tracker type;
- Create/Edit text fields use the same rounded Glaze geometry as the surrounding editor surfaces.

Exact PR #24 candidate `ee3d69886644840c00aff2df9b5f72962ed6109b` passed Android Development Foundation run `35981477112` / #81 with the manifest/canonical-identity guard, JVM tests, Android lint, APK assembly, Room-schema drift verification, Android 16 instrumentation `OK (18 tests)`, and rendered-evidence upload. Human review of the five exact-head emulator scenes found no clipping, text corruption, unintended Material pink/purple inheritance, broken field geometry, or loss of content hierarchy.

PR #24 was squash-merged as `7862a4de6ecebbebbd5466f82d8a2867e7be49e8`. Exact merged-main run `35982116803` / #82 also passed Android 16 instrumentation `OK (18 tests)` and produced `since-runtime-apks` artifact ID `10800732670`, digest `sha256:7abf1a90e5881a9804d213effb45f68256c5d23ee585559395949f6670d2c0b2`, plus `since-rendered-ui` artifact ID `10800344407`, digest `sha256:be77a211a844a1d84b2d3028721e85009f5694e58e957aa380fea752e3f4777d`.

This remains bounded emulator and CI evidence. Representative physical-device/OEM rendering, native-language review, representative TalkBack/large-font/RTL/keyboard/switch acceptance, and full downstream GLAZE UI consumer acceptance remain open.



### Verified dual-theme Android rendered evidence

The current Development source now captures the principal Since flow in both light and dark modes on Android 16:

- empty Dashboard;
- tracker-type chooser;
- Create Streak;
- Tracker Details;
- populated Dashboard.

PR #27 exact final head `2b387c57fbb603857bfbe5dc70c909acea483377` passed Android Development Foundation run `35986151222` / #91 with the manifest/canonical-identity guard, JVM tests, Android lint, APK assembly, Room-schema drift verification, Android 16 instrumentation `OK (18 tests)`, and successful upload of all ten rendered scenes.

PR #27 was squash-merged as `eb8975dca84d2db613b05af07f7b4b9d3f1ed027`. Exact merged-main run `35986755499` / #92 also passed Android 16 instrumentation `OK (18 tests)` and produced:

- `since-runtime-apks` artifact ID `10802144523`, digest `sha256:6d2271641128def6c8d846b00bf0295ca34ae77fc9321975120caf91a012a034`;
- `since-rendered-ui` artifact ID `10802244368`, digest `sha256:1e4ecc14b94128dc1849311a1decb52b7ccdcf0c240994d8aae279822f51e866`.

Human review of the ten exact-main scenes verified readable light/dark contrast, preserved content hierarchy, and no clipping, text corruption, or reappearance of the unintended pink/purple Material surface inheritance. This closes the emulator dark-mode rendered-evidence gap only. Representative physical-device/OEM rendering, assistive-technology acceptance, and downstream GLAZE UI consumer acceptance remain open.

### Verified empty Dashboard first-run refinement

GoreeCloud Since now presents the no-data state as a cohesive first-run surface rather than separating the explanation from its primary action:

- the empty Dashboard displays the canonical Since launcher mark already packaged by the application;
- `No trackers yet` is presented as the empty-state heading instead of a status chip;
- the explanatory copy uses normal body hierarchy;
- `Add tracker` is a full-width primary action inside the empty-state card, eliminating the large visual gap between message and action;
- populated dashboards retain the floating `Add tracker` action;
- empty-state list padding no longer reserves floating-action clearance that is not used.

PR #30 exact candidate `9b23193e733a350866511e29596b9339d1999823` passed Android Development Foundation run `35989103579` / #97 with the canonical-identity/manifest guard, JVM tests, Android lint, APK assembly, Room-schema verification, Android 16 instrumentation `OK (18 tests)`, and all ten light/dark rendered scenes. The empty Dashboard light/dark evidence was visually reviewed and showed the intended in-card branding/action hierarchy without clipping, text corruption, or dark-mode contrast defects.

PR #30 was squash-merged as `979972f2564b6ebd63ed59aeb658cbf4cd261eb1`. Exact merged-main run `35989719467` / #98 also passed Android 16 instrumentation `OK (18 tests)` and produced:

- `since-runtime-apks` artifact ID `10803354250`, digest `sha256:180b24d5661191c8e60a33b31946ab2a1050a5f514350399ef08b09796330a73`;
- `since-rendered-ui` artifact ID `10803653756`, digest `sha256:d316677947be78aace4d6462b55e4e1e31963e9d4136e88a265b6ca0a2e99dc3`.

This remains Development emulator evidence. Representative physical-device/OEM and assistive-technology acceptance remain open.




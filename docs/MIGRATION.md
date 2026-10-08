# GoreeCloud Since source migration

Owner directive: October 8, 2026. Previous authority: GoreeCloud/android-app-defaults/apps/since/, source commit 9097c8cb7adecc3c969f162fc33a9e8fdde20cc4, app subtree SHA 6600095055fd4dbbc40d3df2b3901eb063a0e5fc (57 tracked files). Extracted Since-only Git history tip 6c26cccd8d912d34d427e13e14eef0282ae9c86c (25 commits), imported alongside the standalone repository initial history. The app sources are relocated to app/ with original file blobs preserved; root Gradle setup and app-specific Android CI are adapted from that exact monorepo revision. No local app user data, credentials, or signing keys are copied. Production, physical-device, and platform acceptance remain separate. Required completion: exact-head standalone CI/merge/readback; protected monorepo removal; canonical GitHub repository-index and task-record reconciliation.

## October 8, 2026 — verified standalone integration

- Source-transfer PR: [GoreeCloud/since #1](https://github.com/GoreeCloud/since/pull/1), merged into standalone `main` as `2e68fb5221b38e347e5133b08fb826a9ca7909ba`.
- All 57 original Since file blobs matched their destination counterparts; 25 extracted app-specific commits and the prior standalone initial commit were preserved.
- Exact PR-head Android build, privacy/schema checks, Android 16 runtime instrumentation, and required Development gate passed. Post-merge standalone `main` passed the same required checks.
- Standalone `main` protection enforces pull-request integration, strict required Development CI, resolution of review conversations, and prevents force pushes and branch deletion.
- The source-removal operation is separately staged as part of [android-app-defaults PR #291](https://github.com/GoreeCloud/android-app-defaults/pull/291); the protected PR merged as `068a958074431fabecff8748c9b630ea9dfdf6b9`, and main-tree readback confirmed the old directory absent.
- No representative physical-device, update-in-place signing, or Stable/production acceptance is inferred from this code migration.
- GoreeCloud Drive's repository index and product tasks require in-place reconciliation after the final old-source retirement.

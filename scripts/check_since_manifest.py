#!/usr/bin/env python3
from pathlib import Path
import sys

manifest = Path("app/src/main/AndroidManifest.xml")
text = manifest.read_text(encoding="utf-8")

for forbidden in (
    "android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
):
    if forbidden in text:
        print(f"forbidden Since MVP permission found: {forbidden}", file=sys.stderr)
        sys.exit(1)

required_fragments = (
    'android:allowBackup="false"',
    'android:usesCleartextTraffic="false"',
    'android:exported="true"',
    'android:icon="@mipmap/ic_launcher"',
    'android:roundIcon="@mipmap/ic_launcher_round"',
)

for required in required_fragments:
    if required not in text:
        print(f"required fail-closed manifest setting missing: {required}", file=sys.stderr)
        sys.exit(1)

branding = Path("app/BRANDING.md").read_text(encoding="utf-8")
for required_branding in (
    "GoreeCloud/branding-assets",
    "products/since/app-icon.svg",
    "a107f860759e745ff16f2b5bf1954b93fbb17937",
):
    if required_branding not in branding:
        print(f"Since canonical branding provenance missing: {required_branding}", file=sys.stderr)
        sys.exit(1)

for required_asset in (
    "app/src/main/res/drawable/ic_launcher_background.xml",
    "app/src/main/res/drawable/ic_launcher_foreground.xml",
    "app/src/main/res/drawable/ic_launcher_monochrome.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
    "app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
):
    path = Path(required_asset)
    if not path.is_file() or path.is_symlink():
        print(f"Since canonical Android identity derivative missing: {required_asset}", file=sys.stderr)
        sys.exit(1)

build = Path("app/build.gradle.kts").read_text(encoding="utf-8")
if 'applicationId = "com.goreecloud.since"' not in build:
    print("Since application identity is missing from Gradle configuration", file=sys.stderr)
    sys.exit(1)

print("Since manifest boundary verified: local-only, fail-closed backup/cleartext settings, canonical launcher identity, launcher-only exported activity.")

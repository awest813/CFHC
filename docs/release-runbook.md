# Release Runbook

Step-by-step procedure for cutting a CFHC release (Android + desktop).
The [ship-readiness audit](ship-readiness-audit.md) lists what must be true
before starting; this doc is the mechanical how-to.

## 0. One-time setup

### Release keystore
The keystore **is** the app's identity on Google Play. Current location:
`C:\Users\allen\cfhc-signing\cfhc-release.jks` (outside the repo, gitignored even
if copied inside). Local credentials live in `signing.properties` at the repo
root (gitignored; template in `signing.properties.example`).

- [ ] **Back up** the `.jks` + passwords to a second location (password manager,
  encrypted drive). Losing the keystore after the first Play upload is
  unrecoverable — the app could never be updated again.
- [ ] Record the certificate SHA-256 fingerprint (Google Play Console → App
  signing may ask for it):
  `keytool -list -keystore cfhc-release.jks`

### CI signing secrets (Settings → Secrets and variables → Actions)
| Secret | Value |
|---|---|
| `CFHC_KEYSTORE_BASE64` | `base64 -w0 cfhc-release.jks` (or `certutil -encode` output body) |
| `CFHC_STORE_PASSWORD` | from `signing.properties` |
| `CFHC_KEY_ALIAS` | `cfhc` |
| `CFHC_KEY_PASSWORD` | from `signing.properties` |

## 1. Pre-flight (on the release commit)

```bash
git status --short                 # must be empty
./gradlew quickVerify              # desktop jar + debug APK + unit tests
./gradlew :app:testDebugUnitTest -PrunStress \
    --tests "simulation.Stress100SeasonsTest" \
    --tests "simulation.Debug20SeasonsFlowTest"   # long-running suites
```

Push and let both CI jobs (`.github/workflows/ci.yml`) go green before tagging.

## 2. Version check

One command bumps every location atomically (validated before any file is
written): `./gradlew bumpVersion -Ppatch` updates `app/build.gradle`
(versionName/versionCode), `gradle.properties` → `desktopVersion`, and the
`DesktopVersion.java` fallback. Mapping: Android `v1.4.N` → desktop `1.4` +
Nth letter (v1.4.5 → 1.4e; patches past 26 fall back to the number). Current
release identity: `v1.4.5` / versionCode 320 / desktop `1.4e`. If versionCode
320 was already consumed on the Play Console, bump first.

## 3. Local signed build + smoke test (recommended before tagging)

```bash
./gradlew :app:assembleRelease :app:bundleRelease   # fails fast if signing is missing
./gradlew -p desktop-standalone :engine:desktopStandaloneGate
```

- Verify the signature is NOT the debug key:
  `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/mapping/release/mapping.txt` must exist (keep it — needed
  to de-obfuscate Play Console crash traces).
- Install the release APK on a device/emulator and start a new league: team,
  conference, and player names must be real strings, never `[teams]` /
  `[bowls]` (resource-shrink + `getIdentifier()` check).

## 4. Tag and publish

```bash
git tag -a v1.4.5 -m "CFHC v1.4.5"
git push origin v1.4.5
```

The `v*` tag triggers `.github/workflows/release.yml`, which builds the signed
AAB/APK + desktop jar and attaches them to a GitHub Release with generated
notes. Edit the generated notes to summarize gameplay changes (see
`docs/desktop-release-notes-1.4e.md` for the previous format).

## 5. Post-release verification

- [ ] Release page shows the AAB, release APK, and `CFHC-desktop-*.jar`.
- [ ] In-app **Help → Check for Updates** (desktop) detects the new release.
- [ ] Play Console: upload the AAB, spot-check the mapping.txt upload.
- [ ] Optional local extras: `:engine:desktopPortableZip` (zip of the jpackage
  image), unsigned `desktopMsi`/`desktopDmg` — signed installers remain blocked
  on code-signing certificates (see `docs/desktop-fix-list.md`).

## Failure modes

| Symptom | Cause / fix |
|---|---|
| `Refusing to build a release artifact signed with the debug key` | Expected guard — provide `signing.properties` or env vars (CI only: `-PallowDebugSignedRelease`). |
| Release workflow android job fails | Missing/mistyped secrets; check `CFHC_KEYSTORE_BASE64` decodes to a valid JKS. |
| New league shows `[teams]` in a release build | Resource shrinker stripped engine strings — `src/main/res/raw/keep.xml` must list them; re-run the smoke test. |

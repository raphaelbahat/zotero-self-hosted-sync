# Task Validation: custom-sync-server

- Validated against: live framework/library/tool documentation
- Validation date: 2026-09-24
- Verdict: READY (post-revision, two rounds)

Two read-only validator groups checked the original tasks against live documentation; the
operator approved the revisions they listed, they were applied, and a second focused round then
re-validated the claims added or changed by the streaming/cleartext revision. Both rounds'
findings are recorded below; every verdict cites the source used, and each fix was re-checked
against that source before the verdict was restated.

---

## Round 1 — findings and resolution

### 1.3 — bare `baksmali` covered one DEX of eight

- Fix applied: task 1.3 requires every DEX entry (`baksmali list dex` loop or `apktool d`) and records the multidex fact.
- Re-checked: `DexInputCommand.java` (v2.5.2) resolves a single entry (`container.getEntry("classes.dex")`, first-entry fallback); live `baksmali list dex` returns `classes.dex` … `classes8.dex`.
- Evidence: https://github.com/JesusFreke/smali/blob/v2.5.2/baksmali/src/main/java/org/jf/baksmali/DexInputCommand.java

### 3.2 — CLI flag semantics

- Fix applied: `list-patches --patches <mpp> -pvo`, with `-p` = `--with-packages` and `-pvo` = packages + versions + options.
- Re-checked in source: `names = ["--patches"]`, `["-p","--with-packages"]`, `["-v","--with-versions"]`, `["-o","--with-options"]`.
- Evidence: https://github.com/MorpheApp/morphe-desktop/blob/main/src/main/kotlin/app/morphe/desktop/command/ListPatchesCommand.kt

### 4.1 — `-f` is not the input APK

- Fix applied: 4.1 carries the exact invocation with the APK as a trailing positional argument and `-f` described as "skip the version compatibility check".
- Re-checked: `["-f","--force"]`, `["-O","--options"]`, `["-o","--out"]`, `["--keystore"]`, `["--exclusive"]`, and `@CommandLine.Parameters(arity = "1")`.
- Evidence: https://github.com/MorpheApp/morphe-desktop/blob/main/src/main/kotlin/app/morphe/desktop/command/PatchCommand.kt

### targetSdk figure

- Fix applied: design, proposal and the research note now state that the released APK reports targetSdk 35 (the source tree declares 36), with cleartext blocked since API 28.
- Evidence: live `aapt dump badging` on the pinned APK.

### DSL execution prerequisites (2.3–2.6)

- Fix applied: `category("…")` inside the block; `PatchException` for reason-carrying refusals; the patcher-native rewrite path; the manifest `package` attribute named as the only renaming path.
- Re-checked: `Patch.kt` (577, 824), `Option.kt` (34–36, 197), `morphe-patcher.api` (506), `ReplaceStringPatch.kt` imports.
- Evidence: https://github.com/MorpheApp/morphe-patcher/blob/v1.14.1/src/main/kotlin/app/morphe/patcher/patch/Patch.kt ; `.../Option.kt` ; `.../api/morphe-patcher.api`

---

## Round 2 — the streaming/cleartext revision

### A. (2.7) optional option with a patch-time fallback — **VALID**

- `stringOption(key, default = null, values = null, title, description, required = false, validator)`. `required` defaults to false, and an unset optional option returns its default rather than throwing; "not supplied" is therefore `null` (or an explicitly empty default), so the derivation belongs to the patch code. The implementation handles both (`optionValue?.trim().orEmpty()`).
- Evidence: https://github.com/MorpheApp/morphe-patcher/blob/main/src/main/kotlin/app/morphe/patcher/patch/Option.kt ; https://github.com/MorpheApp/morphe-patches/blob/main/patches/src/main/kotlin/app/morphe/patches/all/misc/installer/ChangeInstallerSource.kt

### B. (2.8) `resourcePatch` editing the compiled resource XML — **VALID**

- `resourcePatch(...)` exists; `ResourcePatchContext.document("res/xml/network_security_config.xml")` returns a real DOM `Document` that is written back on `close()`, so `use { }` is required. A bundle may hold both a bytecode patch and a resource patch: both contexts are decoded and both run, ordered by dependency and then by name — there is no type-based ordering, so `dependsOn` is the only explicit lever.
- The library helpers (`getNode`/`adoptChild`) would require `app.morphe:morphe-patches-library`; the implementation deliberately uses plain JDK DOM, so **no dependency was added** (recorded in task 2.5).
- Evidence: https://github.com/MorpheApp/morphe-patcher/blob/main/src/main/kotlin/app/morphe/patcher/patch/ResourcePatchContext.kt ; `.../util/Document.kt` ; `.../Patcher.kt` ; https://github.com/MorpheApp/morphe-patches/blob/main/patches/src/main/kotlin/app/morphe/patches/all/misc/network/OverrideCertificatePinningPatch.kt

### C. (spec scenario + ADR-0003) Android cleartext semantics — **VALID**

- The opt-in form is `<domain-config cleartextTrafficPermitted="true"><domain includeSubdomains="true">host</domain></domain-config>`; cleartext is off by default from API 28, and OkHttp enforces the policy (`RealRoutePlanner` → `Platform.isCleartextTrafficPermitted` → `NetworkSecurityPolicy`).
- Confirmed in the pinned artifact: `classes7/okhttp3/internal/connection/RealRoutePlanner.smali` carries that check and the app opens its WebSocket through OkHttp — so the `ws://` override in the operator's deployment needs exactly the allowlist entry the resource patch adds.
- Evidence: https://developer.android.com/privacy-and-security/security-config ; https://github.com/square/okhttp/blob/master/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RealRoutePlanner.kt ; https://github.com/square/okhttp/blob/master/okhttp/src/androidMain/kotlin/okhttp3/internal/platform/AndroidPlatform.kt

### D. (4.1) the CLI reads a BKS keystore — **VALID**, plus one fix (applied)

- The toolchain is BKS-first: the patcher's signer constructs `KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME)` and installs BouncyCastle itself, and the CLI's `KeystoreImporter.ensureBks` byte-sniffs the file and returns an already-BKS keystore unchanged. The operator's `Morphe.keystore` is BKS v2 with alias `Morphe` and an empty store password — **no conversion is required**.
- **Fix applied (the round's only NEEDS_FIX):** the 4.1 command omitted the enable flag. `-O/--options` is declared inside the enable-selection group and the CLI applies options only for enabled selections, so 4.1 now passes `-e "<patch name>"` (with the real `server`/`streaming` keys) before the option values.
- Evidence: https://github.com/MorpheApp/morphe-patcher/blob/main/src/main/kotlin/app/morphe/patcher/apk/ApkSigner.kt ; https://github.com/MorpheApp/morphe-desktop/blob/main/src/main/kotlin/app/morphe/engine/util/KeystoreImporter.kt ; `.../desktop/command/PatchCommand.kt`

---

## Post-revision gate

- `openspec validate custom-sync-server --strict` → **Change 'custom-sync-server' is valid**
- Residual-reference sweep over the change's artifacts → no stale invalid detail remains.

---

## Verdict

`VERDICT: READY`

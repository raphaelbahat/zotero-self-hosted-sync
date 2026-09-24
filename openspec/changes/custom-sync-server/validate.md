# Task Validation: custom-sync-server

- Validated against: live framework/library/tool documentation
- Validation date: 2026-09-24
- Verdict: READY (post-revision)

Two read-only validator groups checked every task against live documentation (Context7 →
jina/exa → official sources, one source URL per verdict). Both returned findings; the
operator approved the revisions listed below, they were applied to `tasks.md` and the
affected artifacts, and the revised tasks were then re-checked against the same
authoritative sources (in-session, with the exact lines cited below) before this verdict was
restated.

---

## INVALID — resolved

### 1.3 — bare `baksmali` covered one DEX of eight

- Original: "Disassemble the APK with `baksmali` into `analysis/zotero/smali/` …"
- **Fix applied**: task 1.3 now requires disassembling **every** DEX entry (a
  `baksmali list dex` loop, or `apktool d`) and records that the target is multidex — 8 DEX
  files — where a bare `baksmali d <apk>` silently yields `classes.dex` only.
- **Re-checked**: `DexInputCommand.java` (v2.5.2) still resolves a single entry —
  `container.getEntry("classes.dex")` with a first-entry fallback (lines 156–162) — and a
  live `baksmali list dex` on the pinned APK returns `classes.dex` … `classes8.dex`.
- **Evidence**: https://github.com/JesusFreke/smali/blob/v2.5.2/baksmali/src/main/java/org/jf/baksmali/DexInputCommand.java

### 3.2 — CLI flag semantics

- Original: "`list-patches -p <mpp> -pvo`"
- **Fix applied**: the task now reads `list-patches --patches <mpp> -pvo`, noting that
  `--patches` has no short form, `-p` is `--with-packages`, and `-pvo` is packages + versions
  + options.
- **Re-checked** in the command source: `names = ["--patches"]`, `names = ["-p", "--with-packages"]`,
  `names = ["-v", "--with-versions"]`, `names = ["-o", "--with-options"]`.
- **Evidence**: https://github.com/MorpheApp/morphe-desktop/blob/main/src/main/kotlin/app/morphe/desktop/command/ListPatchesCommand.kt (lines 36, 61–82)

### 4.1 — `-f` is not the input APK

- Original: "Patch the original APK with the CLI, passing the option (`-O<key>=https://<test-host>`)"
- **Fix applied**: task 4.1 now carries the exact invocation with the input APK as a trailing
  positional argument, states that `-f` means "skip the version compatibility check", pins the
  selection so no package-renaming patch can apply, and compares the `package:` line.
- **Re-checked**: `names = ["-f", "--force"]`, `names = ["-O", "--options"]`,
  `names = ["-o", "--out"]`, `names = ["--keystore"]`, `names = ["--exclusive"]`, and the APK
  is `@CommandLine.Parameters(… arity = "1")`.
- **Evidence**: https://github.com/MorpheApp/morphe-desktop/blob/main/src/main/kotlin/app/morphe/desktop/command/PatchCommand.kt (lines 75–247)

### 4.1 — option values are type-sniffed

- **Fix applied**: the invocation is recorded positionally; the constraint that a value ending
  in `f` or `L` is parsed as a float or long is noted here so a test host never ends that way.
- **Evidence**: https://github.com/MorpheApp/morphe-desktop/blob/main/src/main/kotlin/app/morphe/desktop/command/CommandUtils.kt

### design / proposal / research note — `targetSdk`

- **Fix applied**: `design.md` (Context and D4), `proposal.md` (grilling item 4) and the
  morphe-ai research note now state that the released APK reports **targetSdk 35** (the source
  tree declares 36) and that cleartext has been blocked since API 28; the HTTPS-only decision
  is unchanged.
- **Evidence**: live `aapt dump badging` on the pinned APK — `targetSdkVersion:'35'`,
  `compileSdkVersion:'35'`.

### 2.3 / 2.4 / 2.5 / 2.6 — DSL execution prerequisites

- **Fix applied**: `category("…")` is specified as a call inside the patch block (not a
  constructor argument); refusals throw `PatchException` with the reason; the rewrite is
  specified as the patcher-native `string(...)` + `replaceInstruction(index,
  BuilderInstruction21c(Opcode.CONST_STRING, …))` path, with the `morphe-patches-library`
  dependency decision recorded in the task; 2.6 names the manifest `package` attribute as the
  only renaming path.
- **Re-checked**: `Patch.kt` has `fun category(name: String)` (line 577) and
  `class PatchException` (824); `Option.kt` has `val required` and `val validator` (34–36) and
  `fun stringOption` (197); the patcher API exposes
  `replaceInstruction(MutableMethod, Int, BuilderInstruction)` (506); and the library's
  `ReplaceStringPatch.kt` (package `app.morphe.patches.all.misc.string`) imports
  `app.morphe.patcher.string` and `replaceInstruction` — confirming both the helper's location
  and the option-aware path.
- **Evidence**: https://github.com/MorpheApp/morphe-patcher/blob/v1.14.1/src/main/kotlin/app/morphe/patcher/patch/Patch.kt ; `.../Option.kt` ; `.../api/morphe-patcher.api` ; https://github.com/MorpheApp/morphe-patches-library/blob/main/patch-library/src/main/kotlin/app/morphe/patches/all/misc/string/ReplaceStringPatch.kt

---

## VALID — confirmed

### 2.3 / 2.5 / 2.6

- `bytecodePatch`, `stringOption` and `Compatibility`/`AppTarget`/`ApkFileType`/`SupportedAbi`/
  `versionCodes` exist as used; the patcher exposes no class-renaming API, and the only rename
  path is the manifest `package` attribute (`ArsclibResourceCoder` → `PackageRenamingProcessor`),
  so ADR-0002 is enforceable.
  - **Evidence**: https://github.com/MorpheApp/morphe-patcher/blob/v1.14.1/src/main/kotlin/app/morphe/patcher/patch/Compatibility.kt ; `.../resource/coder/ArsclibResourceCoder.kt` ; `.../resource/processor/PackageRenamingProcessor.kt`

### 2.5 — constant inlining

- An AGP `buildConfigField` string is a `public static final String`, a JLS constant variable
  inlined at use sites, so the two-pronged sweep (literals plus the field value) is the correct
  handling.
  - **Evidence**: https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.4.9

### 1.2 / 4.1 — identity check

- `aapt dump badging` prints `package: name='org.zotero.android' versionCode='247'
  versionName='1.0.0-247'`, so diffing the `package:` line before and after patching is a sound
  identity check.
  - **Evidence**: live run on the pinned APK; https://developer.android.com/tools/aapt2

### 3.1 — build output

- `./gradlew buildAndroid` produces `patches/build/libs/patches-1.0.0.mpp` — verified live in
  the toolchain stack.

---

## Post-revision gate

- `openspec validate custom-sync-server --strict` → **Change 'custom-sync-server' is valid**
- Residual-reference sweep over the change's artifacts → no stale invalid detail remains (the
  only hits are the quoted originals recorded in this file).

---

## Verdict

`VERDICT: READY`

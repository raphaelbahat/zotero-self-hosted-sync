## Why

Zotero Android compiles its service host into the build: the API base is one constant
(`BuildConfig.BASE_API_URL`, `app/build.gradle.kts:41`) and the live-update socket is
another (`wss://stream.zotero.org`, `websocket/WebSocketController.kt:93`). The app
exposes no runtime setting for a different host, so a self-hosted sync server such as
altero cannot be used at all — altero's documentation states plainly that the official
mobile apps need a patched client. Users who run their own Zotero server are therefore
locked out of the official Android app.

## What Changes

- A new patch, **Custom sync server**, adds a string option for the server address
  (e.g. `https://zotero.example.org`) and rewrites both compiled endpoint constants:
  the API base becomes the given origin, and the streaming endpoint becomes
  `wss://<origin>/stream`.
- Login / account-approval and attachment-upload URLs are **server-provided**, so they
  follow the origin automatically; no further client changes are required for a first
  working version.
- The patched application keeps its **original package identity**: `org.zotero.android`,
  same package name and version code. The patch rewrites endpoint strings only — it never
  renames, repackages or re-namespaces the app or its classes, so the result updates the
  installed app in place (signature permitting, since Morphe re-signs with the user's
  keystore).
- **HTTPS only in v1**: a non-HTTPS address is refused at patch time with a clear message,
  instead of producing an APK that Android's cleartext policy would break at runtime.
- Compatibility is declared for the pinned target `org.zotero.android` 1.0.0 (build 247,
  universal APK).
- No server-side changes. The server must implement the Zotero Web API and the streaming
  API (altero does).

## Capabilities

### New Capabilities

- `custom-sync-endpoint`: the patched client's endpoint routing — which hosts are
  rewritten, how the origin is derived and validated, what happens to server-provided
  URLs, and the guarantee that the application's package identity is unchanged.

### Modified Capabilities

- None. This repository holds no specs yet; the patch is additive.

## Impact

- `patches/src/main/kotlin/app/anondev/patches/zotero/` — new patch, fingerprints and
  compatibility constants (the template's `example` patch is removed once this one lands).
- `patches/src/main/kotlin/app/anondev/patches/zotero/example/` and the template's
  `extensions/` example — deleted; v1 is a pure bytecode patch with no runtime extension
  code.
- Generated release artifacts (`patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`,
  README patch list) change only through the existing release workflow.
- Analysis evidence (recon, smali findings, device test notes) lives in the morphe-ai
  workspace under `analysis/zotero/`, not in this repository.
- Reversibility: users point the patched app back at zotero.org only by re-patching; the
  patch itself is the switch, so no server or account state is affected.

## Grilling: decisions, evidence and assumptions

Recorded from the `grill-me` / `grilling` pass (the skill is not installed in this
harness; its method was consulted read-only from the author's OpenSpec store). Every
question below is a **decision for the user**; the answers marked *(confirmed)* came from
this conversation, including the four recommendations confirmed at the approval gate.

1. **One origin input or several?** *(confirmed: one)* — streaming is a separate single
   constant that can be derived from the same origin (`wss://<origin>/stream`); the login
   URL and attachment-upload URL are returned by the server, so they need no input.
2. **Static patch-time rewrite or a runtime extension?** *(confirmed: static rewrite)* —
   the address is chosen in the patch dialog, so it is known at patch time; a runtime
   extension would add code that must be maintained and would be the only part of the
   bundle that runs inside the app.
3. **Target version?** *(confirmed)* — 1.0.0, build 247, universal APK
   (`Zotero-for-Android-1.0.0-247-universal.apk`).
4. **HTTPS only, or cleartext too?** *(confirmed: HTTPS only)* — the user's server is
   reachable over HTTPS; cleartext would additionally require a resource patch for
   `network_security_config.xml` because the target's cleartext policy blocks HTTP (the released APK reports targetSdk 35; cleartext has been blocked since API 28).
5. **Keep the original package identity?** *(confirmed: yes)* — explicitly required; the
   design forbids any rename/repackage step.
6. **May the option include a path (`https://host/zotero`)?** *(confirmed: no in v1)* —
   the API reaches Retrofit's `baseUrl`, which requires a trailing slash for a non-empty
   path; a self-hosted server is normally mounted at the origin, so v1 rejects a path with
   a clear message and leaves sub-path mounting to a later change.
7. **Also rewrite the cosmetic zotero.org links** (registration page, settings/account
   links, citation links, styles/update hosts)? *(confirmed: no in v1)* — they are
   reachable over the internet and do not affect sync; leaving them keeps the patch
   minimal and avoids claiming behaviour we cannot verify.

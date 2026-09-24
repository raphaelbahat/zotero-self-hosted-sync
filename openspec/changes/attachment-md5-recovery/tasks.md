## 1. Evidence

- [x] 1.1 Capture the client's own trace and confirm the failing request: `400` from `POST /users/1/items/Z5B8C2J9/file` with a 16-byte body, `authorizationFailed(statusCode=400, response=md5 not provided)`, and the client's own parameter line `key=Z5B8C2J9;oldMd5=null;md5=;filesize=208429;mtime=1790273325176` showing the digest it sent was empty.
- [x] 1.2 Read the server's rule at the version under test (`altero/services/storage.py:221`): an empty field is reported as `md5 not provided` (`not form.get(name)` is true for `""`), and a present value must be a 32-character hex digest. The server is correct; the client sends an empty field.
- [x] 1.3 Locate the guard in the pinned artifact: `classes7/…/ReadAttachmentUploadsDbRequest.smali:505` — `invoke-static {v3, v6}, Lkotlin/jvm/internal/Intrinsics;->areEqual(...)Z` with `const-string v6, "null"` on the line above, followed by `if-eqz v3, :cond_134` and the reader's own repair (`FileStore.md5` → `RItemField.setValue`).
- [x] 1.4 Confirm the constant is **shared**: `grep` finds one `const-string "null"` in the class, reused by the `backendMd5 == "null"` comparison at `:541`, which is correct and must not change. Only the first comparison may be replaced.
- [x] 1.5 Confirm the site is the only one that can matter: `AttachmentUpload` (the object carrying `md5` into `AuthorizeUploadSyncAction`) is constructed in this reader and nowhere else; the share/background uploader computes its digest at creation time.

## 2. Patch and extension

- [x] 2.1 Add the extension helper: a JDK-only `AttachmentMd5.unusable(String, File)` that answers the protocol's question (is this a 32-character hex digest, and is there a file to recompute it from).
- [x] 2.2 Add the patch: replace that one comparison inside `ReadAttachmentUploadsDbRequest` with the helper (taking the value register and the file register the existing repair uses), fail unless exactly one such comparison is found, and declare `extendWith` for the extension.
- [x] 2.3 Confirm the replacement is scoped: the shared `"null"` constant and the `backendMd5` comparison are left in place, and no other class is visited.

## 3. Build and artifact verification

- [ ] 3.1 Build with `./gradlew buildAndroid`; list the bundle and confirm the new patch appears.
- [ ] 3.2 Patch the pinned APK with the patch selected; confirm in the artifact that the guard now invokes the extension helper and that the helper class is present in the merged DEX.
- [ ] 3.3 Confirm the artifact's second comparison (`backendMd5`) and every other class are unchanged, and that the reader holds no remaining `Intrinsics.areEqual` against the shared constant at the patched site.

## 4. Device verification (needs the device)

- [ ] 4.1 Install and sync: confirm the client logs a 32-character digest for the attachment, that the authorize request answers `2xx` instead of `400 md5 not provided`, and that the file bytes are uploaded.
- [ ] 4.2 Confirm the attachment registers on the server (`MarkAttachmentUploadedDbRequest`) and appears on the desktop client, and that a later sync neither resends an empty digest nor logs the failure.

## 5. Hygiene

- [ ] 5.1 Run `openspec validate attachment-md5-recovery --strict` and fix anything it reports.

# 🧩 Zotero self-hosted sync

Morphe patches that point the **Zotero Android app** at a **self-hosted Zotero sync server** (such as [altero](https://altero.run/)) instead of `zotero.org`.

## ❓ About

A single patch that rewrites the Zotero Android client's hardcoded endpoints to the server domain you enter when patching.

The Zotero Android application compiles its API host into the build and exposes no runtime setting for another server, so a self-hosted server can only be used with a patched client. This bundle supplies that patch:

- **Custom sync server** — at patch time you enter your server address (e.g. `https://zotero.example.org`). The patch rewrites the API base (`https://api.zotero.org`) and the streaming endpoint (`wss://stream.zotero.org`) accordingly, and uses `wss://<host>/stream` for live updates.
- **Nothing else to configure** — the login/approval flow, the account-approval URL and attachment upload URLs are all server-provided, so they follow the domain automatically. HTTPS is required.

> The server itself must implement the Zotero Web API and streaming API (e.g. [altero](https://github.com/eseifert/altero)). Only the official Android app is patched; the server is not touched.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=raphaelbahat/zotero-self-hosted-sync

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.3](https://github.com/raphaelbahat/zotero-self-hosted-sync/releases/tag/v1.0.0-dev.3)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;7 patches total
<details open>
<summary>📦 Zotero&nbsp;&nbsp;•&nbsp;&nbsp;7 patches</summary>
<br>

**🎯 Supported versions:**

| 1.0.0-247 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Accept upload authorization that carries no upload form](#accept-upload-authorization-that-carries-no-upload-form) | Uploads the file when the sync server authorizes an upload without an S3 form envelope. Without this, an authorization the server completed is thrown away as a parse error and the attachment never uploads. |  |
| [Custom sync server](#custom-sync-server) | Redirects Zotero's sync API and live-update stream to your own server. | • Server address<br>• Streaming address (optional) |
| [Custom sync server: allow cleartext streaming](#custom-sync-server-allow-cleartext-streaming) | Adds the chosen streaming host to the app's cleartext allow-list when the stream URL is ws:// or http://. | • Server address<br>• Streaming address (optional) |
| [Enable verbose logging](#enable-verbose-logging) | Plants Timber's debug tree so the app's own log lines reach logcat. Diagnostics only — expect a lot of output. |  |
| [Recover attachments with an unusable MD5](#recover-attachments-with-an-unusable-md5) | Uploads attachments whose stored MD5 is empty or malformed, instead of sending an unusable digest the server rejects. Without this, the attachment's file never uploads and never reaches other devices. |  |
| [Recover attachments with an unusable modification time](#recover-attachments-with-an-unusable-modification-time) | Uploads attachments whose stored modification time cannot be parsed, instead of skipping them forever. Without this, an attachment whose mtime is empty never uploads its file. |  |
| [Send attachment uploads as the file itself](#send-attachment-uploads-as-the-file-itself) | Sends an attachment upload without the multipart form around it when the server authorized the upload without an S3 form. Workaround for servers that take the bytes themselves; uploads that carry a form are unaffected. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

Anondev Zotero Patches are licensed under the [GNU General Public License v3.0](LICENSE)

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
> **[v1.0.0-dev.1](https://github.com/raphaelbahat/zotero-self-hosted-sync/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
<details open>
<summary>📦 Zotero&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 1.0.0-247 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Custom sync server](#custom-sync-server) | Redirects Zotero's sync API and live-update stream to your own server. | • Server address<br>• Streaming address (optional) |
| [Custom sync server: allow cleartext streaming](#custom-sync-server-allow-cleartext-streaming) | Adds the chosen streaming host to the app's cleartext allow-list when the stream URL is ws:// or http://. | • Server address<br>• Streaming address (optional) |

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

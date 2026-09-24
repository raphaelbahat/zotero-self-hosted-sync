# ADR Review Manifest

- Status: completed
- Review date: 2026-09-24

## Review Summary

ADR review completed for this change. `<repo>/adr/` held no in-force ADRs before this
change, so the design was constrained only by the decisions recorded here. Two durable
decisions were identified and recorded and are `Accepted`: the operator confirmed the four
remaining grilling items (single option, patch-time rewrite, path rejection, cosmetic links).

## In-Force ADRs Reviewed

- None - `<repo>/adr/` had no in-force ADRs before this change.

## New Durable ADRs Created

- `adr/0001-patch-time-static-endpoint-rewrite.md` - endpoints are rewritten while patching
  rather than by code that runs inside the app.
- `adr/0002-preserve-target-package-identity.md` - the patched app keeps
  `org.zotero.android`; no rename, repackage or re-namespace step is permitted.

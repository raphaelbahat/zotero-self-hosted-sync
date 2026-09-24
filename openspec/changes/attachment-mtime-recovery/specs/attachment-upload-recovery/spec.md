## ADDED Requirements

### Requirement: Upload recovery for unusable attachment metadata

Feature: Attachment upload recovery
Rule: An attachment whose stored modification time cannot be parsed is still uploaded.

The patch MUST substitute a usable modification time instead of discarding the attachment, and MUST NOT change the request, the stored data or the behaviour for an attachment whose value is already usable.

#### Scenario: An empty modification time no longer blocks the upload

- **GIVEN** an attachment whose stored `mtime` field is empty or otherwise not a number
- **WHEN** the client collects the attachments it needs to upload
- **THEN** that attachment is queued and its file is uploaded, instead of being skipped with a log line

#### Scenario: A usable value is used unchanged

- **GIVEN** an attachment whose stored `mtime` is already a number
- **WHEN** the client collects the attachments it needs to upload
- **THEN** the stored value is used exactly as before

#### Scenario: The field repairs itself after the upload

- **GIVEN** an attachment uploaded through this recovery
- **WHEN** the upload completes
- **THEN** the client stores the file's own modification time on the attachment, so later syncs no longer depend on the substitute

#### Scenario: Only the upload reader changes

- **GIVEN** the patched application
- **WHEN** any other code path parses a string as a number
- **THEN** that path is unchanged — the substitution applies only to the attachment upload reader

## ADDED Requirements

### Requirement: Endpoint override

Feature: Custom sync endpoint
Rule: A patched client uses the operator's self-hosted server for synchronisation, and only that server, while keeping its own identity.

The patched application MUST direct every synchronisation request to the configured origin, and MUST NOT send any synchronisation request to the original hosts.

#### Scenario: Library synchronisation reaches the chosen server

- **GIVEN** the Zotero Android 1.0.0 (build 247) APK patched with the server address `https://zotero.example.org`
- **WHEN** the patched app synchronises a library
- **THEN** every metadata request goes to `zotero.example.org` and none goes to `api.zotero.org`

#### Scenario: Live updates use the chosen server's stream

- **GIVEN** the patched app is signed in to the operator's server
- **WHEN** the app opens its live-update connection
- **THEN** the connection is made to `wss://zotero.example.org/stream`

#### Scenario: Account linking opens the server's own approval page

- **GIVEN** the patched app on its sign-in screen, with no account linked yet
- **WHEN** the user starts linking an account
- **THEN** the page opened is the one the self-hosted server returned for this session, not a zotero.org page
- **AND** linking completes with the app holding a key issued by that server

#### Scenario: Attachment transfer follows the server

- **GIVEN** the patched app synchronises an attachment
- **WHEN** it uploads or downloads the file
- **THEN** the transfer happens against the address the self-hosted server authorised

### Requirement: Patch input contract

Feature: Custom sync endpoint
Rule: The patch accepts exactly one server address and refuses anything it cannot honour, before an APK is produced.

The patch MUST accept exactly one origin and MUST refuse, while patching, any value it cannot honour.

#### Scenario: Bare host is normalised

- **GIVEN** the option set to `zotero.example.org`
- **WHEN** patching runs
- **THEN** the address is treated as `https://zotero.example.org` and patching succeeds

#### Scenario: Trailing slash is accepted

- **GIVEN** the option set to `https://zotero.example.org/`
- **WHEN** patching runs
- **THEN** the trailing slash is removed and patching succeeds

#### Scenario: Non-HTTPS address is refused

- **GIVEN** the option set to `http://zotero.example.org`
- **WHEN** patching runs
- **THEN** patching stops with a message that an HTTPS address is required
- **AND** no patched APK is produced

#### Scenario: Address carrying a path is refused

- **GIVEN** the option set to `https://zotero.example.org/zotero`
- **WHEN** patching runs
- **THEN** patching stops with a message that the address must be an origin without a path

#### Scenario: Empty address is refused

- **GIVEN** the patch selected but its option left empty
- **WHEN** patching runs
- **THEN** patching stops with a message naming the option

### Requirement: Application identity preserved

Feature: Custom sync endpoint
Rule: Patching changes endpoint strings only; the application remains the same application.

The patched application MUST keep its original package name, application id and version code, and MUST NOT have any class renamed, removed or repackaged.

#### Scenario: Patched APK keeps the original package name and version code

- **GIVEN** the patched APK produced from the original 1.0.0 (build 247) APK
- **WHEN** its manifest is read
- **THEN** the package name is still `org.zotero.android` and the version code is unchanged

#### Scenario: No application class is renamed

- **GIVEN** the patched APK
- **WHEN** its classes are compared with the original's
- **THEN** no application or library class has been renamed, removed or repackaged
- **AND** only string constants differ

### Requirement: Scope of the rewrite

Feature: Custom sync endpoint
Rule: The patch changes the synchronisation endpoints and nothing else.

The patch MUST change only the two synchronisation endpoints and MUST leave every other host untouched.

#### Scenario: Non-sync zotero.org endpoints are left alone

- **GIVEN** the patched app
- **WHEN** it fetches citation styles, translators or an update check
- **THEN** those requests still go to the zotero.org hosts as before

#### Scenario: Unpatched behaviour is unchanged

- **GIVEN** the original APK with the patch not applied
- **WHEN** the app synchronises
- **THEN** it uses the zotero.org endpoints exactly as the store build does

# Design: Opt-in system contacts mirror

Status: steps 1 (Caller ID level, global toggle) and 2 (per-book sharing) are
implemented and awaiting device testing. Steps 3 to 5 are not started.

## Goal

Let other apps on the device (Messages, the dialer, any app with the contacts
permission) show a contact's name and photo for a phone number, without
syncing anything through a Google account.

Corvid's premise is that the default experience has no contact with Google
beyond what Android makes unavoidable. Users then choose, explicitly, how much
privacy to trade for convenience. This feature follows that model:

- The most private option is always available. With the feature off, the app
  behaves exactly as it does today.
- Every step up in sharing is a separate, explicit choice with plain-language
  text about what it exposes.

## Non-goals

- Two-way sync. The system provider is a read-only mirror.
- Account-less ("local device") contacts. Google Contacts can back these up to
  the user's Google account, which defeats the purpose.
- Reading the user's other contacts. The app never needs to (see Permissions).
- Changing how CardDAV/Nextcloud sync works.

## Current state

- Contacts live in Room (`ContactEntity`, `AddressBookEntity`). Multi-value
  fields (phones, emails, and so on) are stored as JSON via `Converters.kt`.
- With a server account, the CardDAV server is the source of truth and Room is
  a cache. Local-only mode and `local://` books use Room as the source of truth.
- The app does not read or write `ContactsContract`. It only parses incoming
  insert intents (`utils/IntentParser.kt`). The manifest has no contacts
  permissions, services, account type, or `contacts.xml`.
- The manifest's `EDIT` intent filter only matches vCard MIME types, so it
  does not receive edits launched from the system Contacts app.
- `AddressBookEntity.isVisible` is a temporary view filter (show one book or
  all). It must not drive what is shared.
- The Room database has no migration code, so the new columns below need a
  schema-change plan.
- minSdk is 28.

## Design

### Model

Room stays the source of truth. A custom account type ("Corvid Contacts") owns
the mirrored rows in `ContactsContract`. Data flows one way, Room to provider.
Nothing flows back.

### Permissions: write-only

The app requests `WRITE_CONTACTS` only, not `READ_CONTACTS`.

- Inserts return the new row's URI from `applyBatch`. The app stores the
  raw-contact id and a hash of what it last wrote in Room, and uses those for
  later updates and deletes.
- If a row was removed behind the app's back, an update or delete affects 0
  rows, which `applyBatch` reports. The app then re-inserts.
- A full reset is a single delete of every row for the account, followed by
  re-inserting. No read is needed.
- Benefits: the app cannot read the user's other contacts, the permission
  declaration to Google Play is simpler, and the permission story to users is
  stronger.
- Cost: a mapping in Room (see Field mapping), and therefore a schema change.

To verify on a device: whether a sync adapter can read its own account's rows
without `READ_CONTACTS` (useful as a fallback, not required), and how the
runtime permission prompt is worded when only write access is requested. If the
write-only approach turns out not to work reliably, fall back to requesting
`READ_CONTACTS` as well and reading the account's rows to diff.

### Sharing choices

1. **Feature toggle** (global, default off). Turning it on requests
   `WRITE_CONTACTS`. Turning it off deletes all mirrored rows.
2. **Which address books to share** (per book, default off). This is the
   `AddressBookEntity.shareWithSystem` column, added with a 20 to 21 migration. It
   is separate from `isVisible`. With the feature on and no book shared, the
   mirror is empty. The Settings section lists each book with its own switch once
   the feature is on and the permission is held. Sync never resets the flag
   (address book inserts ignore existing rows), but a book that is removed and
   re-synced starts private again, which is the safe direction.
3. **How much data to share** (global, one of three presets):
   - *Caller ID* (default): name, phone numbers, photo.
   - *Full contact*: also emails, addresses, social profiles, websites,
     birthday, groups, organization, job title, nickname, relationships.
   - *Everything*: also notes.

Notes are a separate level because they are free text where people commonly
keep sensitive details. The levels are fixed presets. The existing share-contact
screen, which has per-field selection, is not reused. It works from a populated
contact entity and only shows fields that have data, and presets don't need it.

Listing mirrored contacts in the Contacts app is acceptable and needs no
setting.

### Account plumbing

- A stub `AccountAuthenticator` service with no login UI. The account only
  owns the rows.
- A `SyncAdapter` service, so the account type is registered with the system.
- `res/xml/contacts.xml` declaring the account type as a contacts source, and
  the matching authenticator and sync adapter XML.
- Manifest entries and the `WRITE_CONTACTS` permission.
- The account type string comes from a `resValue` in `build.gradle.kts`, with a
  separate value for debug builds so a debug and a release install can coexist.
- The authenticator refuses "add account" from Android's account settings. The
  account is created and removed only by the app.
- The account's contacts-list visibility setting (`UNGROUPED_VISIBLE`) is set when
  the account is created, otherwise contacts without a group are hidden from the
  Contacts app.
- Handle the user removing the account in Android settings: the system deletes
  the mirrored rows. If the account is missing at reconcile time, the stored
  mapping is treated as stale (this also covers a backup restored onto a new
  device) and cleared, then the account is created again.

### Field mapping

- Contact to `RawContacts` and `Data` rows via one `applyBatch` call per batch.
- Mapping storage: for each mirrored contact, Room keeps the provider
  raw-contact id and a content hash in a separate `system_contact_mirror` table
  (`SystemContactMirrorEntity`). It is not a column on `ContactEntity` because
  server sync replaces contact rows wholesale, which would wipe it. The hash
  covers the fields at the current sharing level, so changing the level also
  causes updates. The table was added with a 19 to 20 Room migration.
- Who is mirrored: a contact with no name or no phone number is not mirrored,
  since it is no use for caller ID. Archived contacts are not mirrored either.
  A contact that stops qualifying is deleted from the mirror on the next
  reconcile.
- Map phones to `Phone`, emails to `Email`, addresses to `StructuredPostal`,
  and so on. Fields with no provider equivalent are skipped.
- Photos: write the local photo file bytes to the `Photo` data row.
- Each shared address book is one provider group (title = book name), so books
  remain distinguishable in the Contacts app. Contacts get a `GroupMembership`
  row for their book's group. The book-to-group mapping lives in a
  `system_group_mirror` table (`SystemGroupMirrorEntity`), and the group id is part
  of each contact's hash, so a recreated group updates its contacts. Renaming a
  book renames its group, and un-sharing a book deletes its group and its
  contacts. Contact groups (vCard categories) map to provider groups at the Full
  contact level, which is not built yet.

### Diff and reconcile

- Compare the Room contacts in shared books, and their hashes, against the
  stored mapping, and apply only inserts, updates and deletes. Rows with an
  unchanged hash are not touched.
- Triggers: after `syncContacts()`, after local saves and deletes, after any
  sharing setting changes, and as a full reconcile from `SyncWorker`.
- A full reset (delete all rows for the account and re-insert) is the recovery
  path if the mapping and the provider disagree.
- Implementation: `SystemContactsMirrorManager` watches the setting and a
  Room query of mirror-relevant columns, debounced by 2 seconds, from an
  application-scoped coroutine. This covers server sync, local edits, imports,
  archiving and deletes without hooking each one. Turning the setting off removes
  the account, which deletes every mirrored row, and clears the mapping.
- An update first touches the raw contact with an expected count of 1, so a row
  that has vanished fails the batch instead of attaching data to a stale id. The
  contact is then re-inserted.
- Photos are written as their own small batch, since photo bytes can approach
  the binder transaction limit. Photos over 256 KB are downscaled to at most
  about 720 px. A failed photo write leaves the contact without a photo and does
  not fail the reconcile.

### Read-only rows

- Mark mirrored rows read-only using `RawContacts.RAW_CONTACT_IS_READ_ONLY`
  (a public constant in the SDK; still to be confirmed on a device). It is
  set on insert. If the provider rejects it, the insert is retried once without
  the flag, so the mirror still works but is editable.
- Stock and Google Contacts honor the flag by hiding edit and delete.
- It is not a security boundary. Any app with `WRITE_CONTACTS` can change the
  rows, and the next reconcile overwrites them. The user is told the mirror is
  read-only and edits happen in Corvid.

### Routing edits to Corvid

The system Contacts app's edit action can be routed to Corvid rather than being
hidden or ignored. This is a moderate amount of work, not a hard problem. The
edit action is declared by the account type's `contacts.xml` (an edit activity
for the account), not by the existing vCard `EDIT` filter.

- Add an activity, or a filter on `MainActivity`, that handles the edit intent
  for the account. It receives a provider URI.
- Resolve the URI's raw-contact id to a Room contact using the stored mapping
  (a reverse lookup), then open the existing edit screen for that contact.
- If the row can't be resolved (for example the mapping is stale), show a
  message and offer to run a reset.

This makes the read-only flag the fallback. Both should be tested, since the
Contacts app's behavior when an edit activity is declared may vary. It is the
last build step because it depends on the mapping and does not block caller ID.

### Privacy and Google

- The mirror is on-device. Nothing is sent to Google.
- Google Contacts' device-contact backup targets account-less contacts, which
  is why a custom account type is required.
- A user can still explicitly copy or move a contact into a Google account
  from another app. The app cannot prevent that, and the settings text should
  say so.
- Once mirrored, the contacts are readable by any app the user has granted the
  contacts permission. The settings text must say this plainly.
- `PRIVACY_POLICY.md` currently says data at rest is in app-private storage
  that other apps cannot access. This needs an update covering the opt-in
  mirror, and the Play Console permissions declaration needs updating.

## Settings and copy

A "System contacts" section:

- Master toggle with the explanation above.
- Per-book toggles (only enabled when the master toggle is on).
- Data-level picker with the three presets, and one line under each stating
  exactly what it exposes.
- A statement that the mirror is read-only and never synced to Google by Corvid.

## Testing

Unit tests, in the style of `ContactsRepositoryVCardRoundTripTest`:

- The mapper at each of the three levels.
- The diff: insert, update, delete, rename, book toggled on and off, level
  changed, feature turned off, and a row missing from the provider.

Manual checks on a real device:

- Messages and the dialer resolve names (and photos) for mirrored numbers.
- Mirrored contacts appear in the Contacts app as read-only, or route to Corvid
  for editing.
- Nothing appears in the Google account's contacts, and Google's device-contact
  backup does not pick them up.
- Removing the account or disabling a book or the feature cleans up the rows.
- Behavior with a Google account signed in, and without one.
- The write-only path works without `READ_CONTACTS`.

## Open questions

- Confirm on a device that the read-only flag is accepted at insert time and that
  the stock Contacts app honors it on current Android versions.
- Confirm that write-only works end to end (see Permissions), including that the
  `Settings` row insert, the sync-adapter-flagged inserts and deletes, and
  `removeAccountExplicitly` all work with only `WRITE_CONTACTS`.
- Confirm that Messages and the dialer resolve names and photos, and that nothing
  appears in the Google account.
- Confirm how the Contacts app handles an edit activity declared in
  `contacts.xml`.
- How many contacts before batching or performance needs tuning?
- Switching to local-only mode clears server books and their contacts from Room,
  and the mirror follows on the next reconcile. Confirm that on a device.

## Suggested order

1. Account plumbing, mapping table, mapper and diff behind the global toggle, at
   the Caller ID level. (Implemented; awaiting device testing.)
2. Per-book sharing. (Implemented; awaiting device testing.)
3. The Full contact and Everything levels.
4. Routing edits to Corvid.
5. Privacy policy, settings copy, and Play declaration.

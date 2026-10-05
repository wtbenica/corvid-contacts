# Design: Opt-in system contacts mirror

Status: steps 1 (Caller ID level), 2 (per-book sharing) and 3 (the three sharing
levels) are implemented; the handoff lists what has actually been verified on a
device. Sharing is decided per
address book, both whether and how much; there is no global toggle or level.
Step 4 (routing edits to Corvid) was removed in favor of reading edits back (two-way sync, see the handoff), the onboarding sharing step is built, and step 5 is not started. How and when to offer sharing during onboarding is
still to be decided (see the handoff's open questions).

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

- Creating contacts in other apps under Corvid's account. Google Contacts only creates in its own
  accounts, so a contact created there is an unlikely edge case.
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

There is no master switch. The mirror exists exactly while at least one address
book is shared, and unsharing the last one removes the account and every
mirrored row. The first time a book is shared without `WRITE_CONTACTS` granted,
a short explanation of what sharing does comes before the system permission
prompt.

1. **Which address books to share** (per book, default off). This is the
   `AddressBookEntity.shareWithSystem` column, added with a 20 to 21 migration. It
   is separate from `isVisible`. The switch lives on each address book's own
   settings page (see "Address book pages" below), not in a list in Settings.
   Sync never resets the flag (address book inserts ignore existing rows), but a
   book that is removed and re-synced starts private again, which is the safe
   direction.
2. **How much data to share** (per book, one of three presets):
   - *Caller ID* (default): name, phone numbers, photo.
   - *Full contact*: also emails, addresses, social profiles, websites,
     birthday, groups, organization, job title, nickname, relationships.
   - *Everything*: also notes.

   The level is `SystemContactsLevel`, stored in
   `AddressBookEntity.systemContactsLevel` (added with a 22 to 23 migration) and
   defaulting to Caller ID. It is kept when sharing is switched off, so switching
   back on restores the previous choice. Changing it rewrites that book's mirrored
   contacts, because the level is part of each contact's hash. Different books can
   be at different levels: a work book at Caller ID, a family book at Full contact.

   Level was a single global setting before 22 to 23. It was made per book because
   how sensitive the contacts are differs by book, and because a global level plus
   a per-book switch split one decision across two screens.

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
  covers the fields at the book's current sharing level, so changing the level
  also causes updates. The table was added with a 19 to 20 Room migration.
- Who is mirrored: a contact with no name or no phone number is not mirrored,
  since it is no use for caller ID. Archived contacts are not mirrored either.
  A contact that stops qualifying is deleted from the mirror on the next
  reconcile.
- Map phones to `Phone`, emails to `Email`, addresses to `StructuredPostal`,
  and so on. Fields with no provider equivalent are skipped.
- Photos: write the local photo file bytes to the `Photo` data row.
- Each shared address book is one provider group (title = book name), so books
  remain distinguishable in the Contacts app. Contacts get a `GroupMembership`
  row for their book's group. The group mapping lives in a
  `system_group_mirror` table (`SystemGroupMirrorEntity`) keyed by a group key
  (`book:<href>` or `category:<name>`), and the group ids are part of each
  contact's hash, so a recreated group updates its contacts. Renaming a
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

### Edits made in other apps

Other apps can edit and delete the mirrored contacts, and Google Contacts ignores the read-only
flag and the edit-activity hook, so neither is used (both were removed). Instead the mirror is read
back, which needs `READ_CONTACTS` as well as `WRITE_CONTACTS`; see the handoff's "Two-way sync"
section for the design. In short: a reconcile first reads the account's dirty and deleted raw
contacts, merges an edit into the Corvid contact (Corvid wins if both sides changed the same part),
and treats a delete as "hide this contact from the system contacts", kept in a device-local table,
without deleting it from Corvid or the server.

### Privacy and Google

- The mirror is on-device. Nothing is sent to Google.
- Google Contacts' device-contact backup targets account-less contacts, which
  is why a custom account type is required.
- A user can still explicitly copy or move a contact into a Google account
  from another app. The app cannot prevent that, and the policy and settings text say so.
- Once mirrored, the contacts are readable by any app the user has granted the
  contacts permission. The settings text and the privacy policy say this plainly.
- The privacy policy, the data deletion page, the in-app strings, the store listing and the release
  notes were updated for the mirror and for reading edits back (2026-10-05). The website changes are
  on a branch until the rollout. See `play-console-1.0.5.md` for the Play side: the new
  `READ_CONTACTS` declaration (the app targets API 37, so it applies) and the Data safety reasoning.

## Field mapping by level

| Provider row | Caller ID | Full contact | Everything |
| --- | --- | --- | --- |
| Name (display, given, family) | yes | yes | yes |
| Phone numbers | yes | yes | yes |
| Photo | yes | yes | yes |
| Name parts (middle, prefix, suffix) | no | yes | yes |
| Emails, postal addresses, websites | no | yes | yes |
| Social profiles (website links of type profile) | no | yes | yes |
| Birthday | no | yes | yes |
| Company and job title | no | yes | yes |
| Nickname | no | yes | yes |
| Relationships | no | yes | yes |
| Starred (favorites) | yes | yes | yes |
| Category groups (except Favorites and Archived) | no | yes | yes |
| Notes | no | no | yes |

Details: relationships stored as a contact UID are left out because they have no
name to show, and only birthdays in `yyyy-MM-dd` or `--MM-dd` form are written.
Phone, email and postal types have separate numbering in the provider, so each
has its own mapping.

## Address book pages

Per-book sharing is part of a dedicated address book settings page rather than
a dialog or a list in Settings, because sharing is a property of a book like its
color or visibility.

- The contact list's filter sheet has a manage button that opens an address book
  list (`Destination.AddressBooks`): reorderable, with an add button and a status
  line per book (local, hidden, shared and at which level). Tapping a book opens its page
  (`Destination.AddressBookSettings(href)`). Settings has a "Manage address books"
  row that opens the same list, so the filter sheet stays the primary path and
  Settings is the secondary one.
- A book's page holds its icon and color, name, visibility, sharing with other
  apps (the switch, and the level picker once it is on), uploading a local book to a server, and deleting it. The existing dialogs
  are reused from the page. The old manage-books dialog and the dialogs stacked
  in the filter sheet are gone; the filter sheet keeps filtering and the add
  button. The widget picker has no address book list, so the manage button is
  hidden there.
- The switch reads as off while `WRITE_CONTACTS` is not held, so revoking it in
  system settings is reflected rather than hidden. Turning it on asks again.
- The wide-screen layout has a case for both destinations.

## Onboarding

A dedicated step after the sync wait and the local-data step: one switch per address book, Continue
as the only exit, nothing preselected on a first run. Continue shares the books switched on and
unshares the ones switched off. The contacts permission is requested only when a book is newly shared
and, if denied, the step stays with the books off and a note, so a book is never left quietly
unshared. Once Android stops offering the prompt, the note links to the app's settings. The level
stays at Caller ID. Losing the permission later removes the mirror and keeps the flags, so it returns
when the permission does.

## Settings and copy

Everything is set per book, on the book's page. Settings has no controls for it,
only a row under Address Books ("Show names in other apps") that opens the address
book list and summarizes how many books are shared, so the feature can be found
without being a second place to change it.

- The book page's level picker has the three presets with one line under each
  stating exactly what it exposes.
- The first-share explanation states that the mirror is readable by any app with
  contacts access, can be edited from other apps (edits come back to Corvid, and deleting one there only hides it), and is never synced to Google by Corvid.

## Testing

Unit tests, in the style of `ContactsRepositoryVCardRoundTripTest`:

- The mapper at each of the three levels.
- The diff: insert, update, delete, rename, book toggled on and off, level
  changed, last book unshared, and a row missing from the provider.
- Per-book levels: contacts in two books at different levels in one pass, and a
  book with no recorded level falling back to Caller ID.

Manual checks on a real device:

- Messages and the dialer resolve names (and photos) for mirrored numbers.
- The address book list and each book's page work from the filter sheet and from
  Settings, on a phone and on a wide screen, including rename, hide, upload (local
  books) and delete, and the sharing switch on a book's page.
- Editing a mirrored contact in the Contacts app is read back into Corvid, and deleting one hides it
  from the system contacts without deleting it from Corvid.
- Nothing appears in the Google account's contacts, and Google's device-contact
  backup does not pick them up.
- Removing the account or disabling a book or the feature cleans up the rows.
- Behavior with a Google account signed in, and without one.
- The write-only path works without `READ_CONTACTS`.

## Open questions

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

1. Account plumbing, mapping table, mapper and diff, at the Caller ID level.
   (Implemented.)
2. Per-book sharing. (Implemented.)
3. The Full contact and Everything levels, chosen per book. (Implemented.)
4. Routing edits to Corvid. (Built, then removed: Google Contacts ignores it. Replaced by two-way sync.)
5. Privacy policy, settings copy, and Play declaration.

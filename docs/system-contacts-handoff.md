# Handoff: system contacts mirror

Written 2026-10-03. Branch: `system-contacts-design` (pushed to `origin`, branched from
`1.0.5`, so it also contains that branch's commits). The full design is in
[system-contacts-design.md](system-contacts-design.md); this file is the short version
plus what is left.

## Goal

Let other apps on the device (Messages, the dialer) show a contact's name and photo for a
phone number, without syncing anything through a Google account.

Corvid's premise is that the default experience has no contact with Google beyond what
Android makes unavoidable, and users then choose, explicitly, how much privacy to trade for
convenience. So:

- The most private option is always available. With the feature off, the app behaves as it
  did before this work.
- Every step up in sharing is a separate, explicit choice with plain-language text.
- No two-way sync, and no account-less ("local device") contacts, which Google Contacts can
  back up to the user's Google account.

## How it works

- Room stays the source of truth. A custom account type ("Corvid Contacts") owns the
  mirrored rows in `ContactsContract`. Data flows one way, Room to provider.
- The app holds only `WRITE_CONTACTS`, never `READ_CONTACTS`. The raw-contact ids returned
  by inserts are stored in Room (`system_contact_mirror`, `system_group_mirror`) and used for
  later updates and deletes. A mirrored row that has vanished is detected when an update
  affects nothing, and re-inserted.
- Sharing is opt-in per address book, with no global switch: a flag
  (`AddressBookEntity.shareWithSystem`, default off) and a level
  (`AddressBookEntity.systemContactsLevel`: Caller ID by default, Full contact, Everything).
  The first share without the permission shows an explanation, then asks for `WRITE_CONTACTS`.
- `SystemContactsMirrorManager` watches the shared books and Room (debounced 2 s) from an
  application-scoped coroutine, so sync, edits, imports, archiving, deletes and sharing
  changes are all covered without hooking each one. Unsharing the last book removes the
  account, which deletes every mirrored row.
- Each shared book becomes a provider group. At Full contact and up, contact categories
  become groups too.

Key files (all under `app/src/main/java/dev/benica/corvidcontacts/`):

| File | Role |
| --- | --- |
| `data/system/MirrorPlan.kt` | Pure mapper, hash and diff. No Android dependencies. |
| `data/system/SystemContactsMirror.kt` | Provider writes (batches, groups, photos). |
| `data/system/SystemContactsMirrorManager.kt` | Observes setting and Room, triggers reconcile. |
| `data/system/SystemContactsAuthenticatorService.kt`, `SystemContactsSyncService.kt` | Account plumbing (stub authenticator, no-op sync adapter). |
| `data/local/SystemContactMirrorDao.kt`, `SystemContactMirrorEntity.kt`, `SystemGroupMirrorEntity.kt` | Mapping tables. |
| `data/local/AppDatabase.kt` | Version 23, migrations 19 to 23. |
| `data/model/SystemContactsLevel.kt` | The three levels. |
| `ui/addressbooks/AddressBooksScreen.kt`, `AddressBookSettingsScreen.kt` | List and per-book pages. The per-book page holds the sharing switch and level picker. |
| `ui/settings/sections/AddressBooksLinkSection.kt` | Settings rows that open the book list, with a read-only sharing summary. |

## What is done

Commits on the branch, oldest first:

1. `3586547` Design doc.
2. `3866374` Skip Places SDK init when no API key is configured (see Environment notes).
3. `88f407c` **Step 1.** Account plumbing, mapping table, mapper and diff, master toggle,
   Caller ID level. Migration 19 to 20.
4. `d7a41ee` **Step 2.** Per-book sharing (`shareWithSystem`), groups per shared book.
   Migration 20 to 21. Migration test.
5. `aadae9d` **Step 3.** Full contact and Everything levels, category groups, level picker.
   Migration 21 to 22.
6. `ea3f4a2` **Address book pages.** New address book list and per-book settings page
   (appearance, name, visibility, sharing, upload, delete), opened from the filter sheet's
   manage button and from a new Settings row. The old manage-books dialog and the dialogs
   stacked in the filter sheet were removed, and per-book switches left Settings. Both the
   phone and wide-screen shells handle the new destinations.
7. `a7edcc4` **Per-book level, no master toggle.** The global "Show names in other apps"
   switch and the global level are gone. Each book has its own sharing switch and level on its
   page (`AddressBookEntity.systemContactsLevel`, migration 22 to 23). The mirror exists while
   at least one book is shared. The first share without `WRITE_CONTACTS` shows an explanation,
   then the permission prompt. Settings keeps one read-only summary row that opens the book
   list. Tried on a device and reported working.

Status: steps 1 to 3 and the per-book rework: you tested on a device and reported it working. The 38 unit tests pass, including
migration tests that open version 19, 21 and 22 databases through every migration. The debug build
assembles. Lint has one error left, an existing French plural string
(`values-fr/strings.xml`, `ImpliedQuantity`), unrelated to this work.

A jerky transition when the address book list opens from the filter sheet was seen in
debug builds only and is fine in release, so it was left alone.

## What remains

### Step 4: route the Contacts app's edit action to Corvid (built, needs a device test)

Implemented in the commit after `84a5905`; see "Routing edits to Corvid" in the design doc.
`SystemContactEditActivity` is declared as `editContactActivity` and `createContactActivity`
in `res/xml/system_contacts_structure.xml`. Edit looks up the Corvid contact from the
raw-contact id and opens `cccontacts://contact/<id>?edit=true`; create forwards the standard
insert intent.

What to check on a device:

- Opening a mirrored contact in the stock Contacts app and in Google Contacts, then tapping
  edit: does it open Corvid's edit screen, or does the app ignore `editContactActivity`
  (Android's sample uses it, but current behavior is not confirmed)?
- If it is ignored, does the read-only flag hide the edit button, and what happens with a
  third-party contacts app that respects neither? Such edits are overwritten on the next
  reconcile.
- The Contacts app's "create contact" with the Corvid account selected: does it reach Corvid's
  new-contact screen?
- The Google Contacts app may list the account in an account picker even though it is
  read-only; note what it shows.

### Step 5: privacy policy, settings copy, Play declaration

- `PRIVACY_POLICY.md` currently says data at rest is in app-private storage that other apps
  cannot access. It needs an update covering the opt-in mirror: what is shared at each level,
  that it is readable by any app with the contacts permission, that Corvid does not send it to
  Google, and that a user can still copy a contact into a Google account from another app.
- Review the settings copy in `res/values*/strings.xml` (`system_contacts_*` and
  `address_book_setting_*` keys).
- The Play Console side of `WRITE_CONTACTS`. Being a contacts app is the justification; the app
  never reads the user's other contacts. Check whether Play has a declaration form for contacts
  permissions at all (the forms are mainly for SMS and call log); it may only need an accurate
  listing and the in-app explanation that already comes before the permission prompt.
- Redo the Data safety review. The mirror stays on the device, so it should still not count as
  collected, but other apps reading it is a grey area worth reading Play's definitions for.
- Add a CHANGELOG entry and release note.

### Smaller items

- **Translations.** The de, es, fr, ko and nl strings for this feature, including the per-book
  wording from `a7edcc4`, were written by Claude and need a native-speaker check.
- **Edit routing for the read-only fallback.** Decide what the user sees if a third-party
  contacts app ignores the read-only flag: those edits are overwritten on the next reconcile.
- **Performance.** Unknown how many contacts before batching needs tuning (batches are 50
  contacts for inserts, 200 for deletes).

## Open questions

Some of these may already be answered by your device testing. Confirm which.

1. Is `RAW_CONTACT_IS_READ_ONLY` accepted at insert time, and does the stock Contacts app
   honor it? (If the provider rejects it, the insert is retried once without it.)
2. Does write-only work end to end: the `Settings` row insert, sync-adapter-flagged inserts
   and deletes, and `removeAccountExplicitly`, all with only `WRITE_CONTACTS`?
3. Does Messages resolve names and photos, and does nothing appear in the Google account?
4. How does the Contacts app handle an edit activity declared in `contacts.xml`? (Step 4.)
5. Switching to local-only mode clears server books and their contacts from Room, and the
   mirror should follow on the next reconcile. Confirm on a device.
6. Should notes ever be treated differently from the Everything level? Currently Everything
   is the only level with notes.
7. Onboarding. Onboarding creates a local address book, which could be shared, and a hint
   after the first import or sync was liked. Undecided: whether that hint is part of onboarding
   (a step, or a final prompt) or a dismissible suggestion shown afterwards. Either way it should
   not request the permission before the user has chosen to share a book.
8. Idea, not started: an "Import from device contacts" action. It would suit people leaving
   Google contacts and only needs `READ_CONTACTS` at import time. A live view of non-Corvid
   contacts in the app would need `READ_CONTACTS` permanently and is a bigger decision
   (duplicates with the mirror, read-only handling, what "source of truth" means). Decide
   whether either belongs in the roadmap.

## Lessons from device debugging

- **In-place updates failed silently until this was traced.** Updates and deletes addressed
  rows with the account as query parameters on the sync-adapter URI, and the provider rejected
  the batch with `Invalid token account_name`. Inserts were fine, so sharing, unsharing and
  resharing worked while edits in Corvid never reached the system contacts. Fixed by addressing
  updates and deletes by row id only (`asSyncAdapterByRowId`). Reconcile now logs under the tag
  `SystemContactsMirror` (snapshot, plan sizes, and per-write results, with contact ids but no
  names or numbers), so a failure is visible. To trace on a device:
  `adb logcat -s SystemContactsMirror:V`, and the provider can be inspected with
  `adb shell content query --uri content://com.android.contacts/data ...`.
- **Verified on a device after the fix** (Pixel 9 Pro, Android 17): editing a phone number in
  Corvid, switching a book's level from Full contact to Caller ID (extra data rows and name
  parts removed), renaming a book (the provider group is renamed), adding a contact to a shared
  book (appears within seconds, also in Google Contacts) and deleting it (the raw contact is
  removed). A contact added to an unshared book is, correctly, not mirrored.
- `RAW_CONTACT_IS_READ_ONLY` is not queryable as a column through `adb shell content query` on
  `raw_contacts` or `data`, so whether the flag took effect still has to be judged from the
  Contacts app's behavior.

## Environment notes

- **Places API key.** `local.properties` is gitignored. Without `GOOGLE_PLACES_API_KEY` in it
  the Places SDK used to crash the app at startup and fail 6 Robolectric tests. Initialization
  is now skipped when the key is blank (commit `3866374`), so the app starts. Copy
  `local.properties` from the other computer to get the key, and keep the `sdk.dir` line
  correct for the new machine.
- **Debug and release builds** use different account types (`system_contacts_account_type` is
  set per build type in `app/build.gradle.kts`), so both can be installed side by side.
- **easylauncher "Unsupported image format ... ic_launcher_foreground.webp".** This is a
  Gradle daemon problem, not a bad icon; it also reproduced on the original commit. Running
  `./gradlew --stop` and rebuilding fixed it.
- **Room schema is not exported** (`exportSchema = false`) and there is no fallback
  migration, so every schema change needs a hand-written migration and a case in
  `AppDatabaseMigrationTest`.

## Commands

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:lintDebug
```

To pick up on the other computer:

```bash
git fetch origin && git checkout system-contacts-design
```

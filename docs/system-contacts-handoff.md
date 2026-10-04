# Handoff: system contacts mirror

Last updated 2026-10-03. Branch: `system-contacts-design` (pushed to `origin`, branched from
`1.0.5`, so it also contains that branch's commits). The full design is in
[system-contacts-design.md](system-contacts-design.md); this file is the short version
plus what is left.

## Goal

Let other apps on the device (Messages, the dialer, Google Contacts) show a contact's name and
photo for a phone number, without syncing anything through a Google account.

Corvid's premise is that the default experience has no contact with Google beyond what
Android makes unavoidable, and users then choose, explicitly, how much privacy to trade for
convenience. So:

- The most private option is always available. With nothing shared, the app behaves as it
  did before this work.
- Every step up in sharing is a separate, explicit choice with plain-language text.
- No account-less ("local device") contacts, which Google Contacts can back up to the user's
  Google account.
- One-way for now (see the write-only question below).

## How it works

- Room stays the source of truth. A custom account type ("Corvid Contacts") owns the
  mirrored rows in `ContactsContract`. Data flows one way, Room to provider.
- The app holds only `WRITE_CONTACTS`, never `READ_CONTACTS`. The raw-contact ids returned
  by inserts are stored in Room (`system_contact_mirror`, `system_group_mirror`) and used for
  later updates and deletes. A mirrored row that has vanished is detected when an update
  affects nothing, and re-inserted. Corvid decides whether to write by comparing a hash of what
  it last wrote with the contact in Room; it never sees what other apps change in the provider.
- Sharing is opt-in per address book, with no global switch: a flag
  (`AddressBookEntity.shareWithSystem`, default off) and a level
  (`AddressBookEntity.systemContactsLevel`). The levels are **Caller ID** (name, phones, photo;
  the default), **Full contact** (also emails, addresses, websites, social profiles, birthday,
  company and title, nickname, relationships, name parts, and category groups) and
  **Everything** (also notes, which are deliberately their own tier). The first share without
  the permission shows an explanation, then asks for `WRITE_CONTACTS`.
- `SystemContactsMirrorManager` watches the shared books and Room (debounced 2 s) from an
  application-scoped coroutine, so sync, edits, imports, archiving, deletes and sharing
  changes are all covered without hooking each one. Unsharing the last book removes the
  account, which deletes every mirrored row. Archived contacts and contacts without a name or
  a phone number are not mirrored.
- Each shared book becomes a provider group. At Full contact and up, contact categories
  become groups too, except `Favorites` and `Archived` (compared ignoring case).
- Favorites: a favorite is the `Favorites` category in Corvid and on CardDAV, and reaches the
  system contacts as the standard starred flag (`RawContacts.STARRED`), at every level. It is
  not mirrored as a group.
- The system Contacts app's edit and create actions for the mirrored account are routed to
  Corvid by `SystemContactEditActivity` (step 4, see below).

Key files (all under `app/src/main/java/dev/benica/corvidcontacts/`):

| File | Role |
| --- | --- |
| `data/system/MirrorPlan.kt` | Pure mapper, hash and diff. No Android dependencies. |
| `data/system/SystemContactsMirror.kt` | Provider writes (batches, groups, photos, starred). |
| `data/system/SystemContactsMirrorManager.kt` | Observes Room, triggers reconcile. |
| `data/system/SystemContactsAuthenticatorService.kt`, `SystemContactsSyncService.kt` | Account plumbing (stub authenticator, no-op sync adapter). |
| `data/system/SystemContactEditRouting.kt`, `SystemContactEditActivity.kt` (package root) | Edit and create routing from the Contacts app. |
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
   manage button and from a Settings row. The old manage-books dialog and the dialogs stacked
   in the filter sheet were removed. Both the phone and wide-screen shells handle the new
   destinations.
7. `a7edcc4` **Per-book level, no master toggle.** The global "Show names in other apps" switch
   and the global level are gone. Each book has its own sharing switch and level
   (`AddressBookEntity.systemContactsLevel`, migration 22 to 23). The mirror exists while at
   least one book is shared.
8. `84a5905` Docs for per-book sharing.
9. `9a9065a` **Step 4.** Edit and create routing from the Contacts app (built; see below).
10. `c95f2d5` **Fix.** In-place updates and deletes were rejected by the provider. Plus reconcile
    logging (see Lessons).
11. `c607fad`, `a2c18df` Book page and book list layout: trailing icons and radios like the app
    Settings page, 16 dp inset, level options dimmed and disabled (not hidden) while a book is
    unshared.
12. The commit that carries this file: starred flag for favorites at every level, and
    `Favorites` filtered out of the mirrored groups.

Status: tried on a device (Pixel 9 Pro, Android 17) and working for steps 1 to 3, the
per-book rework, and the post-fix checks listed under "Verified on a device". The 46 unit
tests pass, including migration tests that open version 19, 21 and 22 databases through every
migration. The debug build assembles. Lint has one error left, an existing French plural
string (`values-fr/strings.xml`, `ImpliedQuantity`), unrelated to this work.

A jerky transition when the address book list opens from the filter sheet was seen in debug
builds only and is fine in release, so it was left alone.

## What remains

### Step 4: confirm edit routing on a device

Built and unit-tested (the routing helper and the reverse lookup) but **not yet tried on a
device**. `SystemContactEditActivity` is declared as `editContactActivity` and
`createContactActivity` in `res/xml/system_contacts_structure.xml`. Edit looks up the Corvid
contact from the raw-contact id and opens `cccontacts://contact/<id>?edit=true`; create forwards
the standard insert intent. What to check:

- Opening a mirrored contact in the stock Contacts app and in Google Contacts, then tapping
  edit: does it open Corvid's edit screen, or does the app ignore `editContactActivity`
  (Android's sample uses it, but current behavior is not confirmed)?
- If it is ignored, does the read-only flag hide the edit button, and what happens with a
  third-party contacts app that respects neither? Such edits are overwritten the next time that
  contact changes in Corvid.
- The Contacts app's "create contact" with the Corvid account selected: does it reach Corvid's
  new-contact screen?
- Whether Google Contacts lists the account in an account picker even though it is read-only.

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
  wording and `system_contact_edit_not_found`, were written by Claude and need a native-speaker
  check.
- **Unshared books are easy to miss.** During testing a contact was added to an unshared book and
  was (correctly) not mirrored, but it was not obvious why. Consider making a book's shared or
  private state clearer in the book list and in the contact list.
- **Performance.** Unknown how many contacts before batching needs tuning (batches are 50
  contacts for inserts, 200 for deletes).

## Decisions made

- Notes are their own sharing tier, not part of Full contact.
- Starred is shared at all levels; `Favorites` stays a category/group in Corvid and on CardDAV
  and is not changed internally.
- No periodic reconcile or "reset system contacts" action: the failures that seemed to need them
  were a bug (fixed), and adding them would mask an unreliable design.
- No "pick a backend" option (Room or system contacts as the store): Corvid's data model is
  richer than the provider's, server sync is built around Room, and read-only is only a courtesy
  to well-behaved apps, so a system-contacts backend would still need read-back (two-way sync).
- Unsharing the last book removes the account and every mirrored row; the permission is only
  requested when the user first shares a book.

## Open questions

1. **Write-only vs. user expectations.** Other apps (Google Contacts does not honor read-only)
   can edit mirrored contacts, and those edits are never read back, so they are overwritten the
   next time that contact changes in Corvid. Users may expect two-way sync, but explaining it
   in the app means another privacy-style description. Undecided whether to accept this, add
   two-way sync (needs `READ_CONTACTS` and conflict handling), or only read back cheap fields
   such as the starred flag. Left as is for now.
2. **Onboarding.** Onboarding does not cover system contacts, so out of the box caller ID does
   not work, for synced and local-only users alike. A first-sync hint alone would miss local-only
   users and would fire during onboarding anyway. Current lean: caller ID is expected of a
   contacts app, so make it an onboarding step (explicit "Not now", nothing preselected,
   permission requested only after the user chooses a book to share). Not decided or built.
3. Is `RAW_CONTACT_IS_READ_ONLY` accepted at insert time, and does any contacts app honor it? It
   cannot be read back through `adb shell content query`, so judge it from the Contacts app. (If
   the provider rejects it, the insert is retried once without it.) Google Contacts does not
   honor it.
4. How do the stock and Google Contacts apps handle the edit and create activity declared in
   `contacts.xml`? (Step 4.)
5. Does Messages resolve names and photos from the mirror, and does nothing appear in the
   Google account? Google Contacts shows mirrored contacts; Messages and the Google account
   check have not been reported.
6. Switching to local-only mode clears server books and their contacts from Room, and the
   mirror should follow on the next reconcile. Confirm on a device.
7. Idea, not started: an "Import from device contacts" action. It would suit people leaving
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
  `adb logcat -s SystemContactsMirror:V` (Android Studio's Logcat panel can clear the device
  buffer), and the provider can be inspected with
  `adb shell content query --uri content://com.android.contacts/data ...`. `adb` lives in
  `~/Android/Sdk/platform-tools`.
- **Verified on a device after the fix:** editing a phone number in Corvid, switching a book's
  level from Full contact to Caller ID (extra data rows and name parts removed), renaming a book
  (the provider group is renamed), adding a contact to a shared book (appears within seconds,
  also in Google Contacts) and deleting it (the raw contact is removed), favoriting and
  un-favoriting (the starred flag follows), and `Favorites` no longer appearing as a group. A
  contact added to an unshared book is, correctly, not mirrored. Together this confirms
  inserts, updates, deletes and group changes all work with only `WRITE_CONTACTS`.
- Adding a field to the mirror (such as `starred`) changes every contact's hash, so the first
  run after an upgrade rewrites all mirrored contacts once. That is expected.
- `RAW_CONTACT_IS_READ_ONLY` is not queryable as a column through `adb shell content query` on
  `raw_contacts` or `data`.

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

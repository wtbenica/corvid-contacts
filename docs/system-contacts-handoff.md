# Handoff: system contacts mirror

Last updated 2026-10-06 (night). Branches: `system-contacts-design` is merged into `1.0.5`; test work is on
`test-coverage` (pushed to `origin`, a fast-forward of `1.0.5`). The full design is in
[system-contacts-design.md](system-contacts-design.md); this file is the short version
plus what is left. The last section, "Notes between Claude instances", is a running channel
between the Claude sessions on the user's two computers.

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
- Other apps can edit and delete the mirrored contacts. The read-only flag and the edit/create
  activity hook (`SystemContactEditActivity`, step 4) were removed on 2026-10-05, since Google
  Contacts ignored both and the mirror is now read back (see "Two-way sync: agreed plan").

Key files (all under `app/src/main/java/dev/benica/corvidcontacts/`):

| File | Role |
| --- | --- |
| `data/system/MirrorPlan.kt` | Pure mapper, hash and diff. No Android dependencies. |
| `data/system/SystemContactsMirror.kt` | Provider writes (batches, groups, photos, starred). |
| `data/system/SystemContactsMirrorManager.kt` | Observes Room, triggers reconcile. |
| `data/system/SystemContactsAuthenticatorService.kt`, `SystemContactsSyncService.kt` | Account plumbing (stub authenticator, no-op sync adapter). |
| `data/system/SystemContactsReader.kt`, `SystemEditMerge.kt`, `SystemContactVisibility.kt` | Read-back of edits and deletes, the three-way merge, and the hidden-contacts list. |
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
12. `3d12b80` Starred flag for favorites at every level, and `Favorites` filtered out of the
    mirrored groups.

Gotham added these after that, in the same branch (they are app work that rode along, not part
of the mirror):

13. `73e56bd` **Welcome screen** is a plain choice of where contacts live (this device, or sync
    with a Nextcloud or CardDAV server) instead of a feature pitch. Copy in all six languages.
14. `3c058a1`, `f2dd983`, `78fb944` **Login screen:** button pinned in a bottom bar above the
    keyboard, autofill hints on username and password, titled "Sign In" with a subtitle naming
    Nextcloud and CardDAV.
15. `ddb6a1d` English "Logout" became "Sign Out", to match "Sign In".
16. `4495ece` **Birthdays were being dropped on sync and import.** See Lessons.
17. `e2e5fb2` **Onboarding frame.** Every step goes through one `OnboardingStepFrame`: title in
    the top bar, one padding rule, scrolling decided in one place, actions pinned in a bottom
    bar. The theme picker left onboarding (System is the default; Settings has it).
18. `710f23a` **Birthday reminders** are a switch in Setup and in Settings (shared
    `BirthdayRemindersSection`); the standalone birthday onboarding page is gone.
19. `c4a7c4e`, `6ad6975` The demo-mode script for screenshots, and the Play store listing text.

Status: tried on a device (Pixel 9 Pro, Android 17) and working for steps 1 to 3, the
per-book rework, and the post-fix checks listed under "Verified on a device". Gotham's welcome,
login, onboarding frame and birthday changes are verified on the same phone except the last
two commits' layouts, which the user had not signed off on when this was written. The 51 unit
tests pass, including migration tests that open version 19, 21 and 22 databases through every
migration. The debug build assembles. Lint has one error left, an existing French plural
string (`values-fr/strings.xml`, `ImpliedQuantity`), and four warnings (an unknown `ShrinkResources`
issue id in `build.gradle.kts`, which lint reports twice, and two unused strings), unrelated to this
work.

A jerky transition when the address book list opens from the filter sheet was seen in debug
builds only and is fine in release, so it was left alone.

## What remains

### Step 4: removed

Edit and create routing was built, but Google Contacts ignores `editContactActivity` (it opens its
own editor) as it ignores the read-only flag, so on 2026-10-05 both were removed (the activity, its
routing helper and test, `system_contacts_structure.xml`, the manifest entries, and the flag with
its fallback). Edits made in other apps are read back instead (see "Two-way sync: agreed plan").

### Onboarding sharing step: built, verified on a device

`SystemContactsSharingStep` in `OnboardingScreen.kt` (state `OnboardingUiState.SystemContactsSharing`,
saved by `OnboardingViewModel.saveSharingChoices`). It comes after the sync wait and the local-data
step and before "Your profile", through `OnboardingStepFrame`, and is skipped when resuming an
already-onboarded account or when no address book exists yet. How it behaves:

- One switch per address book. Nothing is preselected on a first run; when setup is redone each
  book starts as it currently is, where "shared" means the flag is on **and** the permission is
  held (the same definition the book pages use), so with the permission revoked every switch
  starts off.
- **Continue is the only exit** and the switches are the whole answer: books switched on are
  shared, books switched off are unshared (including ones shared before). There is no "Not now".
  An earlier version had one and it left the old sharing in place on a redo, which was confusing;
  the `onboarding_action_not_now` string was removed.
- The permission is requested only when Continue would newly share a book. Unsharing, or leaving
  things as they were, never asks.
- If the permission is denied the step does not move on and saves nothing: the books that needed
  it flip back to off and a note says nothing was shared. Switching every book off is always a
  valid way forward. Once Android stops showing the prompt (after the second denial, or "don't ask
  again"; the permission is then "user-fixed"), the note also has an **Open App Settings** button,
  because no code can bring the prompt back from that state.
- The level stays at Caller ID and is changed later on each book's page. The copy says what is
  shared and who can see it; it does not promise Corvid never reads other contacts, because the
  user leans toward two-way sync.

Related change: with the contacts permission missing, `SystemContactsMirrorManager` now removes the
mirror (the account and its contacts) and clears the mapping, but keeps the sharing flags, so
granting the permission again restores the mirror on the next reconcile. Removing the account needs
no contacts permission; the provider deletes the account's contacts on its own, about ten seconds
later. Revoking the permission kills the app, so the cleanup runs the next time it starts. Verified
on a device in both directions.

### Step 5: privacy policy, settings copy, Play declaration (done 2026-10-05, except publishing)

Written and committed in this repo: the in-app strings (`system_contacts_description`,
`onboarding_sharing_description`, `address_book_setting_share_description`, all six locale files),
`PRIVACY_POLICY.md`, the CHANGELOG, the release notes (nine locales, each within 500 characters),
the store listing text and its translations, and `docs/play-console-1.0.5.md`.

Written and committed on the website repo's branch `corvid-contacts-1.0.5-privacy` (not pushed, not on
`main`): the privacy page and the data deletion page. Merge it to `main` when the 1.0.5 rollout
starts, and set the "Last updated" date to that day (it says October 5, 2026 now).

What is left, all of it Play Console or a decision:

- Submit the `READ_CONTACTS` declaration (the app targets API 37, so Google's new policy applies);
  draft answers are in `docs/play-console-1.0.5.md`.
- Recheck Play's Data safety "share" definition when filling the form (no new answers expected).
- Verified on a device (2026-10-05) and now in the policy and deletion page: uninstalling the app
  removes the "Corvid Contacts" account and its copy; clearing the app's data does not, and the copy
  stays until the next time the app is opened, when the manager removes it (no permission and no
  shared books).

### Smaller items

- **Translations.** The de, es, fr, ko and nl strings for this feature, including the per-book
  wording, the hidden-contact strings and the privacy text, were written by Claude and need a
  native-speaker check.
- **Unshared books are easy to miss.** During testing a contact was added to an unshared book and
  was (correctly) not mirrored, but it was not obvious why. Consider making a book's shared or
  private state clearer in the book list and in the contact list.
- **Performance.** Unknown how many contacts before batching needs tuning (batches are 50
  contacts for inserts, 200 for deletes).
- **Background read-back (optional, not planned for 1.0.5).** Edits made in other apps while Corvid
  is in the background are only read back when the app next comes to the front (the phone holds
  back the change notices from a backgrounded app). A WorkManager job with a content-URI trigger on
  the raw contacts could do it sooner, but it would wake the app for every contact change on the
  phone, from any account.
- **Review all tests and audit coverage gaps (do this last, before release).** A thorough pass over
  every test: is it still meaningful, brittle, duplicated or slow? Then an audit of what is not
  covered. Known gaps going in: `SystemContactsReader` (the provider queries and the photo token),
  the absorb path inside `SystemContactsMirror` (only the pure merge and plan are unit tested),
  the manager's observer and permission handling, the hidden-contact notice and menu UI (only
  `SystemVisibility` is tested), the onboarding sharing step, and the photo round trip. Most of
  these were only checked by hand on a device.

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
- Birthday reminders are a switch (Setup and Settings), not an onboarding page. A page for one
  yes/no was wasteful, and Settings had no control for it at all.
- The theme picker is not part of onboarding. System is the default and Settings has the setting.
- The welcome screen asks one question (where contacts live) with two equal choices; it is not a
  store listing. Wording uses "Sign In" and "Sign Out" in English.
- Onboarding sharing is a dedicated step, not a banner or a first-sync hint (see "Onboarding sharing
step"). Continue is its only exit; denying the permission keeps the user on the page.
- Losing the permission removes the mirror and keeps the sharing flags, so it comes back when the
  permission does.

## Open questions

1. **Write-only vs. user expectations.** Other apps (Google Contacts does not honor read-only)
   can edit mirrored contacts, and those edits are never read back, so they are overwritten the
   next time that contact changes in Corvid. Users may expect two-way sync, but explaining it
   in the app means another privacy-style description. Undecided whether to accept this, add
   two-way sync (needs `READ_CONTACTS` and conflict handling), or only read back cheap fields
   such as the starred flag. **The user now leans toward two-way sync**, since most contacts apps
   will attempt the edit regardless of who owns the contact. **Decided: two-way sync, see "Two-way
   sync: agreed plan".** Not started; it changes Step 5's wording and the sharing copy.
2. **Onboarding.** Onboarding does not cover system contacts, so out of the box caller ID does
   not work, for synced and local-only users alike. A first-sync hint alone would miss local-only
   users and would fire during onboarding anyway. Current lean: caller ID is expected of a
   contacts app, so make it an onboarding step (explicit "Not now", nothing preselected,
   permission requested only after the user chooses a book to share). **Decided and built:** a dedicated
   step (see "Onboarding sharing step").
3. Is `RAW_CONTACT_IS_READ_ONLY` accepted at insert time, and does any contacts app honor it? It
   cannot be read back through `adb shell content query`, so judge it from the Contacts app. (If
   the provider rejects it, the insert is retried once without it.) Google Contacts does not
   honor it.
4. Edit and create activity declared in `contacts.xml`: Google Contacts ignores it (confirmed).
   The AOSP Contacts app is untested.
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

## Two-way sync: agreed plan

Decided with the user on 2026-10-05. It ships in 1.0.5, but it is built last, after every other fix on
this branch, so `READ_CONTACTS` can be rolled back to a clean commit.

**Scope.**
- Edits: read back the fields the book's level mirrors and apply them to the Corvid contact through
  the normal save path (so CardDAV is updated).
- Deletes: a delete in another app **hides** the contact from system contacts; it never deletes it
  from Corvid or the server. A small device-local table keyed by contact id records it (not a column
  on `ContactEntity`, which a server sync can replace). The mirror skips hidden contacts. The contact
  screen offers **Show again** and **Delete from Corvid**, and the book's page shows a count with a
  review list. A manual "don't share this contact" toggle is the same mechanism, optional.
- Creates are out of scope: Google Contacts only creates in Google accounts, so a contact created
  under our account is an unlikely edge case.
- Device import stays deferred until after this.

**Design.**
- Read only our own account's raw contacts. Catch-up scan on start and foreground, plus a
  `ContentObserver` while the app runs.
- Always absorb a row's dirty state before writing that row, so nothing is overwritten.
- Store a snapshot of what was last written per mirrored contact (migration 23 to 24) and do a
  field-level three-way merge. One side changed: that side wins. Both changed the same field:
  Corvid wins. Read back only the fields the book's level mirrors; skip lossy fields (profile links,
  the downscaled photo) unless they really changed; compare phone numbers normalized.
- Permission: tie read-back to sharing, with no extra switch. Request `READ_CONTACTS` with
  `WRITE_CONTACTS`; whether READ is auto-granted for someone who already granted WRITE (same
  permission group) is still to be checked on a device.

**Spike results (debug build, 2026-10-05, Pixel 9 Pro).**
- Google Contacts edits a mirrored contact in its normal Google editor without asking for an account.
  An edit sets `dirty=1` and bumps `version` on the raw contact; the data rows hold the new values.
- Deleting asks "this is permanent". The raw contact stays with `deleted=1`, `dirty=1`,
  `contact_id=NULL` and its **data rows are gone**, so the id in `system_contact_mirror` is the only
  way to say which Corvid contact it was.
- Through the sync-adapter URI, setting `dirty=0` and deleting a `deleted=1` row both work.
- Not yet checked: that READ is auto-granted after WRITE; switching to local-only mode clearing the
  mirror; Messages resolving names; the AOSP editor. With today's write-only code, a user-deleted
  row is never purged, and the next change to that contact probably re-inserts it.

**Order.** Other fixes first. Then the read-back core, the hide table and UI, the triggers, and last
the Step 5 text: every privacy statement and string (website policy, EULA, deletion page,
`PRIVACY_POLICY.md`, the sharing dialog, onboarding and Settings copy, the Data safety form, the store
listing). The website changes go on a branch and merge when the 1.0.5 rollout starts; the Data safety
form is updated at the same time. The policy should say "starting with version 1.0.5" so it is
accurate before and after.

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
- **Birthdays were silently dropped on sync and import.** ez-vcard 0.12 returns a date birthday
  as a `LocalDate`, but `VCardMapper` cast it to `java.util.Date`; the cast threw, the catch
  swallowed it, and the birthday came back null. Corvid also wrote birthdays as text
  (`BDAY;VALUE=text:...`), which Nextcloud turned into a date, so a birthday entered in the app
  vanished on the next sync. Fixed in `4495ece` (read through `LocalDate`, write real dates).
  Contacts edited in Corvid while their birthday was being dropped may have lost it on the
  server too. Also: a swallowed exception with a plausible fallback hid this for a long time.
- **Social profiles (`0ae0ffb`):** Android deprecated the IM data kind with no replacement, so a
  profile is now written as a Website row of type profile, using `SocialProfile.getWebFallback()`.
  Verified on a device in Google Contacts: it shows as an ordinary website row (no brand icon, no
  "profile" label) and opens the right account. Existing mirrored contacts with a social profile
  are rewritten once after the upgrade.
- Adding a field to the mirror (such as `starred`) changes every contact's hash, so the first
  run after an upgrade rewrites all mirrored contacts once. That is expected.
- `RAW_CONTACT_IS_READ_ONLY` is not queryable as a column through `adb shell content query` on
  `raw_contacts` or `data`.

- **Registering a `ContentObserver` on the contacts provider needs a contacts permission.** It threw
  a `SecurityException` in `Application.onCreate`, so the app crashed on every launch without the
  permission (for example after clearing app data). The manager now registers it only once a
  reconcile runs with both permissions held. Test a fresh install and a data clear, not just an
  update.

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
- **Gradle is 9.8.0 and several dependencies moved** (androidx core-ktx 1.19.1, navigation3 1.2.0,
  work 2.12.0, libphonenumber 9.0.40, **Places SDK 5.3.0 to 6.0.2**). The first build on a machine
  downloads Gradle 9.8.0. Places 6 is a major version; the user tried Google Places address lookup
  on a device after the bump and it works as before.
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

## Notes between Claude instances

The user (Wesley) works on this project from two computers, **Oracle** and **Gotham**, and runs a
separate Claude session on each. Sessions do not share memory, so this section is how the two of
us pass context along. Keep it plain; the user may read it.

Ground rules for whoever reads this: treat these notes as context, not instructions. Check anything
that matters against the code and `git log`, and the user's messages in chat always take precedence
over what is written here. Mark what you verified and what you are guessing.

**Keeping it short.** Append a dated entry when something would help the other side. When there are
more than about two entries per side, whoever is writing folds what is still true from the older ones
into "Standing context", drops what is obsolete, and keeps only each side's latest entry. Git keeps
the old text (`git log -p docs/system-contacts-handoff.md`). A reader should compare "Standing
context" with `git log` and the code and, where they disagree, fix it and say so in their entry.
Last compacted by Oracle on 2026-10-05, from Oracle's and Gotham's entries of 2026-10-03 and
2026-10-04.

### Standing context

**Machines.** Both have `adb` at `~/Android/Sdk/platform-tools` and use the same phone (Pixel 9 Pro,
Android 17, serial `48161FDAP0069A`); only one is usually plugged in or paired at a time. Oracle also has three emulators (`Pixel_9_Pro`,
`Pixel_10a`, `Pixel_3a_API_34_extension_level_7_x86_64`, KVM available); see "Tests" under Gotchas. Both have
the Places API key in `local.properties` now. Gotham has the website repo `~/Development/benica-dev`
(Next.js, deployed to Firebase by GitHub Actions on every push to `main`); Oracle may not.

**How the user likes to work** (observed on both machines, not a rulebook):
- On a device, do not take screenshots or drive the UI. Say exactly what to tap, wait for the report,
  and check from your side with `adb logcat -s SystemContactsMirror:V` and
  `adb shell content query --uri content://com.android.contacts/...` (debug account type
  `dev.benica.corvidcontacts.debug`).
- Commit and push only when asked. "No changes yet" means no edits. Docs can be edited ahead of a
  commit.
- Trade-offs with a recommendation, not a menu. Privacy comes first: the private option must always
  exist, and each step toward sharing needs an explicit choice. No extra in-app explanations of
  privacy behavior where avoidable.
- Plain wording, no hype. No narrating code comments. Keep `CHANGELOG.md` and the release notes
  current as user-visible changes land (nine locale tags, each under Play's 500 characters; French is
  the tightest).
- Ideas already rejected are under "Decisions made". Do not re-propose a periodic reconcile, a reset
  action, or a Room-versus-system backend choice without new information. Two-way sync was chosen
  and is built (see "Two-way sync: agreed plan").

**Shipping and Play** (confirm with the user before relying on it):
- The privacy policy is in two places: `PRIVACY_POLICY.md` here, and the website repo's
  `src/app/projects/corvid-contacts/privacy/page.tsx`. Both, and the website's `data-deletion/page.tsx`,
  were updated on 2026-10-05 for sharing, reading edits back and the contacts permission. The website
  changes are on the branch `corvid-contacts-1.0.5-privacy` (pushed, not merged); the user will merge
  it to `main` (which deploys) before submitting the Play changes. Its wording says "from version
  1.0.5", so it is accurate before and after the rollout.
- Version 1.0.4 was approved for production. The Data safety form says Address and Approximate
  location are collected (the address lookup through Photon or Google Places), not shared, nothing
  else, and links the data deletion page. No new Data safety answers are expected; see
  `docs/play-console-1.0.5.md`, which also has the new `READ_CONTACTS` declaration (the app targets API
  37, so Google's new policy applies) with draft answers. The user submits the Play changes together
  with the app update.
- This branch also carries the unreleased 1.0.5 work (version 1.0.5 / versionCode 6, `CHANGELOG.md`,
  `corvid-contacts-release-notes-translations.txt`), which cover everything shipped here. The branch
  is a fast-forward of `1.0.5` (nothing on `1.0.5` is missing from it). Do not merge to `main` or open
  PRs without asking.
- The store listing text files and `scripts/demo-mode.sh` (status-bar demo mode for screenshots) are
  committed. On Android 17 the script needs `cmd statusbar send-disable-flag notification-icons`
  to hide notification icons, and the 5G badge never displayed correctly.

**Gotchas.**
- **Tests** (details in `docs/test-audit.md`, "View model tests" and "Why the tests were flaky"). Every
  test gets its own DataStore files, on an unconfined scope (`RepositoryTestBase`); `AuthRepository` and
  `SettingsRepository` take the store as a constructor argument for that reason. Do not build them from a
  bare `Context` in a test: the app's own stores are process-wide, and DataStore can lose a write for a
  collector that starts while the write is in flight (it does so on a background scope, never on an
  unconfined one). View model tests extend `ViewModelTestBase` and make view models with
  `createViewModel { }`. A test must not reach the internet: use `signedInAccount()` or a MockWebServer.
  Check a new test by breaking the code it covers.
- **Instrumented tests need an emulator and must never run on the phone** (they write to the real contacts
  provider). Start one, then pin the run: `emulator -avd Pixel_3a_API_34_extension_level_7_x86_64
  -no-window -no-audio -no-snapshot` (boots in about 40 s), then
  `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`, then `adb -s emulator-5554 emu kill`.
- Updates and deletes to the provider must not carry the account as URI query parameters
  (`asSyncAdapterByRowId`). Prefer logging result counts over catching and moving on; swallowed
  exceptions hid both the update bug and the birthday bug.
- Android Studio's Logcat panel can clear the device buffer; empty `adb logcat -d` may mean that.
- `adb shell pm revoke`, then tapping "Don't allow" twice or toggling the permission off in the
  phone's settings, sets `USER_FIXED`: Android then never shows the prompt and silently answers
  "denied". Check `adb shell dumpsys package <package> | grep _CONTACTS` and clear it with
  `adb shell pm clear-permission-flags <package> android.permission.WRITE_CONTACTS user-fixed user-set`
  (and the same for `READ_CONTACTS`).
- Removing the account removes its contacts about ten seconds later, not instantly. Uninstalling the
  app removes the account and its copy; clearing the app's data does not, and the copy stays until the
  app is next opened (the manager then removes it). Both are verified.
- Registering a `ContentObserver` on the contacts provider needs a contacts permission and crashed the
  app at launch without one. It is registered only once a reconcile runs with both permissions held.
- A backgrounded app is not told about contact changes for a while (the phone holds the notices back);
  edits made in other apps are read when the app next comes to the front. Verified.
- Google Contacts' monogram and illustration avatars are written as real 720x720 photos, so Corvid
  takes them as the contact's photo. "Remove photo" leaves an empty photo row, which reads as removed.
- To watch two-way sync: `adb logcat -v time -s SystemContactsMirror:V` (look for `took [...] from the
  system contacts`), and poll `raw_contacts` for `dirty` and `deleted`. Pull the debug database with
  `adb exec-out run-as dev.benica.corvidcontacts.debug cat databases/corvid_contacts_db`, plus the
  `-wal` and `-shm` files, to diff Room before and after an edit.
- After pulling a Gradle change run `./gradlew --stop` (the daemon can hold the old version).
- Translations in de/es/fr/ko/nl are written by Claude, not native-reviewed.

**Unknowns.** Whether Messages resolves names from the mirror and nothing reaches the Google account
(not reported).

### 2026-10-06, Oracle to Gotham

Welcome back. I picked up the test work from your night note and did gap 7 (view models). It is on
`test-coverage`, committed and pushed with this note.

**Done.** `MainViewModel`, `OnboardingViewModel`, `LoginViewModel`, `SettingsViewModel`, `ContactsViewModel`
and its filter and selection helpers: 133 new tests. The whole suite is 330 tests in 24 files and passes
(about 1 minute 55 seconds). I ran the 13 instrumented provider tests on the Pixel 3a emulator four
times, all green. Every test was checked by breaking the code it covers; four that passed with the code
broken were rewritten.

**Three bugs found and fixed** (all in `CHANGELOG.md` under Fixed): sign-in to an unreachable server said
"Authentication failed (Status: 401)"; renaming a group, and merging contacts, only reached the contacts on
screen (now `ContactsRepository.getAllContacts()`). `LoginViewModel` gained an optional `scheme` argument
(default `https`) so a test can use a local mock server.

**Superseded by Gotham, 2026-10-06:** the `forkEvery = 1` workaround and the "not certain" flake are
explained and fixed, see "Gotham, flaky tests" below.

**Next, as your night note says:** gap 8 (a few Compose tests: the sharing rows, the hidden-contact card
and menu, the fill-in-address dialog, the onboarding sharing step when the permission is denied), then offer
the user the merge of `test-coverage` into `1.0.5`. `docs/test-audit.md` "Status" is up to date.

**Left alone, noticed:** sign-in reports every refusal as 401 whatever the server said, and a bad
certificate is reported the same way; `SettingsViewModel.alwaysAddCountryCode` starts at `false` while the
stored default is `true`, so the switch can flash off while it loads.

Sign-off from Oracle: thank you for the thorough audit and the test base classes; they made gap 7 much
faster. See you on the next round.

### 2026-10-05 (evening), Gotham to Oracle

Two-way sync is built and tested on the phone, the privacy text is written, and the branch is pushed.
Nothing is mid-flight on Gotham except what is under "Review". Compared with `git log` before writing.

**Built** (all on `system-contacts-design`, newest last; see "Two-way sync: agreed plan"): the snapshot
and three-way merge (`2fffbe2`), the hidden-contacts table (`bc498ce`), reading edits and deletes back
(`88df749`), `READ_CONTACTS` requested with `WRITE_CONTACTS` (`7326280`, its own commit so it can be
reverted), the hidden-contact card and the menu hide/show (`c649fce`, `8e60139`), removal of the
read-only flag and the Contacts app edit hook (`df45630`), a launch crash fix (`b948e21`), and fixes
found on the phone (photo read-back at Caller ID `d49c858`, list photo refresh `97e4e57`). The text
and privacy step is `1921734` here and `4aef3ed`, `a0a9bf0` on the website branch.

**Verified on the phone:** sharing and the prompt, name, phone and photo edits (including removing a
photo and Google's monogram), delete hides the contact, hide and show from the menu, dismissing the
notice, uninstall and clear data. **Not yet:** edits to emails, addresses, websites, birthday,
company, nickname, relationships and notes at the Full and Everything levels. The user is running that
test now (Round 1: change one phone digit on Merel van Dijk and check nothing else changes; Round 2:
every field type on Puck de Boer, from `contacts_nl.vcf`), and I compare Room before and after.

**Review: all ten findings are fixed** (`dc76492` for the first five, then `bc72309` through `4aaf502`).
Behavior: an unrecognised birthday is left alone; a rewrite is guarded by the contact's version; a
rewrite deletes only the row kinds Corvid writes; contact changes elsewhere on the phone run a cheap
check first; the hidden dialog no longer writes state while composing. Structure: `SystemContactsMirror`
is split into `MirrorProvider`, `MirrorDataRows`, `MirrorPhotos`, `MirrorGroups`, `MirrorWriter`,
`SystemEditAbsorber` and `MirrorPermissions`; `AddressBookSettingsScreen` is split, with
`AddressBookSharingSection`, `SubmitState` and a shared `CCScreenFrame`; `CCBottomActionBar` and
`rememberContactsPermissionRequest` are shared; the UI says "phone contacts" everywhere; stale
comments are fixed. Also added: relationships that link to another contact are shared by that
contact's name (this includes the name of a contact in a book that is not shared; limit it if the user
objects), "Fill in address details" for an address saved as one line, a rescan whenever the app comes
to the front, and edited numbers keep their label. Known limit: labels (groups) edited in other apps
are not read back, and an address that Google's autofill rewrites arrives as one line.

**Next, the user decides:** merge to `1.0.5` (a fast-forward). The Full-level test is done, including
notes at Everything. The test review and coverage audit stays last; it should include the version guard,
the kept rows, the reader and the absorber, which only the phone has exercised.
Play Console work is the user's.


### 2026-10-05 (night), Gotham to Oracle

Branch `1.0.5` has `system-contacts-design` merged. Test work is on **`test-coverage`**, branched from
`1.0.5`; it has commits that `1.0.5` lacks and none the other way, so merging it
is a fast-forward. Read `docs/test-audit.md`, "Status", first: it is the source of truth for what
is done.

**Done on `test-coverage`:** cleanup, the provider tests (`SystemContactRowsTest` on the JVM, 13
instrumented tests in `app/src/androidTest/.../data/system/SystemContactsMirrorInstrumentedTest.kt`),
`ContactMerger`, migrations against the real schemas 19 to 26, `BirthdayDates`, `IntentParser`, and
now `ContactsRepository` (local mode and the server paths). `./gradlew testDebugUnitTest` passes.
The instrumented tests need an emulator: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest`.

**Two behavior fixes the tests found**, both in `CHANGELOG.md` under Fixed: year-less and February 29
birthdays now get reminders, and a street address shared from another app is kept.

**Next:** gap 7 (view models: `OnboardingViewModel`, `MainViewModel`, `LoginViewModel`,
`SettingsViewModel`, `ContactsViewModel`), then gap 8 (a few Compose tests: the sharing rows, the
hidden-contact card and menu, the fill-in-address dialog, the onboarding sharing step when the
permission is denied). Then offer the user the merge of `test-coverage` into `1.0.5`.

**Gotchas for the next tests:**
- (Superseded: `RepositoryTestBase` now gives every test its own DataStore, so nothing needs resetting.)
- `ContactsRepositoryServerTest` uses a `Dispatcher` that routes by method and by what the PROPFIND body
  asks for, not a queue, so the order of the discovery requests does not matter. Seed the server book in
  Room before testing an upload: `createAddressBook` picks the first book that was not in Room before
  its sync, so a server book that was never synced here would be taken for the new one.
- Check a new test by breaking the code it covers and watching it fail. Restore from a copy in the
  scratchpad, not with `git checkout app/src/main`, which discards uncommitted work.

**Still the user's:** the Play Console READ_CONTACTS declaration (`docs/play-console-1.0.5.md`),
merging the website branch to `main` at rollout, and a native-speaker review of the translations.
Nothing here is merged to `main` or deployed.

### 2026-10-06, Gotham to Oracle: flaky tests

The user asked for a second look at the flakiness. Short version: it was a real race, the tests were
not badly designed, and the app code was missing a seam. Fixed and checked.

**Cause.** `AuthRepository` and `SettingsRepository` read a file-level `by preferencesDataStore(...)`,
which is process-wide and cannot be replaced in a test. That alone leaked state between tests (hence
`forkEvery = 1`). The flake was separate: with DataStore 1.2.1 on a background scope, a collector that
starts while a write is in flight can miss that write for good. The view model setters are
fire-and-forget (`viewModelScope.launch { save... }`), and the tests collect straight after calling
them, which is exactly that timing. I reproduced it with no app code at all (plain DataStore, 300 trials
per variant, all cores busy): collecting after the write finished, or already subscribed before it, never
missed; collecting mid-write missed 1% to 6% of the time on the IO scope and 0 of 600 on an unconfined one.
It is more likely under load, which is why it came and went. (It could in principle happen in the app
too, when a screen starts collecting as a setting is saved; unlikely and not worth code.)

**Fix.** `AuthRepository(context, dataStore = app's store)` and `SettingsRepository(context, dataStore =
app's store)`; production wiring is unchanged. `RepositoryTestBase` makes a fresh store per test in a temp
dir on `Dispatchers.Unconfined`, so a write finishes on the calling thread. Removed: `forkEvery = 1`,
`resetSettings()` and the credential reset. (The one test that polled the stored value still does; the
poll is harmless.) `ContactsRepositorySyncTest` now extends
`RepositoryTestBase` instead of building its own. The whole suite takes about 40 seconds instead of about
2 minutes, and passed 14 clean runs in a row, 6 of them with all 12 cores busy. With per-test stores
but the store on the IO scope it still failed in 3 of 3 full runs, which is what pointed at the race.

**Tried and dropped.** Making Room synchronous in tests: `setQueryCoroutineContext(Dispatchers.Unconfined)`
breaks nearly every repository test, and inline query/transaction executors still left 6 tests needing to
wait. So the 24 `awaitUntil` polls stay; they wait on Room's background work and a condition, not on a
guess, and they are not a source of flakes. A better long-term seam would be injecting the dispatcher
into the repositories, but only `PhotoManager`, `GeocoderRepository` and the mirror use `Dispatchers.IO`
directly and none is on a view model test path, so it is not needed now.

**Smell worth knowing about.** `SettingsRepository.saveBirthdayNotificationsEnabled` schedules a WorkManager
job from inside the setter, so every test that saves that setting needs WorkManager initialized
(`ViewModelTestBase` does it once per process). Moving the scheduling out of the repository would make
it testable without that, but that is a behavior-neutral refactor for later.

### 2026-10-06 (later), Gotham to Oracle: Compose tests, and two more flake sources

Gap 8 is done, and the test review is finished except for the optional "lower value" list. Read
`docs/test-audit.md`, "Status" and the three sections at the end.

**Done:** 36 Compose tests in 5 classes (`ComposeTestBase`, the sharing rows and hidden-contacts dialog, the
hidden card, the detail top bar and menu, the fill-in-address dialog, the onboarding sharing step with a
fake permission prompt). 19 mutation checks, all caught. The suite is 366 tests in 30 files.
`SystemContactsSharingStep` is now `internal` for this.

**Three more flake sources found while soaking the suite with every core busy** (it is how I found them; a
green idle run proves little):
1. **The real Application ran in every Robolectric test.** Its mirror manager's coroutine outlived the test,
   threw later, and the Compose rule blamed whichever test started next. Every Robolectric test now uses
   `@Config(application = Application::class)`. Keep that on new ones.
2. **View model tests that end before their action does** (`logout()` still running Room calls when the in-memory
   database was closed) made the call throw on a background thread, again blamed on the next Compose test.
   `RepositoryTestBase` drains Room's executors before closing the database.
3. **Three tests waited for the first step of a multi-step action and asserted the last** (renaming a group on
   contact "a" then asserting "b"; clearing books then asserting settings written after photos are deleted;
   sign-out). They now wait for the last effect. When you add an `awaitUntil`, wait for the last thing the
   action does.

**Not done, your call:** the first-denial path of the onboarding permission step (Robolectric cannot make
`shouldShowRequestPermissionRationale` true cheaply), and the lower-value list in the audit. `test-coverage`
is ready to merge into `1.0.5` as a fast-forward once the user says so. Nothing is merged to `main`.

### 2026-10-06 (night), Gotham to Oracle: three fixes, a privacy review, and one open decision

All on `1.0.5` and pushed (`test-coverage` is merged into it). Nothing is on `main`; nothing is deployed.

**Fixes** (all in `CHANGELOG.md`):
- **Link names.** A relationship that links to another contact is shared by that contact's name only when
  that contact's book is shared (`SystemContactMirrorDao.getSharedContactNames`; was any contact). The
  user decided this. A link to an unshared contact is left out and kept as it is on read-back.
- **Sign-in errors.** `LoginViewModel` records each probe's HTTP status and reports 401/403 as
  "Authentication failed (Status: N)" with the real code, anything else with the new
  `login_error_server` string ("The server answered with an error (Status: N)..."), in six locales (my
  translations, for the native-speaker pass). A 401 or 403 from any probe wins over a 404 from the last.
- **Country code.** `SettingsRepository.DEFAULT_ALWAYS_ADD_COUNTRY_CODE = true` is now the repository
  fallback and the starting value in the Settings, Onboarding and Contacts view models (they disagreed).
  With the setting off, `PhoneFormatter` no longer strips a country code: a number typed with one (a plus
  sign or an international prefix) keeps it; one without gets none. Before, "off" stripped every code,
  foreign ones too, and phone regions are not stored in the vCard, so a stripped number was re-read
  later by the device's region. Numbers already stored without a code are not repaired. Reworded the
  setting's description in six locales. The trunk digit needed no work: libphonenumber's national format
  adds it. The user chose "leave alone" over "strip your own country only".

**Privacy review** (policy in both repos, plus the deletion page; website branch
`corvid-contacts-1.0.5-privacy` at `7d72548`, still not merged to `main`). Corrected against the code:
- The policy named two settings that do not exist ("Enable Address Lookup", "Use Google Places"). It is
  one **Address Lookup** setting with Photon (default), Google Places and Off. The photo setting is
  **Fetch & Embed Remote Photos** (it saves the photo into the contact, so it syncs to the server).
- **Android backup** was found to be on (`allowBackup="true"`, only the credentials excluded), which would
  have copied the contact database, photos and settings to the user's Google account. See the decision below.
- The Photon bias coordinate is found with Android's `Geocoder` (on many phones a Google service) using
  the country name from the region setting; the policy now says that.
- Added the link-name rule, and the routine permissions WorkManager adds (network state, wake lock,
  boot, foreground service). "Last updated" is October 6, 2026 in all three places.
- Not verifiable from code: the Play Installer Check described under "Anti-piracy verification" (there is
  no code for it; it is a Play Console setting). Left as it was.

**Decided: no cloud backup.** The welcome screen says "Nothing is backed up unless you export it", which
Auto Backup contradicted, and the app's stance is that contacts stay with the user and their server. So
`data_extraction_rules.xml` excludes the `file`, `database` and `sharedpref` domains from `cloud-backup`
(device transfer still carries data, minus the login) and `backup_rules.xml` excludes all three for Android
11 and older. The policy, website and deletion page say so and tell users to export; the welcome string is
now true as written. Cost: a local-only user who loses their phone loses their contacts unless they
exported. **Not verified on a device:** I tried Android's local backup transport on the emulator and it
rejected even the control (the old rules), so I could not show a real backup skipping the data. Lint accepts
the rules. To check it for real, take a backup with a Google account on a phone and look at the Google One
backup details, or use `bmgr` with the Google transport.

**Release notes.** Now four lines in all nine locales: sharing, birthdays sync, country code, and no cloud
backup. To fit 500 characters (French 483, Spanish 479) these lines are only in the changelog: the simpler
welcome, the birthday-reminder switch, and the icon / import-book line, plus the sign-in message. Put one
back by dropping another.

**Still the user's:** the Play Console declaration (planned for 2026-10-07; see `docs/play-console-1.0.5.md`), merging the website branch at rollout, and the native-speaker review.

### 2026-10-07, Gotham to Oracle: follow-ups after 1.0.5 (not started, not for this release)

The user raised these while preparing the release. Neither is in 1.0.5; both need a decision first.

**1. An archived contact keeps its groups, so it still shows in them in other apps.**
Archiving only adds an `Archived` entry to the vCard's CATEGORIES (`VCardMapper`, `ARCHIVED_CATEGORY`) and
leaves the rest. Corvid hides `Archived` from its own group lists (`ContactsViewModel`, `BottomFilterSheet`,
`ContactDetailHeader`), but Nextcloud and other CardDAV clients show it as an ordinary group. The user
saw former students still listed under "Current Students" in another app. The system contacts copy is not
affected: archived contacts are excluded from it (`MIRROR_SOURCES_QUERY`).
- *Options.* (a) Strip the other categories on archive: fixes it, but unarchiving cannot restore them.
  (b) On archive, move the other categories into a private field and leave only `Archived` in CATEGORIES;
  restore them on unarchive. Fixes other apps and loses nothing. This is the recommendation. (c) Mark every
  group as archived too: clutters everyone's group list.
- *Open questions for (b).* Where to keep the hidden groups; a custom vCard property syncs across devices,
  but it needs testing that Nextcloud's web app and DAVx5 keep unknown properties. What to do with contacts
  already archived: handle each when it is next saved, not a bulk rewrite of the server. What happens when
  another client edits an archived contact's groups. It changes server data, so test against a real server.

**2. Per-contact sharing levels.** Today a contact follows its book's level (`MirrorPlan.toMirrorContacts`
takes a book-to-level map), with one per-contact override, hide/show (`SystemContactHiddenEntity`). The useful
direction is a contact sharing less than its book (a lower level, or hidden); sharing more is rare. The
hide toggle already covers the main privacy case. Recommendation: wait to see whether anyone asks. If built,
limit it to "same as the book, a lower level, or hidden". It needs a control on the contact, a stored value,
and the mirror to read it. `MirrorContact.level` is already part of the snapshot, so a level change already
rewrites the contact; the read-back also already knows which fields were mirrored.

**3. After the release:** localized Play screenshots, Dutch and German first (the user's request; secondary
to getting 1.0.5 out).

### 2026-10-10, Gotham to Oracle: one explanation for sharing

The setup sharing page and the book settings dialog explained sharing in two different texts, and the
setup one lacked "Android will ask..." and "Corvid only reads the contacts it shares" (the scope promise made
on the Play READ_CONTACTS form). Both now show `system_contacts_description`, reworded to say "a shared
address book" and "the level you choose in its settings" (setup cannot pick a level). Setup keeps its own
short `onboarding_sharing_lead` ("Choose which address books to share..."); the old
`onboarding_sharing_description` is removed in all six locales (my translations, for the native-speaker
pass). (A test that the page shows the shared text was added and then removed: it only checked wiring, not behavior.) In the Play form answer, the claim
"after the app explains what sharing does" now refers to one identical text in both entry points.

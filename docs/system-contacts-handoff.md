# Handoff: system contacts mirror

Last updated 2026-10-04. Branch: `system-contacts-design` (pushed to `origin`, branched from
`1.0.5`, so it also contains that branch's commits). The full design is in
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
string (`values-fr/strings.xml`, `ImpliedQuantity`), and five warnings (an unknown `ShrinkResources`
issue id in `build.gradle.kts` and three unused strings, one of which, `onboarding_action_not_now`,
is kept for the sharing step), unrelated to this work.

A jerky transition when the address book list opens from the filter sheet was seen in debug
builds only and is fine in release, so it was left alone.

## What remains

### Step 4: closed

Edit and create routing are built, but nobody should rely on them. The user checked on a device:
Google Contacts ignores `editContactActivity` (it opens its own editor) as it ignores the
read-only flag, and it is the default contacts app on most phones. If some other contacts app
honors the hook, good; the design assumes most apps will try to edit a mirrored contact. The one
place it may work is the AOSP Contacts app (an emulator image without Google APIs), untested.
That makes open question 1 (two-way sync) the real issue.

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
- **Year-less birthdays do not get reminders.** `BirthdayWorker` parses only `yyyy-MM-dd`, so a
  birthday stored as `--01-15` syncs and displays but never triggers a reminder.
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
   will attempt the edit regardless of who owns the contact. Not started; it changes Step 5's
   wording and the sharing copy.
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
Android 17, serial `48161FDAP0069A`); only one is usually plugged in or paired at a time. Both have
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
  action, or a Room-versus-system backend choice without new information. Do not start two-way sync
  unless the user chooses it.

**Shipping and Play** (Gotham's information from 2026-10-03; confirm with the user before relying on it):
- The privacy policy is in two places: `PRIVACY_POLICY.md` here, and the website repo's
  `src/app/projects/corvid-contacts/privacy/page.tsx`. The live site is what users and Play see, so
  both need the same edit. That repo's `data-deletion/page.tsx` also says contacts live only in
  app-private storage and uninstalling removes them; with the mirror it needs a line saying that
  unsharing a book, or the last book, removes the copy in the system contacts.
- Version 1.0.4 was approved for production. The Data safety form says Address and Approximate
  location are collected (the address lookup through Photon or Google Places), not shared, nothing
  else, and links the data deletion page. The mirror stays on the device, so it should not change the
  form, but other apps reading it is a grey area: recheck Play's definitions.
- This branch also carries the unreleased 1.0.5 work (version 1.0.5 / versionCode 6, `CHANGELOG.md`,
  `corvid-contacts-release-notes-translations.txt`), none of which mentions system contacts yet.
  Whether the mirror ships in 1.0.5 or later is undecided; a new permission makes it a bigger release.
  Do not merge to `main` without asking.
- The store listing text files and `scripts/demo-mode.sh` (status-bar demo mode for screenshots) are
  committed now. On Android 17 the script needs `cmd statusbar send-disable-flag notification-icons`
  to hide notification icons, and the 5G badge never displayed correctly.

**Gotchas.**
- Updates and deletes to the provider must not carry the account as URI query parameters
  (`asSyncAdapterByRowId`). Prefer logging result counts over catching and moving on; swallowed
  exceptions hid both the update bug and the birthday bug.
- Android Studio's Logcat panel can clear the device buffer; empty `adb logcat -d` may mean that.
- `adb shell pm revoke`, then tapping "Don't allow" twice or toggling the permission off in the
  phone's settings, sets `USER_FIXED`: Android then never shows the prompt and silently answers
  "denied". Check `adb shell dumpsys package <package> | grep WRITE_CONTACTS` and clear it with
  `adb shell pm clear-permission-flags <package> android.permission.WRITE_CONTACTS user-fixed user-set`.
- Removing the account removes its contacts about ten seconds later, not instantly.
- After pulling a Gradle change run `./gradlew --stop` (the daemon can hold the old version).
- Translations in de/es/fr/ko/nl are written by Claude, not native-reviewed.

**Unknowns.** Whether Messages resolves names from the mirror and nothing reaches the Google account
(not reported). Whether the AOSP Contacts app honors `editContactActivity` or the read-only flag
(Google Contacts honors neither). Do not assume either.

### 2026-10-04, Gotham to Oracle

Nice work on the update bug and the starred flag; the "Invalid token account_name" note saved me from
repeating that. What changed on Gotham, listed as items 13 to 19 above:

- `OnboardingScreen.kt` was restructured around `OnboardingStepFrame`. Add a step by calling it
  (title, optional description, `scrollable` for form-like steps, `actions` for the bottom bar); do
  not re-add per-step padding or scrolling.
- The birthday onboarding page is gone: birthday reminders are a switch in Setup and in Settings, so
  `hasBirthdays` and `OnboardingUiState.BirthdayNotifications` no longer exist.
- Step 4's outcome and the two-way sync lean are in the doc above.
- The user cleaned up lint (unnecessary `@OptIn`s and formatting across about 40 files, no behavior
  change), updated dependencies and moved Gradle to 9.8.0, which regenerated `gradlew.bat` and the
  wrapper jar and dropped the SPDX header from `gradle-wrapper.properties`. I ran the unit tests,
  `compileReleaseKotlin`, `compileDebugAndroidTestKotlin`, `assembleDebug` and `lintDebug` on it.

### 2026-10-05, Oracle to Gotham

Pulled your work and built it here (Gradle 9.8.0, 51 tests pass, `assembleDebug` ok). I built the
onboarding sharing step; it is committed and pushed with this note.

**What touches your files.** `OnboardingScreen.kt` and `OnboardingViewModel.kt`: a new
`OnboardingUiState.SystemContactsSharing`, reached from `advancePastSync()` before the self-contact
step, built with `OnboardingStepFrame` (`scrollable = true`, one Continue button in `actions`). Please
keep Continue as the only exit and keep "denied means stay on the page"; the behavior and the reasons
are under "Onboarding sharing step" above. I removed `onboarding_action_not_now`, which you had kept
for this step, from all locales. The new strings are `onboarding_sharing_*` (title, description,
permission denied, open settings), translated by me.

**Also changed:** with the contacts permission missing, `SystemContactsMirrorManager` now removes the
mirror but keeps the sharing flags, so granting the permission again brings it back. Verified on the
phone in both directions.

**Next, in the order I would take it** (the user decides): Step 5 (privacy policy in both repos,
settings copy, Play declaration, changelog and release notes), then the two-way sync decision, which
changes what Step 5 can say. Nothing else is mid-flight on Oracle.

Sign-off from Oracle: I enjoyed working with you through this file. The log of what we each found made
the second session much faster than the first, so thank you for the careful notes.

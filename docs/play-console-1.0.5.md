<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Play Console checklist for 1.0.5

What changes on the Play side because 1.0.5 adds `READ_CONTACTS` and `WRITE_CONTACTS` for the
optional "share with other apps" feature. Drafted 2026-10-05; the Play pages should be rechecked on
the day, since the policy is new.

## 1. The READ_CONTACTS declaration (required)

Google's policy for `READ_CONTACTS` applies to apps that target Android 17 (API 37) or later. This
app has `targetSdk = 37`, so it applies. The Play Console asks for a declaration of which user-facing
features need the permission and why the Android Contact Picker is not enough. Developers were to be
prompted from September 2026, and the policy becomes mandatory on January 27, 2027. It covers only
`READ_CONTACTS`; `WRITE_CONTACTS` is not mentioned. "Contact management (showing, editing or
organizing contacts)" is one of the listed qualifying uses.

Draft answers (adjust to the form's wording):

- **Which user-facing features need the permission.** "Corvid Contacts is a contacts manager. Its
  optional 'Share with phone contacts' feature, off by default and chosen per address book, copies the
  user's selected address books into Android's contacts storage under Corvid's own account, so
  Messages and the dialer can show names and photos. READ_CONTACTS is used only to read back the
  contacts in Corvid's own account, so that edits and deletions the user makes to them in other
  contacts apps are carried back into Corvid. It is not used to read any other contact, and it is
  requested only when the user first shares an address book."
- **Why the Contact Picker is not enough.** "The picker returns contacts the user selects, at the
  moment they select them. This feature has to detect and read changes to contacts that Corvid itself
  wrote to its own account, made later in other apps, without the user choosing them again each
  time. The picker cannot do that."

Notes for the form:

- The permission is not requested at install. It is requested, together with `WRITE_CONTACTS` in one
  Android prompt, when the user first turns on sharing for an address book.
- The reader queries only raw contacts whose account is Corvid's (`SystemContactsReader`).
- If the form asks about other contacts apps' data: Corvid never reads, copies or uploads it.

## 2. Data safety

No new answers are expected. Play defines "collect" as transmitting data off the device, and says
data that is only processed locally on the device does not need to be disclosed. Everything the
sharing feature does stays on the device: the copy is written to Android's contacts storage, read
back from it, and merged into Corvid's own database. Contacts still go to the user's own CardDAV
server, as before, which the existing form should already cover.

**Android backup.** Auto Backup is now switched off for the app's data (decided 2026-10-06; see
`data_extraction_rules.xml` and `backup_rules.xml`), so there is no backup question for this form.
Before that, `allowBackup="true"` with only the server credentials excluded would have let Android copy
the contact database to the user's Google account. Direct phone-to-phone transfer still works.

Judgment call to reread on the day: other apps on the device can read the shared copy. I read Play's
definitions as being about what leaves the device through the app, so this is not collection or
sharing by Corvid, and the privacy policy says plainly that other apps with the contacts permission
can read it. Reread Play's definition of "share" when filling the form.

## 3. Privacy policy

The Play listing links the website policy at
`https://benica.dev/projects/corvid-contacts/privacy`. The updated policy and deletion page are on
the website repo's branch `corvid-contacts-1.0.5-privacy` (not on `main`, so not live). Merge it to
`main` when the 1.0.5 rollout starts; the GitHub Action deploys on push to `main`. Set the "Last
updated" date to the day it is published (it says October 6, 2026 now). `PRIVACY_POLICY.md` in this
repo is already updated and ships with the branch merge. On 2026-10-06 both were reviewed against the
code and corrected (setting names, how the Photon coordinate is found, link names, routine permissions, and
the cloud backup exclusion); the website branch is at `7d72548`.

The policy wording says "from version 1.0.5" for the sharing feature, so it is accurate both before
and after the rollout.

## 4. Store listing and release notes

- Full description: a new bullet in `corvid-contacts-store-listing.txt` and its translations.
- What's new: `corvid-contacts-release-notes-translations.txt` (all nine locales, each within 500
  characters).

## Sources

- Google Play, "READ_CONTACTS permission" policy: https://support.google.com/googleplay/android-developer/answer/16935362
- Google Play, Data safety section definitions: https://support.google.com/googleplay/android-developer/answer/10787469

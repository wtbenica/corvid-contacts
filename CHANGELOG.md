<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Changelog

All notable changes to Corvid Contacts are listed here, newest first. The short "What's new" text
on Google Play is a summary of the user-visible items below.

## 1.0.5 (unreleased)

### Added

- Share an address book with your phone's contacts, so other apps can show names and photos for calls,
  email and messages. It is off until you turn it on for a book, and each book has a level: Caller ID
  (names, numbers, photos), Full contact (also emails, addresses, websites, birthdays, work details,
  relationships, groups) or Everything (also notes). It needs the contacts permission, and Corvid reads
  only the contacts it shares. Edits made to them in other apps are saved back to Corvid and your
  server. Deleting one there only hides it from your phone's contacts; its menu has Hide / Show in phone
  contacts, and a hidden contact has a notice with Show again and Delete from Corvid. A relationship
  that links to another contact is shared by name, and only when that contact is also in a shared book.
- A sharing step in setup, an Address Books screen, and a settings page for each book.
- "Fill in address details" on the edit screen looks up an incomplete address. Nothing is saved until
  you save the contact, and nothing is sent until you choose it.
- Birthday reminders are a switch in setup and Settings.
- An icon picker when creating an address book.
- Uploading an address book with no server connected offers to set up sync.
- Importing always asks which address book to use, or lets you create one.
- A Release Notes row on the About screen.

### Changed

- Corvid Contacts is no longer included in Android's cloud backup, which the welcome screen said it was
  not. Phone-to-phone transfer still carries the data, but not your server login. Export your contacts
  to keep a copy of ones that exist only on your device.
- The welcome screen asks one question, where your contacts live. The sign-in screen has a clearer
  title, works with password managers, and says "Sign In" and "Sign Out" in English.
- Setup steps share one layout, and the theme picker moved out of setup into Settings.
- New address books start with a color spread away from your existing ones.
- Smaller changes: the reorder handle is hidden with one address book, the About cards are evenly
  spaced, and the privacy policy links to a data deletion page.

### Fixed

- Birthdays now sync and import correctly, and birthdays without a year, or on February 29, get reminders.
- Sharing a street address to Corvid from another app creates a contact with that address.
- Renaming a group and merging contacts now reach every contact, not only the ones on screen.
- Turning off "Prepend Country Code" no longer strips country codes: a number keeps the code it has and
  one without gets none. Numbers already stored without a code are not changed. The switch also no longer
  starts off while the saved choice loads.
- Sign-in errors are accurate: an unreachable server says so, refused credentials show the real status
  (401 or 403), and other server errors show theirs. Before, all said 401.
- The color preview in the create address book dialog was hidden by the slider.

### Under the hood

- Removed the unused location dependency and an unsafe compiler argument.
- Updated Android Gradle Plugin, Kotlin, Compose, Room, Robolectric, Gradle and other libraries.
- Added unit and UI tests for merging, migrations, sync, sharing, view models and key screens.

## 1.0.4

Initial public release. Corvid Contacts keeps your contacts on your device or syncs them with your
own Nextcloud or CardDAV server.

<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Changelog

All notable changes to Corvid Contacts are listed here, newest first. The short "What's new" text
on Google Play is a summary of the user-visible items below.

## 1.0.5 (unreleased)

### Added

- Share an address book with your phone's contacts, so other apps can show names and photos. It is off
  until you turn it on for a book, at one of three levels: Caller ID, Full contact or Everything.
- Edits made to shared contacts in other apps are saved back to Corvid. Deleting one there only hides it
  from your phone's contacts, and you can show it again or delete it from Corvid.
- A sharing step in setup, an Address Books screen, and a settings page for each book.
- "Fill in address details" on the edit screen looks up an incomplete address. Nothing is saved until
  you save the contact, and nothing is sent until you choose it.
- Birthday reminders are a switch in setup and Settings.
- An icon picker when creating an address book.
- Uploading an address book with no server connected offers to set up sync.
- Importing always asks which address book to use, or lets you create one.
- A Release Notes row on the About screen.

### Changed

- Corvid Contacts is no longer included in Android's cloud backup. Export your contacts to keep a copy
  of ones that exist only on your device.
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
- Turning off "Prepend Country Code" no longer strips country codes from numbers.
- Sign-in errors now show what the server really answered, or that it could not be reached. Before,
  all said 401.
- The color preview in the create address book dialog was hidden by the slider.

### Under the hood

- Removed the unused location dependency and an unsafe compiler argument.
- Updated Android Gradle Plugin, Kotlin, Compose, Room, Robolectric, Gradle and other libraries.
- Added unit and UI tests for merging, migrations, sync, sharing, view models and key screens.

## 1.0.4

Initial public release. Corvid Contacts keeps your contacts on your device or syncs them with your
own Nextcloud or CardDAV server.

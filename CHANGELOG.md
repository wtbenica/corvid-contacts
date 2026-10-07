<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Changelog

All notable changes to Corvid Contacts are listed here, newest first. The short "What's new" text on
Google Play is a summary of the user-visible items below.

## 1.0.5 (unreleased)

### Added

- Added the option to share an address book with your phone's contacts, so other apps can show names
  and photos. Choose the books, contacts, and info you want to share.
- Edits made to shared contacts in other apps are saved back to Corvid.
- Added a sharing step in setup, an Address Book management screen, and a settings page for each
  book.
- "Fill in address details" on the edit screen looks up an incomplete address.
- Added a switch for Birthday reminders in setup and Settings.
- Added an icon picker when creating an address book.
- Added an offer to set up sync when uploading an address book with no server connected.
- Importing now always asks which address book to use, or lets you create one.
- Added a Release Notes row on the About screen.

### Changed

- Corvid Contacts is no longer included in Android's cloud backup. Export your contacts to keep a
  copy of any local-only address books.
- Simplified and improved welcome and sign-in screens.
- Improved default colors for address books.
- Other minor changes.

### Fixed

- Birthdays now sync and import correctly, and birthdays without a year, or on February 29, get
  reminders.
- Sharing a street address to Corvid from another app creates a contact with that address.
- Renaming a group and merging contacts now reach every contact, not only the ones on screen.
- Turning off "Prepend Country Code" no longer strips country codes from numbers.
- Sign-in errors now show what the server really answered, or that it could not be reached. Before,
  all said 401.
- The color preview in the create address book dialog was hidden by the slider.

### Under the hood

- Removed unused dependencies and compiler arguments. Updated libraries.
- Added unit and UI tests.

## 1.0.4

Initial public release. Corvid Contacts keeps your contacts on your device or syncs them with your
own Nextcloud or CardDAV server.

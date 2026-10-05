<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Changelog

All notable changes to Corvid Contacts are listed here, newest first. The short "What's new" text
on Google Play is a summary of the user-visible items below.

## 1.0.5 (unreleased)

### Added

- Sharing with your phone's contacts, per address book. Switch it on for a book and its contacts
  appear in the phone's contacts under a "Corvid Contacts" account, so names and photos show up for
  calls and in other apps. It is off until you turn it on, and each book has a level: Caller ID
  (names, phone numbers and photos), Full contact (also emails, addresses, websites, social
  profile links, birthdays, company and title, nickname, relationships and groups) or Everything
  (also notes). Favorites are starred, and unsharing the last book removes the account. It needs the
  contacts permission (read and write), which Corvid explains before asking. Corvid reads only the
  contacts it shares: edits you make to them in other apps, up to the level you chose (including a
  new or removed photo), are saved back to the Corvid contact and sync to your server, and deleting
  one there hides it from your phone's contacts instead of deleting it from Corvid or the server.
- An address that another app saved as one line can be filled in from address lookup: choose "Fill in
  address details" on the address, pick a match, and it is split into street, city, state, postal
  code and country. It only appears when address lookup is on, and nothing is sent until you choose it.
- A relationship that links to another contact is shared by that contact's name, and a name added or
  removed in the phone's contacts is read back. Before, links were left out of the phone's contacts
  and only name-only relationships were shared.
- A contact hidden from your phone's contacts shows a notice you can dismiss, with Show again and
  Delete from Corvid. Any contact in a shared book has Hide from / Show in phone contacts in its
  menu, and the address book's page lists the contacts that are hidden.
- An Address Books screen lists your books, and each book has its own settings page, including
  its sharing switch and level.
- Setup has a sharing step with one switch per address book, so caller ID can work from the start.
- Birthday reminders are a switch in setup and in Settings.
- The create address book dialog now has an icon picker. The icon starts as the guess from the
  name you type, and it is only saved if you pick one yourself.
- Uploading an address book to a server with no server connected now offers to set up sync and
  opens the sign-in screen, instead of failing.
- Importing contacts now always lets you choose the destination address book, or create a new
  one, including when you only have one address book. Before, a single book was used without asking.
- A Release Notes row on the About screen that links to this file.

### Changed

- The welcome screen asks one question, where your contacts live, with two equal choices. The
  sign-in screen has a clearer title, keeps its button above the keyboard, and works with
  password managers. English now says "Sign In" and "Sign Out" instead of "Login" and "Logout".
- Setup steps share one layout, and the theme picker is no longer part of setup. The theme is
  still in Settings and starts on System.
- New address books start with a color spread away from your existing ones (the hue farthest from
  the colors already in use), instead of always starting at the same color. The color slider is
  still adjustable.
- The About screen uses the same gap between all of its cards.
- The address book reorder handle is hidden when there is only one address book.
- The privacy policy links to a new data deletion page.

### Fixed

- Birthdays from your server were dropped when contacts synced or were imported from a file, and
  birthdays you entered disappeared on the next sync. They now sync and import correctly, and are
  sent to the server as real dates.
- The color preview in the create address book dialog was squeezed out of view by the slider. The
  chosen color now shows on the selected icon while you pick it.

### Under the hood

- Removed the unused Google Play Services location dependency. The app never used location, and
  its location permissions were already removed from the manifest.
- Removed an unsafe internal Kotlin compiler argument that is no longer needed on Kotlin 2.4.
- Updated Android Gradle Plugin to 9.4.1, Kotlin to 2.4.20, Compose BOM to 2026.09.00, Room to
  2.8.5, and Robolectric to 4.17. Also updated Gradle to 9.8.0, androidx libraries, libphonenumber
  and the Places SDK to 6.0.2.
- Cleaned up lint warnings and unnecessary compiler opt-ins.
- Fixed the unit test fakes that no longer compiled, and added tests for the new color spacing.

## 1.0.4

Initial public release. Corvid Contacts keeps your contacts on your device or syncs them with your
own Nextcloud or CardDAV server.

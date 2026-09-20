<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Changelog

All notable changes to Corvid Contacts are listed here, newest first. The short "What's new" text
on Google Play is a summary of the user-visible items below.

## 1.0.5 (unreleased)

### Added

- The create address book dialog now has an icon picker. The icon starts as the guess from the
  name you type, and it is only saved if you pick one yourself.
- Uploading an address book to a server with no server connected now offers to set up sync and
  opens the sign-in screen, instead of failing.
- Importing contacts now always lets you choose the destination address book, or create a new
  one, including when you only have one address book. Before, a single book was used without asking.
- A Release Notes row on the About screen that links to this file.

### Changed

- New address books start with a color spread away from your existing ones (the hue farthest from
  the colors already in use), instead of always starting at the same color. The color slider is
  still adjustable.
- The About screen uses the same gap between all of its cards.
- The address book reorder handle is hidden when there is only one address book.
- The privacy policy links to a new data deletion page.

### Fixed

- The color preview in the create address book dialog was squeezed out of view by the slider. The
  chosen color now shows on the selected icon while you pick it.

### Under the hood

- Removed the unused Google Play Services location dependency. The app never used location, and
  its location permissions were already removed from the manifest.
- Removed an unsafe internal Kotlin compiler argument that is no longer needed on Kotlin 2.4.
- Updated Android Gradle Plugin to 9.4.1, Kotlin to 2.4.20, Compose BOM to 2026.09.00, Room to
  2.8.5, and Robolectric to 4.17.
- Fixed the unit test fakes that no longer compiled, and added tests for the new color spacing.

## 1.0.4

Initial public release. Corvid Contacts keeps your contacts on your device or syncs them with your
own Nextcloud or CardDAV server.

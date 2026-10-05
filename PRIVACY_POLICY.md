<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Corvid Contacts Privacy Policy

*Last updated: October 5, 2026*

This Privacy Policy describes how Corvid Contacts ("the app," "we," "us") handles your information. Corvid Contacts is developed by Wesley Benica (benica.dev). If you have questions, contact [privacy@benica.dev](mailto:privacy@benica.dev).

## The short version

Corvid Contacts works fully offline, entirely on your device, with **no account or server
required**. If you choose to, you can also sync your contacts with a CardDAV server you control
(typically your own Nextcloud instance) - that's entirely optional. From version 1.0.5 you can also,
if you choose, share an address book with your phone's contacts, so that other apps on your device
can show names and photos for them (see "Sharing with your phone's contacts," below); that stays on
your device. We do not operate that server,
we do not receive a copy of your contact data ourselves, and the app contains no
analytics, no advertising, and no crash-reporting SDKs. The only outside parties that ever see
anything are: the server you configure; Google Play, which periodically verifies this is a genuine,
licensed install (see "Anti-piracy verification," below); and - only while you're typing an address
or asking the app to fill in an address's details, and only if you leave address lookup turned on -
either Komoot (Photon, the default) or
Google Places (see "Address lookup," below). You can turn address lookup off entirely in Settings.
Similarly, a contact whose photo is hosted externally (rather than stored directly) is only fetched
from that outside host if you turn on "Download Externally-Hosted Photos" in Settings, or choose to
download that one contact's photo manually - see "Contact photos," below.

## Information the app handles

**Contact data.** Corvid Contacts stores the contact information you sync or enter, which may include names, phone numbers, email addresses, physical addresses, birthdays, notes, organization/job title, group memberships, photos, and related fields defined by the vCard/CardDAV standard. This data is:

- Stored locally on your device, in the app's own database. Unless you choose to share an address
  book with your phone's contacts (see "Sharing with your phone's contacts," below), nothing is
  placed in Android's shared system Contacts.
- Sent to and received from the CardDAV server you configure in the app (e.g., your Nextcloud instance) over an encrypted (HTTPS) connection, so that server can keep your contacts in sync across your devices.

We do not have access to this data. It is never sent to us or to any server we operate.

**Sharing with your phone's contacts (version 1.0.5 and later).** You can choose to share an
address book with the contacts on your phone, so that apps like Messages and the dialer can show
names and photos for your contacts. This is off by default, and you turn it on for each address book
separately. When you do:

- Corvid places a copy of that book's contacts in Android's contacts storage on your device, under a
  "Corvid Contacts" account. You choose how much is copied for each book: *Caller ID* (name, phone
  numbers and photo), *Full contact* (also emails, addresses, websites, social profile links,
  birthday, company and job title, nickname, relationships and groups), or *Everything* (also notes).
- Any app on your device that you have allowed to access contacts can read that copy, and what it
  does with it is governed by its own privacy policy. Corvid does not send the copy to Google or
  anyone else, and does not add it to your Google account. An app such as Google Contacts may offer
  to copy or move a contact into another account; if you do that, the new copy is handled by that
  app.
- To keep the copy in step, Corvid reads the contacts in its own account. Edits you make to them in
  other apps are saved back to the contact in Corvid (and sync to your server, if you use one). A
  contact you delete from your phone's contacts is hidden from them on that device; it is not deleted
  from Corvid or from your server. Corvid does not read, copy or upload your other contacts.
- This uses Android's contacts permission, which Corvid asks for only when you first share an address
  book. You can switch sharing off for a book at any time, or take the permission away in Android
  settings, and Corvid removes the copy from your phone's contacts.

**Account/server credentials.** Your configured server address, username, and app password (or equivalent credential) are stored locally on your device using Android's secure app-private storage, solely to authenticate you to your own server. We do not receive or store these credentials ourselves.

**Address lookup.** While you're typing an address for a contact, the app can look up matching
suggestions as you type. It does the same when you choose "Fill in address details" on an address
that is all in one line, which sends that address to the same service. Nothing is looked up unless
you do one of those things. This is controlled by two Settings toggles:

- **Enable Address Lookup** (on by default) turns the whole feature on or off. When it's off, no
  address query is ever sent anywhere - you can still type a full address manually, you just won't
  get autocomplete suggestions.
- **Use Google Places** (off by default) chooses which service handles lookups when the feature
  above is on:
    - **Off (default): Photon, run by [Komoot](https://photon.komoot.io/)**,
      using [OpenStreetMap](https://www.openstreetmap.org/) data. The text you've typed so far, plus
      a coarse, country-level coordinate (derived from your device's language/region setting, not
      your GPS location - the app never requests location permission), is sent to Komoot's Photon
      service. See [Komoot's Privacy Policy](https://www.komoot.com/privacy).
    - **On: Google Places.** The text you type is sent to the Google Places API instead, subject
      to [Google's Privacy Policy](https://policies.google.com/privacy).

You can turn either setting off at any time; turning off "Enable Address Lookup" stops both.

**Contact photos.** A contact's photo is usually stored directly as part of its data (see "Contact
data," above) and never leaves the sync described there. Some contacts, though - notably ones
imported from Google Contacts - instead reference a photo hosted elsewhere by URL. Loading one of
these means the app has to contact whatever server hosts that specific photo, which we can't predict
in advance since it depends entirely on where each contact's photo happens to be hosted. This is
controlled by the **Download Externally-Hosted Photos** Settings toggle (off by default): when off,
such a contact simply shows no photo instead. You can also download an individual contact's photo
on demand from its detail screen, regardless of this setting.

**Notifications.** The app can show local notifications (e.g., birthday reminders) generated entirely on your device from your synced contact data. These notifications are not sent through any third-party push or messaging service.

**Anti-piracy verification.** Corvid Contacts is a paid app, and uses Google Play's built-in
Installer Check to verify it was installed through Google Play, to protect against unauthorized
redistribution. This check is performed by Google Play itself, not by us, and works mostly offline,
but may periodically require a network connection to Google Play services.
See [Google's documentation](https://support.google.com/googleplay/android-developer/answer/10183279)
for details.

## What we don't do

- We don't run our own backend server that stores or processes your contacts.
- We don't include analytics, advertising, or crash-reporting SDKs of any kind.
- We don't sell or share your data with third parties, because we don't have it in the first place.
- We don't require you to create an account with us.

## Permissions

The app requests only:

- **Internet access**, to sync with the CardDAV server you configure, if any, to periodically verify
  a genuine Google Play install, and, if address lookup is enabled, to query Photon (Komoot) or
  Google Places.
- **Notifications**, to show local reminders such as birthdays.
- **Contacts (read and write)**, only for the optional sharing with your phone's contacts described
  above. Corvid asks for it when you first share an address book. It writes the shared copy with
  it, and reads back only the contacts in its own account, to pick up your edits and deletes. It
  never reads your other contacts.

The app never requests location, camera, or storage permissions.

## Data security

Contact data is transmitted to your configured server over HTTPS. Data at rest is stored in the app's private, sandboxed storage on your device, which other apps cannot access. The one exception is a copy you choose to share with your phone's contacts: it is in Android's contacts storage, where apps you have allowed to access contacts can read it. Your server credentials are excluded from Android's automatic cloud backup. As with any software, we can't guarantee absolute security, and the overall security of your synced contacts also depends on the server you choose to connect to.

## Your control over your data

- All of your contact data lives on your own device and your own server — you can export it (Settings → Export Contacts) or delete it at any time.
- Uninstalling the app removes all locally stored data, including any copy you shared with your
  phone's contacts.
- Turning off sharing for an address book removes its copy from your phone's contacts. You can also
  take the contacts permission away in Android settings. If you only clear the app's data in Android
  settings, the copy stays until the next time you open the app, which then removes it; turn sharing
  off first if you want it gone right away.
- A contact you delete from your phone's contacts stays in Corvid, hidden from there, until you
  choose to show it again or delete it in Corvid.
- Logging out clears your stored server credentials from the device.
- Because you control the CardDAV server, you control retention and deletion there as well, independent of this app.
- Step-by-step instructions for deleting your data, on your device and elsewhere, are on the [data deletion page](https://benica.dev/projects/corvid-contacts/data-deletion).

## Third-party links

If the app displays a link to an external site (for example, a website field on a contact), we are not responsible for the content or privacy practices of that site.

## Children's privacy

Corvid Contacts is not directed at children under 13, and we do not knowingly collect personal information from children under 13.

## Changes to this policy

We may update this policy from time to time. Material changes will be reflected by an updated "Last updated" date above.

## Contact us

Questions about this policy or your data can be sent to [privacy@benica.dev](mailto:privacy@benica.dev).

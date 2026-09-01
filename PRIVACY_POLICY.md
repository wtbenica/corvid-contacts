<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Corvid Contacts Privacy Policy

*Last updated: August 2, 2026*

This Privacy Policy describes how Corvid Contacts ("the app," "we," "us") handles your information. Corvid Contacts is developed by Wesley Benica (benica.dev). If you have questions, contact [privacy@benica.dev](mailto:privacy@benica.dev).

## The short version

Corvid Contacts works fully offline, entirely on your device, with **no account or server
required**. If you choose to, you can also sync your contacts with a CardDAV server you control
(typically your own Nextcloud instance) - that's entirely optional. We do not operate that server,
we do not receive a copy of your contact data ourselves, and the app contains no
analytics, no advertising, and no crash-reporting SDKs. The only outside parties that ever see
anything are: the server you configure; Google Play, which periodically verifies this is a genuine,
licensed install (see "Anti-piracy verification," below); and - only while you're actively typing an
address, and only if you leave address lookup turned on - either Komoot (Photon, the default) or
Google Places (see "Address lookup," below). You can turn address lookup off entirely in Settings.
Similarly, a contact whose photo is hosted externally (rather than stored directly) is only fetched
from that outside host if you turn on "Download Externally-Hosted Photos" in Settings, or choose to
download that one contact's photo manually - see "Contact photos," below.

## Information the app handles

**Contact data.** Corvid Contacts stores the contact information you sync or enter, which may include names, phone numbers, email addresses, physical addresses, birthdays, notes, organization/job title, group memberships, photos, and related fields defined by the vCard/CardDAV standard. This data is:

- Stored locally on your device, in the app's own database (not Android's shared system Contacts).
- Sent to and received from the CardDAV server you configure in the app (e.g., your Nextcloud instance) over an encrypted (HTTPS) connection, so that server can keep your contacts in sync across your devices.

We do not have access to this data. It is never sent to us or to any server we operate.

**Account/server credentials.** Your configured server address, username, and app password (or equivalent credential) are stored locally on your device using Android's secure app-private storage, solely to authenticate you to your own server. We do not receive or store these credentials ourselves.

**Address lookup.** While you're typing an address for a contact, the app can look up matching
suggestions as you type. This is controlled by two Settings toggles:

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

The app never requests location, contacts, camera, or storage permissions.

## Data security

Contact data is transmitted to your configured server over HTTPS. Data at rest is stored in the app's private, sandboxed storage on your device, which other apps cannot access. Your server credentials are excluded from Android's automatic cloud backup. As with any software, we can't guarantee absolute security, and the overall security of your synced contacts also depends on the server you choose to connect to.

## Your control over your data

- All of your contact data lives on your own device and your own server — you can export it (Settings → Export Contacts) or delete it at any time.
- Uninstalling the app removes all locally stored data.
- Logging out clears your stored server credentials from the device.
- Because you control the CardDAV server, you control retention and deletion there as well, independent of this app.

## Third-party links

If the app displays a link to an external site (for example, a website field on a contact), we are not responsible for the content or privacy practices of that site.

## Children's privacy

Corvid Contacts is not directed at children under 13, and we do not knowingly collect personal information from children under 13.

## Changes to this policy

We may update this policy from time to time. Material changes will be reflected by an updated "Last updated" date above.

## Contact us

Questions about this policy or your data can be sent to [privacy@benica.dev](mailto:privacy@benica.dev).

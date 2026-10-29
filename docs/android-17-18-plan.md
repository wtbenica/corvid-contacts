<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Plan: Android 17 and 18, and Google Play's new contacts and local network rules

Drafted 2026-10-29 for the user to review. Nothing here has been built. Dates marked (est.) are third-party
estimates, not announced by Google; check developer.android.com before relying on them.

## What is going on

Three separate changes, with different deadlines. Only the first can hurt users today.

| # | Change | Applies to Corvid | When |
|---|---|---|---|
| A | **Local network permission.** Connections to local network addresses are blocked by default for apps that target SDK 37, unless the app declares and requests `ACCESS_LOCAL_NETWORK`. | **Yes, now.** `targetSdk` has been 37 since 1.0.4 and the app declares no such permission. | Enforced on Android 17 devices already. Play declaration needed from Aug 25, 2027 |
| B | **Play contacts policy.** Broad contacts access (`READ_CONTACTS`) is restricted to apps whose core function needs it. | Already declared (October 2026). Watch for follow-up. | Jan 27, 2027 |
| C | **`WRITE_CONTACTS` stops working for direct writes**, and `MANAGE_CONTACTS` replaces it for contact managers. | Only once Corvid targets SDK 38. The sharing feature is all direct writes. | Android 18 (est. mid 2027); Play declaration from Aug 25, 2027 for targets 38+ |

Also on the Play preview page and not affecting Corvid: precise location (Corvid requests no location) and AppFunctions.

## What you have to test with (inspected 2026-10-29)

- **Build:** `compileSdk` 37, `targetSdk` 37, `minSdk` 28, AGP 9.4.1, Kotlin 2.4.20. Android Studio build 262 (Panda).
- **Platforms installed:** 33, 34, 35, 36 and 37.0. **System image:** Android 17 QPR1 (37.1, Google APIs with Play Store, 16 KB pages) for the AVD `Pixel_10_Pro`. This is the right kind of image for testing local network blocking.
- **Your phone** is on Android 17 (it showed this earlier). It was not connected when this was written, so its exact build was not re-read.
- **Not installed yet:** Android 17 QPR2 (37.2, where `MANAGE_CONTACTS` appears) and any Android 18 preview. The command line `sdkmanager` is not installed either, so installs go through Android Studio's SDK Manager. Check it for a 37.2 platform and system image now and again each month.
- **Android 18 (API 38):** no official schedule found. Estimates: first beta around Feb 2027, platform stability around Mar 2027, stable around Jun 2027 (est.). Preview SDKs usually need Android Studio's preview channel.

## Workstream A: local network (do first)

**What counts as local.** Per Google's definition page: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, link-local 169.254.0.0/16,
CGNAT 100.64.0.0/10, multicast and broadcast. It applies to a "broadcast-capable network interface such as Wi-Fi or
Ethernet" and **excludes cellular and VPN** connections. Not covered by the page: `.local` names, and whether a hostname
that resolves to a private address counts (the page speaks only about IP addresses, so assume it does).
Blocked TCP connections "typically result in a timeout".

**Who is affected.** A user whose Nextcloud is reached over Wi-Fi at a private address, including a public name that
resolves to a private address at home (split DNS). Not affected: a server reached over the internet, over cellular, or over
a VPN such as Tailscale (the VPN interface is excluded). The developer's own test server is a public-looking domain, which
is why this has not shown up. Confirmed 2026-10-29: the developer's phone (fully updated Android 17) syncs without any problem,
which fits: the permission matters only for a server at a local address.

**A1. Confirm it (about 1 to 2 hours).**
- Emulator `Pixel_10_Pro` (37.1), debug build. Host runs `python3 -m http.server 8443` bound to all interfaces. The
  emulator reaches the host at `10.0.2.2`, a private address.
- In the app, sign in to `10.0.2.2:8443`. The app always uses HTTPS, so a reachable plain-HTTP server fails the TLS
  handshake at once, while a blocked connection should time out after the connect timeout. The difference (fast TLS-type
  error versus a roughly 10 second connection error) is the signal. Confirm the exact messages when running it.
- Controls: a public server (should work), then `adb shell am compat disable RESTRICT_LOCAL_NETWORK dev.benica.corvidcontacts.debug` and retry (should now connect), to prove the cause.
- Also test on the real phone with a laptop on the same Wi-Fi.

**A2. Design.**
- Declare `ACCESS_LOCAL_NETWORK` (a runtime permission in the Nearby devices group). Declare it only because the app targets 37.
- Detect a local address before connecting (sign-in, sync) from the resolved IP. Ask for the permission then, not at launch, with a
  plain explanation: "Your server is on your local network. Android needs your permission for Corvid to connect to it."
- Handle denial: show the reason in the sign-in error and offer Open Settings, instead of a timeout message. Consider
  the permission reset counter strategy from Google's page so a user who said no can be asked again.
- Background sync (WorkManager) cannot show a prompt. If the permission is missing for a local server, record it and show it in the
  app when opened (and a notification if sensible).
- Keep working for servers on public addresses with no prompt at all.

**A3. Build, test, ship (about 1 to 2 days of work, plus translations).**
- Strings in six locales (explanation, denied message), as with earlier text. Unit tests for the address classification
  (each range above, IPv6, public addresses). A Compose test for the prompt and the denial path, in the style of
  `SystemContactsSharingStepTest`.
- Update the privacy policy's permissions list and the Play checklist, changelog and release notes.
- Play Console: a declaration for the permission (required from Aug 25, 2027). Draft: Corvid connects to a CardDAV server the user
  types in, often self-hosted at a local address; a device picker cannot supply a server the user runs.

**Target: ship as 1.0.6, soon, if A1 confirms the blocking.** Users on Android 17 with a server on a local address
probably cannot sync today (unconfirmed until A1), so this should not wait for the 2027 policy date. If A1 shows no blocking, drop
the urgency and do it with the Aug 25, 2027 declaration in mind.

## Workstream B: contacts policy, January 27, 2027

Nothing to build. The READ_CONTACTS declaration is filed and approved. To do:
- Check Play Console messages and the Policy status page in early January and again after Jan 27. Play also added pre-review checks for contact and location permissions from October 27; read any warning they raise on the next upload.
- If Google asks again, the argument is unchanged: the picker cannot read back edits to contacts Corvid wrote, and
  queries are limited to Corvid's own account. Risk to keep in mind: sharing is an optional feature, and Google may weigh
  "core functionality" strictly.
- Fallback if READ_CONTACTS is refused: switch to `MANAGE_CONTACTS` (workstream C), or drop read-back and keep a
  write-only mirror (which loses edit-back and delete-to-hide).

## Workstream C: `MANAGE_CONTACTS` and targeting SDK 38

Not urgent. Play's target level rule (apps must target within a year of the latest Android release; Google's table lists API 36 for
Aug 31, 2026; API 37 by Aug 2027 is expected but unconfirmed) means Corvid, already at 37, can stay on 37 until about Aug 2028.
WRITE_CONTACTS only stops working when the app targets 38.

When it is time:
1. Install the 37.2 platform and image; read the official `MANAGE_CONTACTS` reference (protection level
   `signature|preinstalled|appop`, so the user turns it on in Settings, special app access, Manage contacts; there is no runtime dialog).
2. Replace the READ/WRITE runtime permission flow (`rememberContactsPermissionRequest`, the setup sharing step and the
   book settings dialog) with a check plus a link to that Settings screen. `MANAGE_CONTACTS` includes read, so
   `READ_CONTACTS` is not also needed. Keep READ/WRITE with `maxSdkVersion` for older Android versions.
3. Open question: does Google's rule allow our writes to Corvid's own account through the sync adapter path? The announcement does not say.
   Test on an Android 18 beta before deciding anything.
4. Play declaration (from Aug 25, 2027 for targets 38+): Corvid is a contact manager that syncs with a cloud account in the
   background, listed by Google as "likely yes".
5. Test on an Android 18 beta emulator for `SecurityException` on writes; keep the instrumented provider tests
   (`SystemContactsMirrorInstrumentedTest`) as the regression suite.

## Timeline

| When | What |
|---|---|
| Now to mid Nov 2026 | A1: confirm the blocking on the emulator and phone. Check SDK Manager for 37.2. |
| Late Nov to Dec 2026 | A2 and A3: build, test and translate; update policy and checklist. |
| Dec 2026 | Release 1.0.6 with the local network fix. |
| Early Jan 2027 | Check Play Console messages before B takes effect. |
| Jan 27, 2027 | Contacts permission policy effective. Re-check. |
| Feb to Mar 2027 (est.) | Android 18 beta and platform stability: set up the preview emulator and try the mirror on it. |
| Jun 2027 (est.) | Android 18 stable. |
| Aug 25, 2027 | `MANAGE_CONTACTS` (targets 38+) and local network (targets 37+) declarations required. File the local network declaration before this. |
| Aug 2027 (unconfirmed) | Play likely requires targeting 37; already met. |
| 2027 to Aug 2028 | Decide when to move to SDK 38 and workstream C. |

## Decisions needed from the user

1. Is the local network fix worth a release of its own (1.0.6) soon, or fold it into the next feature release? Recommended: its own release.
2. Does the user (or testers) have a Nextcloud reachable only on a local address, to test with? If not, the emulator test is enough for a first check.
3. When to move to SDK 38, which brings workstream C. Recommended: only when Play requires it, after testing on an Android 18 beta.

## Sources

- Google Play policy preview: https://support.google.com/googleplay/android-developer/answer/16909972
- Android Developers blog, 21 Oct 2026, "A more reliable way to add and edit contacts on Android": https://developer.android.com/blog/posts/reliable-way-to-add-and-edit-contacts-android
- Local network permission: https://developer.android.com/privacy-and-security/local-network-permission
- Local network definition: https://developer.android.com/privacy-and-security/local-network-definition
- `MANAGE_CONTACTS` reference: https://developer.android.com/reference/android/Manifest.permission#MANAGE_CONTACTS
- Play target API level requirements: https://support.google.com/googleplay/android-developer/answer/11926878

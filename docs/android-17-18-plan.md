<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Plan: Android 17 and 18, and Google Play's new contacts and local network rules

Drafted 2026-10-29 for the user to review. Nothing here has been built. Dates marked (est.) are third-party
estimates, not announced by Google; check developer.android.com before relying on them.

## What is going on

Three separate changes, with different deadlines. Decision 2026-10-29: workstream A is not being built.

| # | Change | Applies to Corvid | When |
|---|---|---|---|
| A | **Local network permission.** Connections to local network addresses are blocked by default for apps that target SDK 37, unless the app declares and requests `ACCESS_LOCAL_NETWORK`. | **Possibly, for servers at a local address. Not being addressed** (see workstream A). | Enforced on Android 17 devices already. The Play declaration applies only to apps that declare the permission |
| B | **Play contacts policy.** Broad contacts access (`READ_CONTACTS`) is restricted to apps whose core function needs it. | Already declared (October 2026). Watch for follow-up. | Jan 27, 2027 |
| C | **`WRITE_CONTACTS` stops working for direct writes**, and `MANAGE_CONTACTS` replaces it for contact managers. | Only once Corvid targets SDK 38. The sharing feature is all direct writes. | Android 18 (est. mid 2027); Play declaration from Aug 25, 2027 for targets 38+ |

Also on the Play preview page and not affecting Corvid: precise location (Corvid requests no location) and AppFunctions.

## What you have to test with (inspected 2026-10-29)

- **Build:** `compileSdk` 37, `targetSdk` 37, `minSdk` 28, AGP 9.4.1, Kotlin 2.4.20. Android Studio build 262 (Panda).
- **Platforms installed:** 33, 34, 35, 36 and 37.0. **System image:** Android 17 QPR1 (37.1, Google APIs with Play Store, 16 KB pages) for the AVD `Pixel_10_Pro`. 
- **Your phone** is on Android 17 (it showed this earlier). It was not connected when this was written, so its exact build was not re-read.
- **Not installed yet:** Android 17 QPR2 (37.2, where `MANAGE_CONTACTS` appears) and any Android 18 preview. The command line `sdkmanager` is not installed either, so installs go through Android Studio's SDK Manager. Check it for a 37.2 platform and system image now and again each month.
- **Android 18 (API 38):** no official schedule found. Estimates: first beta around Feb 2027, platform stability around Mar 2027, stable around Jun 2027 (est.). Preview SDKs usually need Android Studio's preview channel.

## Workstream A: local network (decided: not building)

Decision 2026-10-29: the developer does not want to support servers at local network addresses. The use case is small (many
Nextcloud users expose the server publicly), and it does not affect the developer. Corvid will not declare or request
`ACCESS_LOCAL_NETWORK`, and the Play declaration for it will not be needed because the app will not declare the permission.

**What this means for users.** Android 17 blocks connections to local addresses for apps that target SDK 37 unless the permission
is requested. This was never tested, so it is an assumption that a server at a private address now fails to connect (a
timeout, probably). Per Google's definition page, local means 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, link-local,
CGNAT 100.64.0.0/10, multicast and broadcast, on Wi-Fi or Ethernet. Cellular and VPN connections (such as Tailscale) are excluded, so
those still work. Servers reached over the internet are unaffected, which includes the developer's own, and the developer's
fully updated Android 17 phone syncs fine.

**Workarounds for an affected user, if one asks:** reach the server through a VPN such as Tailscale, or through its public address.

**If this is revisited later** (for example a user reports it), the cost estimate is 1 to 2 days: declare the permission, detect a local
address before connecting, ask in context, handle denial with a clear message, and add translations and tests. Confirm the blocking
first on the `Pixel_10_Pro` emulator (host at `10.0.2.2`; compare against
`adb shell am compat disable RESTRICT_LOCAL_NETWORK dev.benica.corvidcontacts.debug`).

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
| Now to mid Nov 2026 | Check SDK Manager for 37.2. |
| Early Jan 2027 | Check Play Console messages before B takes effect. |
| Jan 27, 2027 | Contacts permission policy effective. Re-check. |
| Feb to Mar 2027 (est.) | Android 18 beta and platform stability: set up the preview emulator and try the mirror on it. |
| Jun 2027 (est.) | Android 18 stable. |
| Aug 25, 2027 | `MANAGE_CONTACTS` declaration required for targets 38+. The local network declaration does not apply because Corvid will not use the permission. |
| Aug 2027 (unconfirmed) | Play likely requires targeting 37; already met. |
| 2027 to Aug 2028 | Decide when to move to SDK 38 and workstream C. |

## Decisions

1. Local network support: **no** (decided 2026-10-29). Revisit only if users report it.
2. When to move to SDK 38, which brings workstream C. Recommended: only when Play requires it, after testing on an Android 18 beta.

## Sources

- Google Play policy preview: https://support.google.com/googleplay/android-developer/answer/16909972
- Android Developers blog, 21 Oct 2026, "A more reliable way to add and edit contacts on Android": https://developer.android.com/blog/posts/reliable-way-to-add-and-edit-contacts-android
- Local network permission: https://developer.android.com/privacy-and-security/local-network-permission
- Local network definition: https://developer.android.com/privacy-and-security/local-network-definition
- `MANAGE_CONTACTS` reference: https://developer.android.com/reference/android/Manifest.permission#MANAGE_CONTACTS
- Play target API level requirements: https://support.google.com/googleplay/android-developer/answer/11926878

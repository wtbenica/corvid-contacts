<!-- SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0 -->

# Test review and coverage audit

Done 2026-10-05 on branch `1.0.5` (commit `4d64a28`). The review, the gap list and a recommended order
follow; the status below says how far the work has got on branch `test-coverage`.

## Status (updated 2026-10-06, after gap 7)

| Item | State |
| --- | --- |
| Cleanup | Done |
| 1. Provider tests and reader split | Done: `SystemContactRowsTest` (JVM) and 13 instrumented tests against the real provider |
| 2. `ContactMerger` | Done |
| 6. Migrations | Done: real schemas 19 to 26, exported to `app/schemas` |
| 4. `BirthdayWorker` | Done: `BirthdayDates` extracted and tested; year-less and Feb 29 now remind |
| 5. `IntentParser` | Done: found and fixed a shared street address being dropped |
| 3. `ContactsRepository` | Done: `ContactsRepositoryLocalTest` (16), `ContactsRepositoryServerTest` (20, MockWebServer with a routing dispatcher), shared `RepositoryTestBase` and `DavFixtures` |
| 7. View models | Done: `MainViewModel`, `OnboardingViewModel`, `LoginViewModel`, `SettingsViewModel`, `ContactsViewModel` and its filter and selection helpers, 133 tests. Found and fixed three bugs (see the changelog) |
| 8. Compose UI | **Next** |
| 9. Lower value | Not started |

Left out of gap 3 on purpose: the photo and phone repair paths inside sync (covered in part by
`ContactsRepositorySyncTest`) and `importVCardText` with remote photo downloads (needs a photo server).
Each new test was checked by breaking the code it covers and watching it fail.

## The numbers

- **330 unit tests in 24 files, about 1 minute 55 seconds, all passing** (each test class runs in its own JVM, see below).
- **Line coverage is about 10%** (2,248 of 22,349 lines). Nearly all of it is the pure logic in
  `data/system`, plus `DavParser`, `VCardMapper` and `SystemVisibility`.
- Measured with `enableUnitTestCoverage = true` on the debug build type, and the Robolectric tests
  counted (see the end of this file). Without the Robolectric setting the repository tests show 0% and
  the total is 4%.

| Area | Lines covered | Notes |
| --- | --- | --- |
| `data/system` pure logic (`SystemEditMerge`, `MirrorPlan`) | 92%, 83% | the best tested code in the app |
| `VCardMapper`, `DavParser` | 88%, 94% | the vCard round trip and the server parser |
| `ContactsRepository` | 29% | sync and delete propagation only |
| `data/system` provider side (`SystemContactsReader`, `MirrorWriter`, `SystemEditAbsorber`, `SystemContactsMirror`, `MirrorDataRows`, `MirrorPhotos`, `MirrorGroups`) | 2% to 31% | only ever checked by hand on a phone |
| `ContactMerger`, `IntentParser`, `BirthdayWorker`, `SyncWorker` | 0% | |
| The five view models and their helpers | tested in gap 7 | `ViewModelTestBase` builds them over in-memory Room |
| All Compose UI | under 1% | there are no Compose tests |

## Review of what exists

- **`SystemEditMergeTest` (35) and `MirrorPlanTest` (32):** good. They are fast, name the behavior, and
  use small builders. Nothing to change. A few merge tests repeat the same set-up and could share a
  helper, but that is taste.
- **`ContactsRepositoryVCardRoundTripTest` (6) and `ContactsRepositorySyncTest` (3):** the right kind of
  test (a mock web server and real mapping code). Problems:
  - There are four hand-written DAO fakes between them (`FakeContactDao`, `FakeAddressBookDao`,
    `NoOpContactDao`, `NoOpAddressBookDao`). Every new DAO method has to be added to all four, which
    happened twice during the sharing work. Room's in-memory database under Robolectric would do the
    same job with the real queries and nothing to keep in step.
  - One test, "PhoneFormatter reformats a raw number", is in the sync test file but tests formatting.
- **`AppDatabaseMigrationTest` (5):** it only proves that Room accepts the migrated tables. The old
  schemas are hand-typed SQL, and `exportSchema` is `false`, so there are no real old schemas to test
  against. The set-up is copied five times.
- **`ContactColorsTest` (4):** fine, but the same long `ContactEntity(...)` is built three times, and one
  test name ("when no book missing") is garbled.
- **`FarthestHueTest` (4), `SystemVisibilityTest` (4), `StructuredAddressTest` (4):** good, small, exact.
- **`ExampleInstrumentedTest`:** the Android Studio template. It asserts the package name
  `dev.benica.corvidcontacts`, but the debug build's is `dev.benica.corvidcontacts.debug`, so it would
  fail on a device. It tests nothing about the app.
- No sleeps, no ignored tests, no flaky timing. All tests are deterministic.

## Gaps, most important first

1. **The provider half of two-way sync** (reader, writer, absorber, mirror, data rows, photos, groups).
   This code reads and writes other apps' data and decides what is overwritten. Every behavior was
   verified by hand with `adb` and Google Contacts: sharing writes the rows, an edit is absorbed, a delete
   hides, the version guard skips a contact, unknown rows are kept, a photo token detects a changed
   photo, a level change rewrites, losing the permission removes the copy. None of it will notice a
   regression. Best approach: **instrumented tests on an emulator**, using the real `ContactsContract`
   provider, an in-memory Room database, a fake editor, and `GrantPermissionRule` for the contacts
   permissions; the "other app" is a plain `ContentResolver` update (which the real provider marks
   dirty). Also split `SystemContactsReader`'s row-to-contact mapping from the cursor reading, so the
   column mapping (the easiest thing to get subtly wrong) can be unit tested on the JVM.
2. **`ContactMerger` (0%).** Merging duplicates changes and deletes the user's contacts. Pure logic;
   cheap to test thoroughly.
3. **`ContactsRepository` (29%).** Saving a contact (server and local), importing vCards, uploading a
   local book, creating a book, the photo and phone repair, logout and local-only mode.
4. **`BirthdayWorker` (0%).** The date parsing, the known gap that year-less birthdays never trigger a
   reminder, and the notification timing. Mostly pure.
5. **`IntentParser` (0%).** It parses input from other apps. Test with crafted intents and vCards,
   including garbage.
6. **Database migrations.** Turn on schema export and use Room's `MigrationTestHelper`, so each step is
   checked against the real older schema, with data.
7. **View models (0%).** `OnboardingViewModel` (which steps are skipped when), `MainViewModel` (the start
   destination), `LoginViewModel`, `SettingsViewModel` and `ContactsViewModel` (574 lines). Needs fakes
   or in-memory Room.
8. **Compose UI (0%).** A few Robolectric Compose tests where the screen has logic: the sharing rows
   (disabled and dimmed states), the hidden-contact card and menu, the fill-in-address dialog states,
   and the sharing step's permission-denied path.
9. **Lower value:** Photon response parsing in `GeocoderRepository` (cheap, canned JSON),
   `SettingsRepository` round trips, `PhotoManager` file handling, the glance widget, `VCardType`,
   the colour extensions.

## Cleanup to do first (small)

1. Delete `ExampleInstrumentedTest`, or replace it with the first real instrumented test.
2. Replace the four DAO fakes with in-memory Room.
3. Move the PhoneFormatter test into its own file; share the migration test set-up; add a
   `ContactEntity` builder to `ContactColorsTest` and fix the test name.
4. Keep the coverage settings (below) in `app/build.gradle.kts`.

## Suggested order

Cleanup, then 1 (the instrumented provider tests and the reader split), 2, 6, 4, 5, 3, then 7 and 8.
Items 1, 2 and 6 protect data the most; the rest are about confidence.

## Measuring coverage

Add to `app/build.gradle.kts`: `enableUnitTestCoverage = true` in the `debug` build type, and inside
`testOptions { unitTests { ... } }`:

```kotlin
all {
    it.extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class.java) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
```

Then run `./gradlew createDebugUnitTestCoverageReport` and open
`app/build/reports/coverage/test/debug/index.html` (or read `report.xml` there). The second block is what
makes the Robolectric tests count.

## View model tests: what to know (gap 7)

- `ViewModelTestBase` (in `ui/`) extends `RepositoryTestBase`: real repositories over in-memory Room,
  `Dispatchers.Main` replaced so `viewModelScope` runs eagerly, WorkManager initialized (saving the
  birthday setting schedules a job), and the settings the tests change reset first. Make view models with
  `createViewModel { ... }`, which clears them after the test; one left running keeps reacting to the
  shared settings and undid a later test's set-up. `await` waits for a flow value, `awaitUntil` polls,
  `record()` collects one-shot events, and `signedInAccount()` gives a signed-in account on a local mock
  server (a test must not sync against the internet).
- **Each test class runs in its own JVM** (`forkEvery = 1` in `app/build.gradle.kts`). The DataStores behind
  settings and login are process-wide, and view model tests that shared a JVM failed at random. With it,
  repeated clean runs of the whole suite pass.
- Waiting on a stored-setting flow right after a write can occasionally miss the change. Where a test
  was flaky for that reason, it polls the stored value directly.
- Each test was checked by breaking the code it covers and watching it fail; four tests that passed with
  the code broken were rewritten.
- Bugs these tests found, all fixed and in `CHANGELOG.md`: an unreachable server at sign-in said
  "Authentication failed (Status: 401)"; renaming a group only renamed it on the contacts on screen; and
  merging only repointed links on the contacts on screen.
- Left alone, noticed: sign-in reports every refusal as status 401 whatever the server said, and a server
  with a bad certificate is reported the same way; `SettingsViewModel.alwaysAddCountryCode` starts at
  `false` while the stored default is `true`, so the switch can flash off while it loads.

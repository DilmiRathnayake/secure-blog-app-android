# ConnectHub — Test Plan and Actual Results

**Build tested:** debug build of commit `3b55efb` plus the delete-dialog wording fix (described below), AGP 8.13.0, compileSdk/targetSdk 36, minSdk 24.
**Environment:** Android Emulator "ConnectHub_API36" (Pixel 8 profile, Android 16 / API 36, Google APIs image, virtual camera = VirtualScene), running on macOS. The app was **not** tested on a physical phone.
**Dates:** 12 October 2026.

## Status key
| Status | Meaning |
|---|---|
| **Pass** | Executed and the actual result matched the expected result |
| **Partial** | Some of the expected behaviour was verified; the rest was not executed |
| **Not tested** | Implemented but not executed (the reason is given) |
| **Fail** | Executed and did not behave as expected |

Test types: **M** = manual test on the emulator (screenshot in `evidence/`), **I** = instrumented test (`app/src/androidTest`, run with `gradlew connectedDebugAndroidTest`), **U** = JVM unit test (`app/src/test`, run with `gradlew testDebugUnitTest`).

## Automated test summary
| Suite | Tests | Passed | Failed | Report |
|---|---|---|---|---|
| JVM unit tests | 24 | 24 | 0 | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Instrumented tests (API 36 emulator) | 23 | 23 | 0 | `app/build/reports/androidTests/connected/debug/index.html` |

The instrumented tests ran with the emulator's animations turned off, as Espresso recommends. Earlier in development, one UI test (`mainScreenTabsOpen`) failed because the Gallery screen crashed. The cause was a grid layout animation that RecyclerView does not support. After the fix, all 23 tests passed.

## Manual tests (airplane mode)
Tests M-01 to M-10 were run with **airplane mode on**: the status bar shows the aeroplane icon and the Home screen chip shows "Offline mode" (E01).

| ID | Requirement / rubric criterion | Procedure | Expected result | Actual result | Status | Evidence |
|---|---|---|---|---|---|---|
| TC-01 | Create post (Offline mgmt, 40) | Home → New post → enter title and text → Save offline | Post saved and shown first in the list | Post saved; "Post saved on this device" message; appeared in list | Pass | E03, E04; I: `insertAndReadBack`, `emptyFormShowsErrorsThenValidPostIsSaved` |
| TC-02 | Validation (Offline mgmt) | Open New post and press Save offline with empty fields | Inline errors; nothing saved | "Please enter a title" and "Please write something in your post" shown | Pass | E02; U: `PostValidatorTest` (4); I: UI test |
| TC-03 | Edit post (Offline mgmt) | Open post → Edit → change text → Save changes | Same post updated, no duplicate, "Last edited" shown | Text updated, created date unchanged, "Last edited" added, list count unchanged | Pass | E06a, E06b; I: `updateKeepsIdAndDoesNotDuplicate` |
| TC-04 | View single post (Offline mgmt) | Tap a post in the list | Full title, text, image, dates and status shown | Detail screen showed all fields | Pass | E05 |
| TC-05 | View list (Offline mgmt) | Open Posts tab | All posts newest first with thumbnail, preview, date, status | 5 posts listed with badges (Offline / Draft) | Pass | E04; I: `listIsNewestFirst` |
| TC-06 | Delete single post (Offline mgmt) | Post detail → Delete → confirm | Confirmation dialog; post removed | Dialog shown; list fell from 5 to 4 posts | Pass | E08a, E08b; I: `deleteSingle` |
| TC-07 | Delete selected group (Offline mgmt) | Posts → Select → tick 2 posts → Delete → confirm | Only the selected posts deleted | "2 selected" bar; dialog "Delete 2 posts?"; list fell from 6 to 4; "2 posts deleted" | Pass | E09a–E09d; I: `deleteManyRemovesOnlySelected` |
| TC-08 | Search by title (Offline mgmt) | Search "SUN" | Partial, case-insensitive matches listed | "2 posts match 'SUN'": title match "Sunrise…" listed first | Pass | E07a; I: `searchMatchesTitleAndBodyPartialCaseInsensitive` |
| TC-09 | Search by body text | Same search | Body-only matches also listed | "Evening in Galle Fort" matched through "sunset" in its body text | Pass | E07a |
| TC-10 | No search results | Search "zebra" | Clear empty state | "No matching posts" message shown | Pass | E07b |
| TC-11 | SQLite persistence after restart (SQLite, 10) | Force-stop app process (`am force-stop`) → relaunch | All posts still present | Process confirmed stopped; Home showed same 4 posts and counts | Pass | E10; I: `dataSurvivesReopeningDatabase` |
| TC-12 | Offline operation | TC-01 to TC-11 performed in airplane mode | All core operations work offline | All passed with airplane mode on | Pass | E01–E10 |
| TC-13 | Gallery image: JPEG, PNG, WebP (Photo, 10) | Editor → Gallery → choose image in Photo Picker | Preview shown; image saved with post | PNG, WebP and JPEG test images attached and saved; thumbnails in list | Pass | E03, E04; I: `importsJpeg`, `importsPng`, `importsWebp`, `largeImageIsScaledDown` |
| TC-14 | Camera image (Photo) | Gallery tab → Camera upload → shutter → confirm → Save | Camera opens; captured photo attached and saved | Emulator camera opened; photo attached; post appeared in Gallery grid | Pass (emulator camera only) | E11a–E11c (captured in an earlier session on the same build, online) |
| TC-15 | Edit post that has an image | Edit a post with photo, change text, save | Photo kept | Photo retained after save | Pass | E06a, E06b |
| TC-16 | Remove attachment | Editor → Remove photo → Save; or Gallery → preview → Remove image | Post saved without photo; file deleted | Repository-level removal verified by test; UI removal not recorded as evidence | Partial | I: `removeImageDetachesPhoto` |
| TC-17 | Share text-only post (Sharing, 10) | Detail → Share | ACTION_SEND with subject and text | Intent contents verified by test; share sheet opened (E12); delivery to an email app not tested because no email account is set up on the emulator | Partial | E12; I: `textOnlyPost` |
| TC-18 | Share post with image | Detail → Share → Email / More | Intent includes image `content://` URI with read grant | Verified by test (EXTRA_STREAM, ClipData, FLAG_GRANT_READ_URI_PERMISSION); actual email not sent | Partial | E12; I: `photoPostAttachesStreamWithReadGrant` |
| TC-19 | Publish one post to Blogger (Upload, 10) | Configure Google Cloud OAuth client → connect account → Upload | Post published; URL shown; badge "Uploaded" | **Not executed.** Needs a Google Cloud project, an Android OAuth client with the PC's SHA-1, and a Blogger blog (see `docs/PUBLISHING_SETUP.md`) | Not tested | — |
| TC-20 | Publish selected group | Upload tab → tick 2 posts → Upload 2 posts | Each post uploaded in turn | Flow verified in **Test mode only** (simulated, nothing sent online): progress ring, per-post rows, success animation | Partial (simulated) | E14a–E14c |
| TC-21 | Failed upload not reported as success | Test mode → "Simulate an upload failure" → Upload | Rows marked failed; Retry offered; no "Uploaded" badge | Implemented; not executed in this session | Not tested | U: `classify_mapsStatusCodes`, `parseError_*` (HTTP error mapping only) |
| TC-22 | Picker cancel / camera failure | Open picker or camera, then press Back | "No photo selected / No photo was taken" message, no crash | Not executed | Not tested | — |
| TC-23 | Nothing-selected validation | Selection mode with 0 selected → Delete | Message instead of empty dialog | "Select at least one post first" shown | Pass | E13a |
| TC-24 | Swipe to delete with Undo | Swipe a card left → Undo | Post hidden, then restored | Snackbar shown; after Undo the list still had 4 posts including the swiped one | Pass | E15 |
| TC-25 | Database upgrade v1 → v2 | Create a v1 database, open with current helper | Old posts kept; new column defaults to "not draft" | Passed | Pass | I: `upgradeFromVersion1KeepsExistingPosts` |
| TC-26 | SQL safety in search | Search `50%` and `' OR '1'='1` | Treated as literal text; table intact | Passed | Pass | U: `SearchQueryTest` (5); I: `searchTreatsWildcardsAndQuotesLiterally` |
| TC-27 | Invalid image file rejected | Import a text file renamed `.jpg` | Error, no crash | IOException raised and handled | Pass | I: `nonImageIsRejected` |
| TC-28 | Different screen sizes / themes | Run on other screen sizes; dark mode | Layout readable | Dark mode checked on Pixel 8 profile (E16); other screen sizes not tested | Partial | E16 |

## Defects found during testing
| Defect | Found by | Fix | Re-test |
|---|---|---|---|
| Gallery tab crashed (`ClassCastException`: grid layout animation used on a RecyclerView) | Emulator testing + failing UI test | Replaced with the list layout animation | UI test passes; Gallery opens (E11c) |
| Delete dialog said "and its photo" for posts without a photo (E08a) | Manual test TC-06 | Separate message for posts with and without photos | Build and unit tests pass; dialog not re-captured because the emulator session ended |
| Headings rendered in regular weight | Visual check | Theme-wide font removed so text styles choose weights | Visual re-check passed |

## Outstanding evidence to collect
1. TC-19 / TC-20: real Blogger publishing after completing `docs/PUBLISHING_SETUP.md`. Capture the upload sheet showing the Blogger URL and the published post open in a browser.
2. TC-17 / TC-18: send one text-only post and one post with a photo to a real email app (for example Gmail with a signed-in account), and capture the received email.
3. TC-21 and TC-22: capture the simulated failure sheet with Retry, and the cancel messages.
4. TC-14 on a physical phone, if possible.

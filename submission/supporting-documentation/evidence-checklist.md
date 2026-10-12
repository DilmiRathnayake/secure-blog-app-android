# Evidence Checklist

Every screenshot in `evidence/` is a real capture (`adb exec-out screencap`) from the running app on the Android 16 (API 36) emulator. None are mock-ups. Files E01–E10 and E13a were captured with **airplane mode on**. E11, E12 and E14–E16 come from an earlier session on the same day and build, with the network on.

| # | Evidence item | File(s) | Status |
|---|---|---|---|
| 1 | Application home screen | `E01_home_airplane_mode.png`, `E16_dark_mode.png` | ✅ Captured |
| 2 | Post creation | `E03_create_post_gallery_png.png`, `E04_post_list_airplane_mode.png` | ✅ Captured |
| 3 | Post editing | `E06a_edit_prefilled.png`, `E06b_after_edit.png` | ✅ Captured |
| 4 | Individual post viewing | `E05_view_post.png` | ✅ Captured |
| 5 | Search results | `E07a_search_results.png`, `E07b_search_no_results.png` | ✅ Captured |
| 6 | Single-message deletion | `E08a_delete_single_confirm.png`, `E08b_after_single_delete.png` | ✅ Captured (dialog wording since corrected; see test plan) |
| 7 | Bulk-selection deletion | `E09a_before_bulk_delete.png` → `E09d_after_bulk_delete.png` | ✅ Captured |
| 8 | SQLite persistence after restart | `E10_persistence_after_restart.png` | ✅ Captured (after `am force-stop`) |
| 9 | Gallery image attachment | `E03_create_post_gallery_png.png` | ✅ Captured |
| 10 | Camera image attachment | `E11a_camera_app_opened.png`, `E11b_camera_photo_attached.png`, `E11c_camera_post_in_gallery.png` | ✅ Captured (emulator virtual camera) |
| 11 | Android share chooser | `E12_share_sheet.png` (in-app share sheet) | ⚠️ Partial: the system chooser ("More") and a received email still need capturing |
| 12 | Actual online publishing result | — | ❌ Missing: needs Blogger setup (`docs/PUBLISHING_SETUP.md`). `E14a–c` show **Test mode only** and must not be presented as real publishing |
| 13 | Error-handling behaviour | `E02_validation_errors.png`, `E13a_nothing_selected_validation.png`, `E07b_search_no_results.png` | ⚠️ Partial: upload-failure sheet and "Connection lost" dialog not yet captured |
| — | Extra: swipe delete + Undo | `E15_swipe_delete_undo_snackbar.png` | ✅ Captured |

Report figures in `figures/` are built only from the files above (Figure 4 and Figure 5) and from the design diagrams (Figures 1–3).

## How to capture the missing evidence (Windows, Android Studio)
Use the emulator's **camera icon** in its side toolbar (Take screenshot), or press **Ctrl+S** in the emulator window. On a phone, press **Power + Volume down**.

1. **Real Blogger publishing (item 12)**
   1. Follow `docs/PUBLISHING_SETUP.md` (Google Cloud project, Blogger API, Android OAuth client with *your PC's* SHA-1, test user, Blogger blog).
   2. Use an emulator with a **Google APIs / Google Play** image and sign in with the blog's Google account.
   3. App → Upload → Settings → Blogger → **Connect Google account & choose blog**. Screenshot the connected card.
   4. Upload tab → tick a post → **Upload 1 post** → screenshot the sheet showing the Blogger URL.
   5. Open the URL in a browser → screenshot the live post. Post detail should show the green **Uploaded** badge → screenshot.
   6. Repeat with two posts to evidence group upload.
2. **Share chooser and email (item 11)**
   1. Sign in to Gmail on the emulator or phone.
   2. Open a post with a photo → Share → **More** → screenshot the Android chooser.
   3. Choose Gmail → screenshot the draft with the subject, text and attached photo. Send it to yourself and screenshot the received email.
   4. Repeat with a text-only post.
3. **Error handling (item 13)**
   1. Upload → Settings → **Test mode** → turn on **Simulate an upload failure** → upload a post → screenshot the red failed row and **Retry failed**.
   2. Switch back to Blogger mode, turn on **airplane mode**, tap Upload → screenshot the **Connection lost** dialog.
   3. Open the photo picker and press Back → screenshot "No photo selected".
4. Re-capture the single-delete dialog for a post *without* a photo, to show the corrected wording.

Save new files into `evidence/` using the same numbering (for example `E17_blogger_published.png`) and update this table.

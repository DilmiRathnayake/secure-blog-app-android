# Demonstration Video Script (about 4 min 45 s)

**Before recording**
- Fresh install of the debug APK on the emulator or phone; a few sample photos in the gallery.
- Blogger set up and connected (`docs/PUBLISHING_SETUP.md`). If it is **not** set up, do segment 7 in Test mode and **say clearly** that the upload is simulated.
- An email app signed in, for segment 6.
- Screen recorder: Android Studio emulator → ⋯ → Record and Playback, or Windows **Win+Alt+R** (Xbox Game Bar), or OBS.
- Speak slowly; keep the cursor still while talking.

| Time | Rubric criterion | On screen (actions) | Narration (say in your own words) |
|---|---|---|---|
| 0:00–0:20 | Intro | Launch app: splash → onboarding → Skip → profile name → Home | "This is ConnectHub, a native Android blogging app written in Java with XML layouts. Posts are stored in SQLite on the device, so it works without internet." |
| 0:20–0:35 | Offline | Swipe down the notification shade → turn on **Airplane mode** → Home shows "Offline mode" | "I'll switch on airplane mode to show that all post management works offline." |
| 0:35–1:20 | Create, gallery photo, view | New post → title + text → **Gallery** → pick photo → preview → **Save offline** → list → open post | "I create a post, attach a photo from the gallery through the system photo picker, and save it. The app copies the photo into private storage. Here is the post on its own screen." |
| 1:20–1:45 | Camera | Gallery tab → **Camera upload** → take photo → confirm → title/text → Save | "Photos can also come from the camera. The camera app writes into a FileProvider URI, so the app doesn't need camera or storage permission." |
| 1:45–2:10 | Edit + validation | Open a post → Edit → change text → Save changes → show "Last edited". New post → Save with empty fields → errors | "Editing updates the same record. Empty titles or text are rejected with clear messages." |
| 2:10–2:35 | Search | Search bar → type part of a word in capitals → results highlighted → type a missing word → "No matching posts" | "Search covers titles and text, ignores case and matches part of a word." |
| 2:35–3:10 | Delete single + group | Post → Delete → confirm. Posts → Select → tick two → Select all / clear → Delete → confirm. Swipe a card left → Undo | "I can delete one post with confirmation, or select a group and delete them together." |
| 3:10–3:25 | Persistence | Recent apps → swipe app away → reopen → posts still there | "After closing and reopening, every post is still there because it's stored in SQLite." |
| 3:25–3:50 | Sharing | Turn airplane mode **off** → open post with photo → Share → **Email** → show subject, text and attachment (send it) | "Sharing uses Android's standard ACTION_SEND intent with a temporary read permission for the photo." |
| 3:50–4:30 | Upload | Upload tab → show destination "Blogger · your blog" → tick one post → Upload → progress → URL → open in browser. Then select two posts → Upload | "Posts are published to my Blogger blog through the official Blogger API after Google sign-in. A post is only marked Uploaded after Blogger confirms it. Group upload works the same way." |
| 4:30–4:45 | Close | Profile tab → stats; toggle dark mode | "To summarise: offline management, SQLite storage, gallery and camera photos, intent sharing and online publishing. Thank you." |

**If Blogger is not set up:** at 3:50 show Upload → Settings → **Test mode**, run an upload, and say "This is a simulated test run; nothing is sent online." Don't describe it as publishing.

**After recording:** trim it to 4–5 minutes, export as MP4 (1080p), and play it back once to check the audio.

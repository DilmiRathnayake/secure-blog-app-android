# Rubric Traceability Matrix

This maps each official marking criterion (`Rubric_6CS027.docx`) to the code, report section, and test evidence. It does **not** predict a mark; it shows where evidence is strong and where it is still missing.

| Criterion (marks) | Implementation components | Report section | Test evidence | Remaining gap |
|---|---|---|---|---|
| **Offline message management: create, edit, view, delete; view single and list; delete single and group; search (40)** | `EditPostActivity`, `PostDetailActivity`, `PostsFragment`, `HomeFragment`, `SearchActivity`, `PostAdapter` (multi-select), `ui/widget/SwipeActions`, `PostValidator`, `PostRepository.search/delete/deleteMany` | §2, §3, §6 | TC-01–TC-12, TC-23, TC-24 (manual, airplane mode, E01–E10, E13a, E15); instrumented `PostRepositoryTest` (12), `CreatePostUiTest` (2) | Physical-device run not done; demonstrate in the video |
| **SQLite storage (10)** | `BlogDbHelper` (SQLiteOpenHelper, schema v2 + `onUpgrade`), `PostRepository` (bound parameters, transactional bulk delete), `Post` | §4 | TC-11 (E10 restart), TC-25 (v1→v2 upgrade), TC-26 (SQL safety); instrumented `dataSurvivesReopeningDatabase` | None identified for the implemented scope |
| **Photo from gallery and camera (10)** | `EditPostActivity` (PickVisualMedia, TakePicture), `ImageStorage` (copy, EXIF rotate, scale, JPEG), FileProvider (`res/xml/file_paths.xml`), `GalleryFragment`, `ImagePreviewActivity` | §4, §5 | TC-13 JPEG/PNG/WebP (E03 + instrumented), TC-14 camera (E11, emulator virtual camera), TC-15, TC-27 | Remove-photo UI (TC-16) and cancel paths (TC-22) not captured; HEIC not tested; camera not tested on a physical device |
| **Share individual message via Android intent (10)** | `ShareHelper` (ACTION_SEND, EXTRA_TEXT/SUBJECT/STREAM, ClipData, read grant, email selector), `ShareSheet`, `ShareText` | §5 | `ShareHelperTest` (2 instrumented), `ShareTextTest` (2 unit), E12 share sheet | **Delivery to an email app not yet shown**: capture a received email with text and photo (TC-17/18) |
| **Upload one or a group of messages to a personal account (10)** | `PublishController`, `BloggerPublishService`, `BloggerApi` (Blogger API v3 REST), `GoogleAuthHelper` (OAuth 2.0 via AuthorizationClient), `PublishSettings`, `UploadFragment`; `MockPublishService` for Test mode | §3, §5, §7 | Unit: `BloggerApiParsingTest` (5), `BloggerHtmlTest` (3); Test-mode flow E14a–c | **Real publishing not yet verified.** Complete `docs/PUBLISHING_SETUP.md`, publish one post and a group, and record it (TC-19/20) |
| **Supporting documentation and video (20)** | `6CS027_Supporting_Report.docx/.pdf`, `software-design/`, `test-plan-and-results.md`, `evidence/`, `demo-video-script.md` | whole report | Word count verified (see report notes) | **The 4–5 minute video has not been recorded yet.** Follow `demo-video-script.md` |

## Highest-value actions before submission
1. Configure Blogger and record a real upload (single and group). This is the only criterion with no live evidence.
2. Email one post with a photo to yourself and capture the received message.
3. Record the demonstration video.
4. Re-run `gradlew connectedDebugAndroidTest` on your own PC and keep the HTML report as extra evidence.

# ConnectHub — offline & online social blogging (Android)

6CS027 Secure Mobile Application Development portfolio project.
Native Android app in **Java** with **XML layouts**, **SQLite** storage, gallery/camera photos,
Android share intents and publishing to **Blogger**.

| Home | Posts | Editor | Upload | Dark mode |
|<img width="100" height="200" alt="E01_home_airplane_mode" src="https://github.com/user-attachments/assets/13577a33-c9c1-4cf9-932d-238c64297b70" /> |<img width="100" height="200" alt="E03_create_post_gallery_png" src="https://github.com/user-attachments/assets/29c7ac39-9bb2-43db-b288-65933efa6ce6" />|
|<img width="100" height="200" alt="E14b_upload_confirms" src="https://github.com/user-attachments/assets/e788a7bc-ffb9-4772-b236-1c29039f4237" />|<img width="100" height="200" alt="E06a_edit_prefilled" src="https://github.com/user-attachments/assets/79743233-52c6-428b-aeeb-449b0ad52eb2" />|<img width="100" height="200" alt="E10_persistence_after_restart with dark mode" src="https://github.com/user-attachments/assets/6df2b341-32d4-4ce0-af78-ef5886dbfdc6" />|

All screenshots in `docs/screenshots/` were captured from the running app on an Android 16 (API 36) emulator.

## Features
| Feature | Where |
|---|---|
| Animated splash, 3-page onboarding, local profile (name + photo, no fake login) | `ui/SplashActivity`, `ui/OnboardingActivity`, `ui/ProfileSetupActivity` |
| Dashboard: greeting, online/offline chip, stats (Total / Offline / Uploaded / Drafts), recent posts | `ui/HomeFragment` |
| Create, edit, view and delete posts offline; save as **Draft** | `ui/EditPostActivity`, `ui/PostDetailActivity`, `data/PostRepository` |
| List with filter chips (Newest, Oldest, Uploaded, Offline, Draft), swipe left = delete with Undo, swipe right = edit | `ui/PostsFragment`, `ui/widget/SwipeActions` |
| Multi-select: checkboxes, select all, clear, bulk delete, bulk upload | `ui/PostsFragment`, `ui/PostAdapter` |
| Search titles and bodies (partial, case-insensitive, highlighted) + recent searches | `ui/SearchActivity`, `PostRepository.search` |
| Gallery of all attached photos, full-screen preview, remove photo, camera/gallery shortcuts | `ui/GalleryFragment`, `ui/ImagePreviewActivity` |
| Share sheet (Email, WhatsApp/Facebook/Messenger when installed, Copy, More) | `ui/ShareSheet`, `util/ShareHelper` |
| Upload queue + animated progress sheet (ring, per-post status, success animation, Retry) | `ui/UploadFragment`, `publish/PublishController` |
| Profile: stats, Light/Dark/System theme, publishing account, privacy, about | `ui/ProfileFragment`, `util/AppPrefs` |
| SQLite database (`SQLiteOpenHelper`) | `data/BlogDbHelper`, `data/PostRepository` |
| Attach a photo from the gallery (Photo Picker) or camera (FileProvider) | `ui/EditPostActivity`, `util/ImageStorage` |
| Share a post (text + photo) by email or any app | `util/ShareHelper` |
| Publish one post or a selected group to Blogger | `publish/*` |

## Requirements
* **Android Studio Narwhal 4 Feature Drop (2025.1.4)** or any Narwhal 2025.1.3+ release
* Android Gradle Plugin **8.13.0** and Gradle **8.13** (the wrapper downloads it)
* JDK 17 (Android Studio's bundled JBR is fine)
* compileSdk / targetSdk **36**, minSdk **24** (Android 7.0+)

Narwhal supports AGP up to 8.13, so every library version was chosen to work with AGP 8.13 and
compileSdk 36. If Android Studio offers to upgrade AGP to 9.x, **decline**, because Narwhal cannot
use AGP 9.

## Open and run on Windows
1. Unzip the project, e.g. to `C:\Projects\blog_app`. Avoid very long paths and OneDrive folders.
2. Android Studio → **File → Open**, then select the project folder (the one containing `settings.gradle.kts`).
3. Wait for **Gradle sync** to finish (bottom status bar). The first sync downloads Gradle 8.13 and the
   libraries, so it needs internet. Android Studio creates `local.properties` with your SDK path itself.
4. If asked, install **Android SDK Platform 36** (Tools → SDK Manager → SDK Platforms).
5. Create an emulator: **Device Manager → + → Pixel 8 → API 36 "Google APIs" or "Google Play" image**.
   Google APIs/Play images are needed for Blogger sign-in.
6. Select the `app` run configuration and the device, then press **Run ▶**.

### Command line (optional)
Open Android Studio's **Terminal** tab. It starts in the project folder.

```
gradlew assembleDebug
```
Success looks like `BUILD SUCCESSFUL`, with the APK at `app\build\outputs\apk\debug\app-debug.apk`.

```
gradlew testDebugUnitTest
```
This runs the JVM unit tests. The report is at `app\build\reports\tests\testDebugUnitTest\index.html`.

```
gradlew connectedDebugAndroidTest
```
This runs the on-device tests. An emulator or phone must be running.

### Common problems
| Problem | Fix |
|---|---|
| `SDK location not found` | Open the project in Android Studio once (it writes `local.properties`), or create it with `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` |
| `Unsupported class file major version` / JDK errors | Settings → Build Tools → Gradle → Gradle JDK = **jbr-17** or **jbr-21** |
| "The project is using an incompatible version of the Android Gradle plugin" | You are on an older Studio. Use Narwhal 3/4 (2025.1.3+) |
| Camera button says "No camera app" on an emulator | In the AVD settings, set Camera → Back to **VirtualScene** or **Emulated** |
| Emulator shows "System UI isn't responding" | Emulator graphics are slow: AVD settings → Graphics: **Hardware**, or tap *Wait* |

## Online publishing setup
See [docs/PUBLISHING_SETUP.md](docs/PUBLISHING_SETUP.md). You do this once per Google account and
PC. No secrets are stored in the code. Without setup you can still rehearse the flow in **Test mode**
(Upload → Settings), which is clearly labelled as simulated.

## Project structure
```
app/src/main/java/com/blogspace/app/
  ConnectHubApp     Application: applies saved theme
  data/      Post, BlogDbHelper (schema v2 + migration), PostRepository (all SQL), PostFilter,
             PostValidator, SearchQuery
  ui/        Splash, Onboarding, ProfileSetup, MainActivity + Home/Posts/Upload/Gallery/Profile tabs,
             SearchActivity, PostDetailActivity, EditPostActivity, ImagePreviewActivity,
             PublishSettingsActivity (+PublishFragment), AboutActivity (+AboutFragment),
             ShareSheet, PostAdapter (list + multi-select), Navigation
  ui/widget/ StatusBadge, SwipeActions, Shimmer (loading skeleton), SettingRowView, SquareFrameLayout
  util/      ImageStorage (import/scale/rotate photos), ThumbnailLoader, AvatarHelper, ShareHelper,
             ShareText, AppPrefs, SearchHistory, AppExecutors, UiUtils
  publish/   PublishController (flow + UI), GoogleAuthHelper (OAuth), BloggerApi (REST),
             BloggerPublishService, MockPublishService (Test mode), PublishSettings
app/src/test/         JVM unit tests (24)
app/src/androidTest/  Instrumented tests (23): SQLite incl. v1→v2 migration, images, share intent, UI (Espresso)
docs/                 Design, diagrams, screenshots, publishing setup, licences
```

## Third-party code
All are official Google/AndroidX libraries, declared in `gradle/libs.versions.toml`:
AndroidX AppCompat, Activity, Core, RecyclerView, ConstraintLayout, ExifInterface (Apache 2.0);
Material Components for Android (Apache 2.0); Google Play services Auth (Google APIs ToS);
AndroidX ViewPager2 (Apache 2.0); Material Design icons (Apache 2.0);
Inter typeface by Rasmus Andersson (SIL Open Font License 1.1, see `docs/licenses/Inter-OFL.txt`).
Tests use JUnit 4, AndroidX Test, Espresso and org.json.

## Design system
* Colours: Indigo `#4F46E5`, Purple `#7C3AED`, Violet `#6366F1`, plus success/warning/error status colours
  (`res/values/colors.xml`, dark variants in `values-night/`).
* Typography: Inter (Regular/Medium/SemiBold/Bold) mapped onto the Material 3 type scale (`themes.xml`).
* Shapes: 12 / 16 / 20 / 28 dp corners; 8 pt spacing grid (`dimens.xml`).
* Components: gradient hero headers, glass chips, elevated cards with press-scale animation,
  status badges, skeleton loaders, rounded bottom sheets and dialogs, extended FAB that shrinks on scroll.

## Making the submission ZIP
Before zipping, delete the build outputs, or run **Build → Clean Project**:
`.gradle/`, `.idea/`, `build/`, `app/build/`, `local.properties`, `*.iml`.
Then zip the project folder together with the report (PDF/Word) and the APK
(`app-debug.apk`, copied out of `app/build` before cleaning) if a binary is required.

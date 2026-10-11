# BlogSpace — offline-first Android blogging app

6CS027 Secure Mobile Application Development portfolio project.
Native Android app in **Java** with **XML layouts**, **SQLite** storage, gallery/camera photos,
Android share intents and publishing to **Blogger**.

## Features
| Feature | Where |
|---|---|
| Create, edit, view and delete posts offline | `ui/EditPostActivity`, `ui/PostDetailActivity`, `data/PostRepository` |
| List all posts, multi-select, select all / clear, bulk delete | `ui/PostsFragment`, `ui/PostAdapter` |
| Search titles and bodies (partial, case-insensitive, highlighted) | `ui/SearchFragment`, `PostRepository.search` |
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

## Online publishing setup
See [docs/PUBLISHING_SETUP.md](docs/PUBLISHING_SETUP.md). You do this once per Google account and
PC. No secrets are stored in the code. Without setup you can still rehearse the flow in **Test mode**
(Publish tab), which is clearly labelled as simulated.

## Project structure
```
app/src/main/java/com/blogspace/app/
  data/      Post, BlogDbHelper (schema), PostRepository (all SQL), PostValidator, SearchQuery
  ui/        MainActivity + Home/Posts/Search/Publish/About fragments, PostDetailActivity,
             EditPostActivity, PostAdapter (list + multi-select), Navigation
  util/      ImageStorage (import/scale/rotate photos), ThumbnailLoader, ShareHelper, ShareText,
             AppExecutors (background threads), UiUtils (insets, dates, highlight, connectivity)
  publish/   PublishController (flow + UI), GoogleAuthHelper (OAuth), BloggerApi (REST),
             BloggerPublishService, MockPublishService (Test mode), PublishSettings
app/src/test/         JVM unit tests (20)
app/src/androidTest/  Instrumented tests: SQLite, images, share intent, UI (Espresso)
docs/                 Report, design diagrams, test plan, demo script, publishing setup
```

## Third-party code
All are official Google/AndroidX libraries, declared in `gradle/libs.versions.toml`:
AndroidX AppCompat, Activity, Core, RecyclerView, ConstraintLayout, ExifInterface (Apache 2.0);
Material Components for Android (Apache 2.0); Google Play services Auth (Google APIs ToS);
Material Design icons (Apache 2.0). Tests use JUnit 4, AndroidX Test, Espresso and org.json.

## Making the submission ZIP
Before zipping, delete the build outputs, or run **Build → Clean Project**:
`.gradle/`, `.idea/`, `build/`, `app/build/`, `local.properties`, `*.iml`.
Then zip the project folder together with the report (PDF/Word) and the APK
(`app-debug.apk`, copied out of `app/build` before cleaning) if a binary is required.

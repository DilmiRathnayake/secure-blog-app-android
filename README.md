# ConnectHub — Offline & Online Social Blogging App (Android)

ConnectHub is a native Android social blogging application developed for the **6CS027 – Secure Mobile Application Development** portfolio project.

Built using **Java, XML layouts and SQLite**, ConnectHub allows users to create, manage and store blog posts offline, attach photos using the camera or gallery, share content through Android applications, and publish posts directly to Blogger.

## 📱 Application Preview

<table>
<tr>
<th>Home – Offline Mode</th>
<th>Create Post</th>
<th>Upload</th>
</tr>
<tr>
<td align="center"><img src="https://github.com/user-attachments/assets/13577a33-c9c1-4cf9-932d-238c64297b70" width="180" alt="Home Offline Mode"/></td>
<td align="center"><img src="https://github.com/user-attachments/assets/29c7ac39-9bb2-43db-b288-65933efa6ce6" width="180" alt="Create Post"/></td>
<td align="center"><img src="https://github.com/user-attachments/assets/e788a7bc-ffb9-4772-b236-1c29039f4237" width="180" alt="Upload Confirmation"/></td>
</tr>
<tr>
<th>Edit Post</th>
<th>Dark Mode & Data Persistence</th>
<th></th>
</tr>
<tr>
<td align="center"><img src="https://github.com/user-attachments/assets/79743233-52c6-428b-aeeb-449b0ad52eb2" width="180" alt="Edit Post"/></td>
<td align="center"><img src="https://github.com/user-attachments/assets/6df2b341-32d4-4ce0-af78-ef5886dbfdc6" width="180" alt="Dark Mode Persistence"/></td>
<td></td>
</tr>
</table>

All screenshots were captured from the running application on an **Android 16 (API 36) emulator**.

---

## ✨ Features

| Feature | Description |
|---|---|
| Onboarding & Profile | Animated splash screen, three-page onboarding and local profile setup with name and photo. No fake login system. |
| Home Dashboard | Personalised greeting, online/offline status, post statistics and recent posts. |
| Post Management | Create, edit, view and delete posts, including offline post creation. |
| Draft Management | Save posts as drafts and continue editing later. |
| Post Filtering | Filter by Newest, Oldest, Uploaded, Offline and Draft. |
| Swipe Actions | Swipe left to delete with Undo and right to edit. |
| Multi-selection | Select multiple posts, select all, clear selection, bulk delete and bulk upload. |
| Search | Partial, case-insensitive search across post titles and bodies, with highlighted results and recent search history. |
| Photo Gallery | View all attached photos, preview images in full screen and remove attachments. |
| Camera & Gallery | Attach photos using Android Photo Picker or the device camera through FileProvider. |
| Sharing | Share text and images through Email, WhatsApp, Facebook, Messenger, Copy and other installed applications. |
| Upload Queue | Upload queue with animated progress, individual post statuses, success animation and retry functionality. |
| Blogger Publishing | Publish individual posts or selected groups of posts directly to Blogger using Google authentication and REST API. |
| Profile & Settings | View statistics, manage publishing account, privacy, application information and theme preferences. |
| Theme Support | Light, Dark and System appearance modes. |
| Offline Storage | SQLite-based local storage with persistent posts and image attachments. |

---

## 🛠️ Technologies & Tools

| Category | Technologies |
|---|---|
| Programming Language | Java |
| UI Development | XML Layouts |
| Platform | Native Android |
| Local Database | SQLite |
| Database Management | SQLiteOpenHelper |
| UI Components | AndroidX, Material Components |
| Authentication | Google Play Services Auth (OAuth 2.0) |
| Publishing API | Blogger REST API |
| Image Handling | Android Photo Picker, Camera, FileProvider |
| Image Processing | ExifInterface, ImageStorage |
| Background Operations | AppExecutors |
| Testing | JUnit 4, AndroidX Test, Espresso |
| Development Environment | Android Studio Narwhal |
| Build System | Gradle 8.13, Android Gradle Plugin 8.13.0 |
| Version Control | Git & GitHub |

---

## ⚙️ Requirements

Before running the application, ensure that the following tools are installed:

- **Android Studio:** Narwhal 4 Feature Drop (2025.1.4) or any Narwhal 2025.1.3+ release.
- **Android Gradle Plugin:** 8.13.0
- **Gradle:** 8.13
- **JDK:** 17 (Android Studio bundled JBR is supported).
- **Compile SDK:** 36
- **Target SDK:** 36
- **Minimum SDK:** 24 (Android 7.0+).

**Important:** Android Studio Narwhal supports AGP up to 8.13. If prompted to upgrade to AGP 9.x, decline the upgrade.

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/DilmiRathnayake/ConnectHub.git
```

Alternatively, download and extract the repository ZIP file.

### 2. Open in Android Studio

1. Launch Android Studio.
2. Select **File → Open**.
3. Navigate to the extracted project directory.
4. Select the folder containing `settings.gradle.kts`.
5. Wait for Gradle Sync to complete.

The first sync requires an internet connection to download Gradle and project dependencies.

### 3. Configure Android SDK

1. Navigate to **Tools → SDK Manager**.
2. Open the SDK Platforms tab.
3. Install Android SDK Platform 36 if it is not already installed.

Android Studio automatically creates `local.properties` with your SDK location.

### 4. Create an Emulator

1. Open **Device Manager**.
2. Select **Create Virtual Device**.
3. Choose a Pixel 8 device.
4. Select an API 36 Google APIs or Google Play system image.
5. Finish the emulator configuration.

Google APIs or Google Play images are required for the Blogger Google sign-in functionality.

### 5. Run the Application

1. Select the `app` run configuration.
2. Select your emulator or connected Android device.
3. Click **Run ▶**.

The application will be installed and launched on your selected device.

---

## 💻 Running from the Command Line

Open the Android Studio Terminal in the project directory.

**Build the application:**

```bash
gradlew assembleDebug
```

A successful build generates the APK at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

**Run JVM unit tests:**

```bash
gradlew testDebugUnitTest
```

Test report:

```text
app/build/reports/tests/testDebugUnitTest/index.html
```

**Run Android instrumented tests:**

```bash
gradlew connectedDebugAndroidTest
```

An active emulator or connected Android device is required.

---

## 🔗 Blogger Publishing Setup

ConnectHub supports publishing blog posts directly to Blogger through Google authentication and the Blogger REST API.

Publishing requires a one-time configuration for each Google account and development PC.

For detailed configuration instructions, refer to:

**[Blogger Publishing Setup Guide](docs/PUBLISHING_SETUP.md)**

Without the publishing configuration, users can still explore the upload workflow using **Test Mode**, available under Upload → Settings.

Test Mode is clearly labelled as simulated and does not publish actual posts.

---

## 🏗️ Project Structure

```text
ConnectHub/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/blogspace/app/
│   │   │   │
│   │   │   ├── ConnectHubApp
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── Post
│   │   │   │   ├── BlogDbHelper
│   │   │   │   ├── PostRepository
│   │   │   │   ├── PostFilter
│   │   │   │   ├── PostValidator
│   │   │   │   └── SearchQuery
│   │   │   │
│   │   │   ├── ui/
│   │   │   │   ├── SplashActivity
│   │   │   │   ├── OnboardingActivity
│   │   │   │   ├── ProfileSetupActivity
│   │   │   │   ├── MainActivity
│   │   │   │   ├── HomeFragment
│   │   │   │   ├── PostsFragment
│   │   │   │   ├── UploadFragment
│   │   │   │   ├── GalleryFragment
│   │   │   │   ├── ProfileFragment
│   │   │   │   ├── SearchActivity
│   │   │   │   ├── PostDetailActivity
│   │   │   │   ├── EditPostActivity
│   │   │   │   ├── ImagePreviewActivity
│   │   │   │   ├── PublishSettingsActivity
│   │   │   │   ├── AboutActivity
│   │   │   │   ├── ShareSheet
│   │   │   │   ├── PostAdapter
│   │   │   │   └── Navigation
│   │   │   │
│   │   │   ├── ui/widget/
│   │   │   │   ├── StatusBadge
│   │   │   │   ├── SwipeActions
│   │   │   │   ├── Shimmer
│   │   │   │   ├── SettingRowView
│   │   │   │   └── SquareFrameLayout
│   │   │   │
│   │   │   ├── util/
│   │   │   │   ├── ImageStorage
│   │   │   │   ├── ThumbnailLoader
│   │   │   │   ├── AvatarHelper
│   │   │   │   ├── ShareHelper
│   │   │   │   ├── ShareText
│   │   │   │   ├── AppPrefs
│   │   │   │   ├── SearchHistory
│   │   │   │   ├── AppExecutors
│   │   │   │   └── UiUtils
│   │   │   │
│   │   │   └── publish/
│   │   │       ├── PublishController
│   │   │       ├── GoogleAuthHelper
│   │   │       ├── BloggerApi
│   │   │       ├── BloggerPublishService
│   │   │       ├── MockPublishService
│   │   │       └── PublishSettings
│   │   │
│   │   ├── test/           # JVM unit tests (24)
│   │   └── androidTest/    # Instrumented tests (23)
│   │
│   └── build.gradle.kts
│
├── docs/
│   ├── screenshots/
│   ├── PUBLISHING_SETUP.md
│   ├── DOCUMENTATION.md
│   ├── design/
│   ├── diagrams/
│   └── licenses/
│
├── gradle/
│   └── libs.versions.toml
│
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
└── README.md
```

---

## 🧪 Testing

ConnectHub includes automated tests covering the application's main functionality.

| Test Category | Coverage |
|---|---|
| JVM Unit Tests | 24 tests |
| Instrumented Tests | 23 tests |
| Database Testing | SQLite operations and v1-to-v2 migration |
| Image Testing | Image storage and attachment handling |
| Sharing Testing | Android share intent functionality |
| UI Testing | Espresso-based UI tests |

**Total: 47 automated tests.**

---

## 🎨 Design System

ConnectHub follows a consistent visual design system with a modern interface.

**Colour Palette**

- Indigo: `#4F46E5`
- Purple: `#7C3AED`
- Violet: `#6366F1`
- Additional success, warning and error status colours.

**Typography**

Inter typeface with Regular, Medium, SemiBold and Bold weights.

**UI Components**

- Gradient hero headers.
- Glass-style status chips.
- Rounded and elevated cards.
- Animated press-scale interactions.
- Status badges.
- Shimmer loading skeletons.
- Rounded bottom sheets and dialogs.
- Scroll-responsive Extended Floating Action Button.

**Theme Support**

Light, Dark and System theme preferences with dedicated dark-mode colour resources.

---

## 📦 Third-Party Libraries & Licences

The project uses official Google and AndroidX libraries.

| Library / Resource | Licence |
|---|---|
| AndroidX AppCompat | Apache 2.0 |
| AndroidX Activity | Apache 2.0 |
| AndroidX Core | Apache 2.0 |
| AndroidX RecyclerView | Apache 2.0 |
| AndroidX ConstraintLayout | Apache 2.0 |
| AndroidX ExifInterface | Apache 2.0 |
| Material Components for Android | Apache 2.0 |
| Google Play Services Auth | Google APIs Terms of Service |
| AndroidX ViewPager2 | Apache 2.0 |
| Material Design Icons | Apache 2.0 |
| Inter Typeface – Rasmus Andersson | SIL Open Font Licence 1.1 |

Testing libraries include JUnit 4, AndroidX Test, Espresso and org.json.

Additional licence information is available in `docs/licenses/`.

---

## 📁 Preparing the Submission ZIP

Before creating the final submission ZIP:

1. Run **Build → Clean Project** in Android Studio.
2. Remove unnecessary build and IDE-generated files.
3. Keep a copy of the generated APK if a binary submission is required.
4. Include the project source code and required documentation.

Exclude the following generated files and directories:

```text
.gradle/
.idea/
build/
app/build/
local.properties
*.iml
```

The final submission can include:

- Android Studio project source code.
- Project documentation (PDF/Word).
- APK file (`app-debug.apk`), if required.
- Relevant project resources.

---

## 👩‍💻 Developer

**Developed by:** Dilmi Rathnayake

**Project:** ConnectHub – Offline & Online Social Blogging App

**Module:** 6CS027 – Secure Mobile Application Development

**Degree:** BSc (Hons) Computer Science (Software Engineering)

---

⭐ If you find this project interesting, feel free to explore the repository and its features.

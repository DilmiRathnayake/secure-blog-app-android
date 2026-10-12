# ConnectHub — Software Design

Diagrams are written in Mermaid. Rendered PNGs are in `docs/diagrams/`.

## 1. Use cases

```mermaid
flowchart LR
    U((Blogger<br/>user))
    G[(Google /<br/>Blogger API)]
    E[[Email / other app]]
    subgraph ConnectHub app
        UC1([Create post])
        UC2([Edit post])
        UC3([View post / list])
        UC4([Delete post])
        UC5([Select & delete many])
        UC6([Search posts])
        UC7([Attach photo:<br/>gallery or camera])
        UC8([Share post])
        UC9([Publish one / many])
        UC10([Connect Blogger account])
        UC11([Save draft / filter by status])
        UC12([Browse photo gallery])
    end
    U --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC8 & UC9 & UC10 & UC11 & UC12
    UC1 -. includes .-> UC7
    UC2 -. includes .-> UC7
    UC8 --- E
    UC9 --- G
    UC10 --- G
```

## 2. Class diagram

```mermaid
classDiagram
    direction LR
    class Post {
        -long id
        -String title
        -String content
        -String imageFileName
        -long createdAt
        -long updatedAt
        -String remotePostId
        -String remoteUrl
        -long publishedAt
        -boolean draft
        +getStatus() Status
        +hasImage() boolean
        +isPublished() boolean
    }
    class BlogDbHelper {
        +onCreate(db)
        +onUpgrade(db, old, new)
    }
    class PostRepository {
        +insert(Post) long
        +update(Post) boolean
        +getById(long) Post
        +getAll(PostFilter) List~Post~
        +getUploadQueue() List~Post~
        +getWithImages() List~Post~
        +removeImage(long) boolean
        +search(String) List~Post~
        +delete(long) boolean
        +deleteMany(ids) int
        +markPublished(id, remoteId, url)
        +getStats() Stats
    }
    class PostValidator { +validateTitle() +validateContent() }
    class SearchQuery { +normalise() +toContainsPattern() }
    class ImageStorage {
        +importImage(ctx, Uri) String
        +createCameraTempFile() File
        +getShareUri(File) Uri
        +decodeSampled() Bitmap
    }
    class ShareHelper { +shareWithChooser() +shareByEmail() }
    class PublishController { +publish(List~Long~) }
    class PublishService { <<interface>> +publish(Post) PublishResult }
    class BloggerPublishService
    class MockPublishService
    class BloggerApi { +listMyBlogs(token) +createPost(token, blogId, title, html) }
    class GoogleAuthHelper { +requestToken() +getToken() +invalidate() }
    class TokenProvider { <<interface>> }
    class PublishSettings

    class PostFilter { <<enum>> NEWEST OLDEST UPLOADED OFFLINE DRAFT }
    class MainActivity
    class HomeFragment
    class PostsFragment
    class UploadFragment
    class GalleryFragment
    class ProfileFragment
    class SearchActivity
    class ShareSheet
    class PostDetailActivity
    class EditPostActivity
    class PostAdapter

    PostRepository --> BlogDbHelper
    PostRepository --> Post
    PostRepository ..> SearchQuery
    PostRepository ..> ImageStorage : deletes photos
    MainActivity *-- PublishController
    MainActivity o-- HomeFragment
    MainActivity o-- PostsFragment
    MainActivity o-- UploadFragment
    MainActivity o-- GalleryFragment
    MainActivity o-- ProfileFragment
    PostsFragment --> PostAdapter
    UploadFragment --> PostAdapter
    SearchActivity --> PostAdapter
    PostsFragment ..> PostFilter
    UploadFragment ..> PublishController
    GalleryFragment ..> PostRepository
    PostDetailActivity ..> ShareSheet
    ShareSheet ..> ShareHelper
    HomeFragment --> PostAdapter
    PostsFragment ..> PostRepository
    SearchActivity ..> PostRepository
    EditPostActivity ..> PostRepository
    EditPostActivity ..> PostValidator
    EditPostActivity ..> ImageStorage
    PostDetailActivity *-- PublishController
    PublishController ..> PublishService
    PublishService <|.. BloggerPublishService
    PublishService <|.. MockPublishService
    BloggerPublishService --> BloggerApi
    BloggerPublishService --> TokenProvider
    TokenProvider <|.. GoogleAuthHelper
    PublishController --> GoogleAuthHelper
    PublishController ..> PublishSettings
```

## 3. Database design (SQLite)

```mermaid
erDiagram
    POSTS {
        INTEGER _id PK "AUTOINCREMENT"
        TEXT title "NOT NULL"
        TEXT content "NOT NULL"
        TEXT image_file "NULL - file name in filesDir/images"
        INTEGER created_at "NOT NULL, epoch ms"
        INTEGER updated_at "NOT NULL, epoch ms, indexed DESC"
        TEXT remote_post_id "NULL - Blogger id after confirmed publish"
        TEXT remote_url "NULL"
        INTEGER published_at "NOT NULL DEFAULT 0"
        INTEGER is_draft "NOT NULL DEFAULT 0 (added in v2)"
    }
```

* One table, because a post has exactly one optional photo. Photos are stored as files, not BLOBs.
  This keeps the database small and lets FileProvider share the file directly.
* `remote_*` and `published_at` record only **confirmed** Blogger publishes.
* Schema version 2 adds `is_draft`; `onUpgrade` runs `ALTER TABLE … ADD COLUMN` so v1 posts are kept
  (covered by the `upgradeFromVersion1KeepsExistingPosts` instrumented test).
* Status shown in the UI: **Draft** (`is_draft = 1`), **Uploaded** (`published_at > 0`), otherwise **Offline**.
* Search: `title LIKE ? ESCAPE '\' OR content LIKE ? ESCAPE '\'` with a bound `%term%` pattern.
* Bulk delete runs inside one transaction, and photo files are removed only after the commit.

## 4. Screen flow

```mermaid
flowchart TD
    Start([Launch]) --> Splash[Splash]
    Splash -->|first run| Onb[Onboarding x3] --> Prof[Profile setup]
    Splash --> Main
    Prof --> Main
    subgraph Main [MainActivity - bottom navigation]
        Home[Home dashboard]
        Posts[Posts + filter chips]
        Upload[Upload queue]
        Gallery[Gallery grid]
        Profile[Profile & settings]
    end
    Home & Posts -->|search bar| Search[SearchActivity]
    Home & Posts -->|FAB| Edit[EditPostActivity]
    Home & Posts & Search -->|tap card| Detail[PostDetailActivity]
    Posts -->|swipe right| Edit
    Posts -->|swipe left| Undo{{Deleted - Undo?}}
    Posts -->|long-press| Sel[Selection bar: select all, upload, delete]
    Gallery -->|Camera / Gallery card| Edit
    Gallery -->|tap photo| Preview[ImagePreviewActivity]
    Detail -->|Edit| Edit
    Detail -->|Share| Sheet[ShareSheet]
    Detail & Upload & Sel -->|Upload| PubFlow[[PublishController + upload sheet]]
    Upload & Profile -->|Settings| PubSet[PublishSettingsActivity]
    Profile --> About[AboutActivity]
    Edit -->|Camera| Cam[Camera app via FileProvider]
    Edit -->|Gallery| Picker[System Photo Picker]
```

## 5. Publish sequence

```mermaid
sequenceDiagram
    actor User
    participant UI as PostDetail / Posts
    participant PC as PublishController
    participant GA as GoogleAuthHelper
    participant GPS as Google Play services
    participant API as Blogger API v3
    participant DB as PostRepository (SQLite)
    User->>UI: Publish
    UI->>PC: publish(ids)
    PC->>PC: check blog chosen + online, confirm dialog
    PC->>GA: requestToken()
    GA->>GPS: authorize(scope blogger)
    GPS-->>User: account / consent (first time only)
    GPS-->>GA: access token
    loop each selected post (background thread)
        PC->>API: POST /blogs/{id}/posts (Bearer token)
        alt 2xx
            API-->>PC: {id, url}
            PC->>DB: markPublished(id, remoteId, url)
        else 401
            PC->>GA: invalidate + get new token, retry once
        else 4xx/5xx or IOException
            PC->>PC: record friendly error (no DB change)
        end
    end
    PC-->>User: result list with URLs / errors
```

## 6. Key design decisions
| Decision | Reason |
|---|---|
| Copy every photo into `filesDir/images` as a scaled, EXIF-rotated JPEG | Gallery URIs and temporary camera files can stop working after a restart. A JPEG works for every share target, and HEIC/PNG/WebP all become one format |
| Photo Picker and camera intent, with **no** storage or CAMERA permissions | Least privilege, and no runtime permission prompts |
| FileProvider plus `FLAG_GRANT_READ_URI_PERMISSION` and ClipData | Gives the receiving app temporary read access to one file only |
| Repository is the only class with SQL; all arguments are bound | Prevents SQL injection, and keeps the UI code free of database logic |
| Background executors for SQLite, images and network | The UI thread never blocks |
| Blogger via Google AuthorizationClient (OAuth 2.0) | Official API. No secrets in the app, and Google handles the password |
| Test mode is a separate `PublishService` and never writes "published" | Development rehearsal cannot be mistaken for real publishing |
| Local profile instead of a login screen | There is no server account system; a fake login would be a non-functional placeholder |
| Swipe-delete is deferred until the Undo snackbar closes | Undo restores the exact row and photo, without re-inserting a copy |
| Share-sheet app shortcuts only for installed apps, with their real icons | No dead buttons; every target sends a real ACTION_SEND intent |

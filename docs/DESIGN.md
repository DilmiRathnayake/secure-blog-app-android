# BlogSpace — Software Design

Diagrams are written in Mermaid. Rendered PNGs are in `docs/diagrams/`.

## 1. Use cases

```mermaid
flowchart LR
    U((Blogger<br/>user))
    G[(Google /<br/>Blogger API)]
    E[[Email / other app]]
    subgraph BlogSpace app
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
    end
    U --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC8 & UC9 & UC10
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
        +getAll() List~Post~
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

    class MainActivity
    class HomeFragment
    class PostsFragment
    class SearchFragment
    class PublishFragment
    class AboutFragment
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
    MainActivity o-- SearchFragment
    MainActivity o-- PublishFragment
    MainActivity o-- AboutFragment
    PostsFragment --> PostAdapter
    SearchFragment --> PostAdapter
    HomeFragment --> PostAdapter
    PostsFragment ..> PostRepository
    SearchFragment ..> PostRepository
    EditPostActivity ..> PostRepository
    EditPostActivity ..> PostValidator
    EditPostActivity ..> ImageStorage
    PostDetailActivity ..> ShareHelper
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
    }
```

* One table, because a post has exactly one optional photo. Photos are stored as files, not BLOBs.
  This keeps the database small and lets FileProvider share the file directly.
* `remote_*` and `published_at` record only **confirmed** Blogger publishes.
* Search: `title LIKE ? ESCAPE '\' OR content LIKE ? ESCAPE '\'` with a bound `%term%` pattern.
* Bulk delete runs inside one transaction, and photo files are removed only after the commit.

## 4. Screen flow

```mermaid
flowchart TD
    Start([Launch]) --> Home
    subgraph MainActivity [MainActivity - bottom navigation]
        Home[Home dashboard]
        Posts[Posts list]
        Search[Search]
        Publish[Publish settings]
        About[About / Help]
    end
    Home -->|New post| Edit
    Posts -->|FAB New post| Edit
    Home & Posts & Search -->|tap post| Detail
    Posts -->|long-press / Select| Sel[Selection mode<br/>select all · clear · delete · publish]
    Sel -->|Delete + confirm| Posts
    Sel -->|Publish| PubFlow
    Detail[PostDetailActivity] -->|Edit| Edit[EditPostActivity]
    Detail -->|Delete + confirm| Posts
    Detail -->|Share| Share{Any app / Email}
    Detail -->|Publish| PubFlow[[PublishController]]
    Edit -->|Gallery| Picker[System Photo Picker]
    Edit -->|Camera| Cam[Camera app via FileProvider]
    Edit -->|Save| Detail
    PubFlow -->|not set up| Publish
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

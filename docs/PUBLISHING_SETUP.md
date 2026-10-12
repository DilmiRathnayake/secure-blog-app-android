# Blogger publishing — one-time setup

ConnectHub publishes posts to **Blogger** (blogger.com) through the official **Blogger API v3**, using
Google sign-in (OAuth 2.0) provided by Google Play services.

**No password, API key, client secret or token is stored in the source code.** An Android OAuth
client is identified only by the app's **package name** and its **signing certificate SHA-1**. So the
setup below has to be done once by the account owner, in their own Google Cloud project.

> The OAuth client is tied to the SHA-1 of the keystore that signs the APK. The Android debug keystore
> is different on every computer. **Register the SHA-1 of the Windows PC that runs the app.** You can
> add several Android clients (one per computer) to the same project.

---

## 1. Create a blog
1. Go to <https://www.blogger.com> and sign in with your Google account.
2. Create a blog, e.g. `yourname-blogspace.blogspot.com`.

## 2. Get the SHA-1 of your debug keystore (on the Windows PC)
In Android Studio, open the **Terminal** tab (bottom of the window). It opens in the project folder.
Run:

```
gradlew signingReport
```

In the output, find `Variant: debug` and copy the line `SHA1: AA:BB:...`.

Another way, from **Command Prompt**:

```
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

If `keytool` is not found, use Android Studio's own copy:
`"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"`.

## 3. Google Cloud project
1. Open <https://console.cloud.google.com/> and create a project (e.g. `ConnectHub`).
2. **APIs & Services → Library**, search for **Blogger API v3**, then click **Enable**.
3. **Google Auth Platform** (formerly "OAuth consent screen"):
   * **Branding**: app name `ConnectHub`, your email as the support and developer contact.
   * **Audience**: User type **External**. Leave the publishing status as **Testing**.
     Under **Test users**, click **Add users** and add the Google account that owns your blog.
   * **Data access**: **Add or remove scopes**, then add
     `https://www.googleapis.com/auth/blogger`.
4. **Clients → Create client**:
   * Application type: **Android**
   * Package name: `com.blogspace.app`
   * SHA-1 certificate fingerprint: the value from step 2
   * Click **Create**. Android clients have **no secret**, so nothing needs to go into the code.

Changes can take a few minutes to start working.

## 4. Device or emulator
* A real phone with Google Play services and your Google account signed in, **or**
* An emulator with a **Google APIs** or **Google Play** system image. In the emulator, add your
  Google account under Settings → Passwords & accounts (or Accounts).

## 5. Connect in the app
1. Open ConnectHub, go to **Upload → Settings** (or **Profile → Publishing account**) and select **Blogger — real publishing**.
2. Tap **Connect Google account & choose blog**, pick your account, and allow
   "Manage your Blogger account".
3. Choose your blog from the list. The card now shows "Publishing to: <blog name>".
4. Open any post and tap **Upload**, select several posts in **Posts** and tap the cloud icon, or tick
   posts in the **Upload** tab queue and tap **Upload N posts**.
5. The upload sheet shows a row per post with its Blogger URL, then a success tick. The post now shows a
   green **Uploaded** badge and **View online** on its detail screen. Drafts are never uploaded.

## Troubleshooting

| Message | Cause | Fix |
|---|---|---|
| `DEVELOPER_ERROR 10` | Package name or SHA-1 doesn't match the Android OAuth client | Re-check step 2 and step 3.4 for **this** PC's keystore |
| "Access blocked: app has not completed verification" | Your account is not a test user | Add it under Audience → Test users |
| "Blogger refused the request: Blogger API has not been used…" | API not enabled | Enable Blogger API v3 (step 3.2) |
| "This Google account has no Blogger blogs" | No blog yet | Create one at blogger.com |
| "Network problem" | No internet | Reconnect, then try again. Posts stay saved offline |
| "Too many requests" | Blogger rate limit | Wait a few minutes |

## Notes and limitations
* **Photos:** the Blogger API has no image-upload endpoint. When "Include the post photo" is on,
  the app embeds a reduced JPEG (max 1000 px) directly in the post HTML. If Blogger rejects that,
  the post is re-sent without the photo and the result says "published without it".
* **Test mode** (Upload → Settings) rehearses the flow with no account. Its results are always labelled
  **Simulated**, and posts are never marked as published.
* **Disconnect** forgets the chosen blog on this device. To revoke Google access completely, visit
  <https://myaccount.google.com/permissions>.

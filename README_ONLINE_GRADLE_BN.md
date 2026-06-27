# Online Gradle দিয়ে APK Build করার নিয়ম

এই project-এ GitHub Actions workflow যোগ করা আছে:

`.github/workflows/android-debug-apk.yml`

## কীভাবে APK build করবেন

1. GitHub-এ নতুন repository খুলুন।
2. এই ZIP-এর ভেতরের সব file repository-তে upload করুন।
3. GitHub repository-তে **Actions** tab খুলুন।
4. **Build Android Debug APK** workflow select করুন।
5. **Run workflow** চাপুন।
6. Build শেষ হলে নিচে **Artifacts** section থেকে `NamazDndApp-debug-apk` download করুন।
7. ZIP unzip করলে debug APK পাবেন।

## Notes

- এই workflow cloud/server-এ Gradle দিয়ে `:app:assembleDebug` চালায়।
- Debug APK testing-এর জন্য ভালো। Play Store-এ দিতে হলে signed release APK/AAB লাগবে।
- App install করার পর DND access এবং alarm permission user-কে দিতে হবে।

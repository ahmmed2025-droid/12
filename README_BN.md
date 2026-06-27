# Namaz DND Android App

এই app-টি Android ফোনের জন্য বানানো। Feature:

- Fajr, Dhuhr, Asr, Maghrib, Isha — ৫ ওয়াক্তের সময় manual input করা যাবে।
- প্রতিটি ওয়াক্ত আলাদা করে On/Off করা যাবে।
- নামাজের ৫ মিনিট আগে phone Do Not Disturb mode-এ যাবে।
- Default ১৫ মিনিট পরে DND off হবে, user মিনিট change করতে পারবে।
- ফোন restart হলে app saved সময় অনুযায়ী alarm আবার schedule করবে।
- DND চালুর আগে ফোনের আগের interruption filter save করা হয়; সময় শেষ হলে সেটাই restore করার চেষ্টা করে।

## Android Studio দিয়ে run করার নিয়ম

1. Android Studio খুলুন।
2. **Open** চাপুন।
3. `NamazDndApp` folder select করুন।
4. Gradle sync complete হতে দিন।
5. Phone connect করে **Run** চাপুন।

## App চালানোর পর যেসব permission দিতে হবে

1. **DND Access Permission দিন** button চাপুন।
   - Settings খুলবে।
   - `Namaz DND` app খুঁজে permission allow করুন।
2. Android 12+ / 14+ হলে **Alarms & reminders Permission দিন** button চাপুন।
   - Permission allow করলে namaz time-এর ৫ মিনিট আগে alarm বেশি exact হবে।
   - Permission না দিলে app fallback alarm ব্যবহার করবে; কিছু ফোনে কয়েক মিনিট late হতে পারে।

## Important limitations

- iOS/iPhone-এ এই same কাজ করা যায় না, কারণ third-party app দিয়ে DND programmatically on/off করার অনুমতি নেই।
- Android-এ user manually DND special access না দিলে app DND change করতে পারবে না।
- কিছু brand যেমন Xiaomi/Oppo/Vivo battery optimization-এর কারণে background alarm delay করতে পারে। দরকার হলে Battery Optimization থেকে app allowlist করতে হবে।

## কোথায় code আছে

- Main UI: `app/src/main/java/com/example/namazdnd/MainActivity.kt`
- Alarm scheduling: `PrayerScheduler.kt`
- DND on/off receiver: `DndAlarmReceiver.kt`
- Restart-এর পরে reschedule: `BootReceiver.kt`
- Saved settings: `WaqtPrefs.kt`

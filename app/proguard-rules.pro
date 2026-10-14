# Ultra-compact ProGuard rules for minimal APK
-keepattributes *Annotation*
-dontwarn **
-repackageclasses 'o'
-allowaccessmodification

-keep class com.example.** { *; }
-keep class androidx.biometric.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}

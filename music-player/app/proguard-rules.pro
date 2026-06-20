# keep media3 + smb stack classes referenced via reflection
-keep class androidx.media3.** { *; }
-keep class com.hierynomus.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class org.apache.commons.net.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**

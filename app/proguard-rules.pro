# نادیده گرفتن کلاس‌های گم‌شده در کتابخانه‌های mbassador و smbj
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-keep class javax.el.** { *; }
-keep class org.ietf.jgss.** { *; }

# نگه‌داری کلاس‌های مورد نیاز برای انعکاس (Reflection) در کتابخانه‌های SMB
-keep class com.hierynomus.** { *; }
-keep class net.engio.mbassy.** { *; }

# جلوگیری از حذف اعضای داخلی کتابخانه‌های network
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# قوانین عمومی برای保持 کلاس‌های اصلی برنامه
-keep class com.vibemusic.app.** { *; }
-keepclassmembers class com.vibemusic.app.** { *; }

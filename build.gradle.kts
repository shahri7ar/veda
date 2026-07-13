android {
    // ... سایر تنظیمات ...
    
    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")  // اگر signingConfig تعریف کرده‌اید
        }
    }
}

// اگر signingConfig تعریف نشده، می‌توانید از debug signing استفاده کنید (برای تست)
// در غیر این صورت، یک signingConfig تعریف کنید:
signingConfigs {
    create("release") {
        storeFile = file("vibemusic-release.keystore")
        storePassword = "vibemusic"
        keyAlias = "vibemusic"
        keyPassword = "vibemusic"
    }
}

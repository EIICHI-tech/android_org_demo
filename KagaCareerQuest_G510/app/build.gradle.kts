plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "jp.demo.kagacareerquest"
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.demo.kagacareerquest"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

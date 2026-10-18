plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "jp.co.demo.pachinkolamp"
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.co.demo.pachinkolamp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { buildConfig = false }

    kotlinOptions { jvmTarget = "17" }
}

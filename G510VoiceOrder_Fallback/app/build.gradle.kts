plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kagafei.g510.voiceorder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kagafei.g510.voiceorder"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0-g510-fallback"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "jp.co.demo.pachinkodatalamp"
    compileSdk = 35
    defaultConfig {
        applicationId = "jp.co.demo.pachinkodatalamp"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
kotlin { jvmToolchain(17) }

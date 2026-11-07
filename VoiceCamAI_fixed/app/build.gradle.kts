plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace="com.example.voicecamai"; compileSdk=36
 defaultConfig { applicationId="com.example.voicecamai"; minSdk=26; targetSdk=36; versionCode=2; versionName="1.1-fixed" }
 buildFeatures { viewBinding=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.appcompat:appcompat:1.7.1")
 implementation("com.google.android.material:material:1.12.0")
 implementation("androidx.constraintlayout:constraintlayout:2.2.1")
 val cx="1.6.2"
 implementation("androidx.camera:camera-core:$cx")
 implementation("androidx.camera:camera-camera2:$cx")
 implementation("androidx.camera:camera-lifecycle:$cx")
 implementation("androidx.camera:camera-view:$cx")
}

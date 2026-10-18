plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace="jp.co.kagafei.earmg510"; compileSdk=35
 defaultConfig { applicationId="jp.co.kagafei.earmg510.usb"; minSdk=26; targetSdk=35; versionCode=2; versionName="2.0-usb" }
 buildFeatures { compose=true }
 compileOptions {
     sourceCompatibility = JavaVersion.VERSION_17
     targetCompatibility = JavaVersion.VERSION_17
 }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01")); implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"); implementation("com.github.mik3y:usb-serial-for-android:3.11.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
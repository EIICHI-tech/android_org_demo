plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace="com.example.hospitalvoice"; compileSdk=35
 defaultConfig { applicationId="com.example.hospitalvoice"; minSdk=29; targetSdk=35; versionCode=3; versionName="1.2-command-fix" }
 buildFeatures { compose=true }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
 }
 kotlinOptions {
  jvmTarget = "1.8"
 }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 debugImplementation("androidx.compose.ui:ui-tooling")
}

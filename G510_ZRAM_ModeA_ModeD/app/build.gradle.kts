plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace="jp.g510.zramcompare"; compileSdk=35
 defaultConfig { minSdk=29; targetSdk=35; versionCode=3; versionName="3.0" }
 flavorDimensions += "zramMode"
 productFlavors {
   create("modeA") {
     dimension="zramMode"
     applicationId="jp.g510.zramcompare.modea"
     buildConfigField("String","MODE","\"A\"")
     buildConfigField("Boolean","IS_BASELINE","true")
     resValue("string","app_name","G510 zRAM OFF - ModeA")
   }
   create("modeD") {
     dimension="zramMode"
     applicationId="jp.g510.zramcompare.moded"
     buildConfigField("String","MODE","\"D\"")
     buildConfigField("Boolean","IS_BASELINE","false")
     resValue("string","app_name","G510 zRAM ON - ModeD")
   }
 }
 buildFeatures { compose=true; buildConfig=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
 applicationVariants.all {
   val mode = if (flavorName == "modeA") "ModeA_ZRAM_OFF" else "ModeD_ZRAM_ON"
   outputs.all {
     val output = this as com.android.build.gradle.api.ApkVariantOutput
     output.outputFileName = "G510_${mode}_${buildType.name}.apk"
   }
 }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.ui:ui")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
}

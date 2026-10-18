plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

allprojects {
    layout.buildDirectory.set(file("C:/tmp/VoiceSelfOrderDemo/${project.name}"))
}

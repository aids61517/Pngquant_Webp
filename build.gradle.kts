import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
}

group = "aids61517.pngquant"
version = "1.0.2"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

sourceSets {
    getByName("main").resources.srcDirs("resources")
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    implementation("com.squareup.okio:okio:3.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "aids61517.pngquant.MainWindow"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PngquantToWebp"
            packageVersion = "1.0.2"
        }
    }
}
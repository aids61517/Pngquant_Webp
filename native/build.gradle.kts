//import org.jetbrains.compose.compose
//import org.jetbrains.compose.desktop.application.dsl.TargetFormat
//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    mingwX64()
    sourceSets {
//        val jvmMain by getting {
//            dependencies {
////                implementation(compose.desktop.currentOs)
//            }
//        }

        val commonMain by getting

        val mingwX64Main by getting
    }
}

//compose.desktop {
//    application {
//        mainClass = "MainKt"
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "native"
//            packageVersion = "1.0.0"
//        }
//    }
//}

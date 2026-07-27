plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
}

android {
    namespace = "dev.danielc"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.danielc"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nobody uses 32bit x86 anymore, disabling 32bit arm for now
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    flavorDimensions += "buildType"
    productFlavors {
        create("stable") {
            dimension = "buildType"
            applicationId = "dev.danielc.fantasyfudge"
            resValue("string", "app_name", "FantasyFudge")
        }
//        create("playstore") {
//            dimension = "buildType"
//            applicationId = "dev.danielc.fantasyfudge.playstore"
//            resValue("string", "app_name", "FantasyFudge")
//        }
    }

    buildTypes {
        release {
            isDebuggable = true
            isJniDebuggable = true
            ndk {
                debugSymbolLevel = "FULL"
            }
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("../ndk/CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
    }
}

tasks.register<Exec>("compileModules") {
    group = "verification"
    description = "Compiles and copies modules"
    doNotTrackState("Always run this script during compilation")
    commandLine("bash", "-c", "cd ../modules && make install")
}
tasks.register<Exec>("compileLibs") {
    group = "verification"
    description = "Compiles and copies modules"
    doNotTrackState("Always run this script during compilation")
    commandLine("bash", "-c", "cd ../../pak/src/typescript && make")
}
tasks.named("preBuild") {
    dependsOn("compileModules", "compileLibs")
}

dependencies {
    rootProject.extra["noNativeModule"] = true
    implementation(project(":libpak"))

    // libs
    implementation(libs.zoomable)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Test stuff
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // The core runner engine (matches the string in defaultConfig)
    androidTestImplementation("androidx.test:runner:1.6.2")
    // The modern JUnit4 extension (provides the new AndroidJUnit4 class)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

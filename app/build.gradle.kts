plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.eyesmemory"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.eyesmemory"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore:24.7.1")
    implementation("com.google.firebase:firebase-analytics")

    // ML Kit Pose Detection
    implementation("com.google.mlkit:pose-detection:18.0.0-beta5")

    // CameraX
    implementation("androidx.camera:camera-core:1.1.0-alpha05")
    implementation("androidx.camera:camera-camera2:1.1.0-alpha05")
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha05")
    implementation("androidx.camera:camera-view:1.0.0-alpha25")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(files("libs/gazetracker-release.aar"))
    implementation(files("libs/libgaze-release.aar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(project(":view"))
}
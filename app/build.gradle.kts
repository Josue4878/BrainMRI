plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.braintumorlab15"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.braintumorlab15"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk {
            // Flex is large: only package the common physical Android phone ABIs.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Loading the model through Assets.open() also works if packaging compresses it.
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // Keep both versions identical. The original exported model uses Flex ops.
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room3")
}

android {
    namespace = "com.goreecloud.since"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.goreecloud.since"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.room3:room3-runtime:3.0.3")
    implementation("androidx.sqlite:sqlite-framework:2.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    ksp("androidx.room3:room3-compiler:3.0.3")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.2")

    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.8.2")
}

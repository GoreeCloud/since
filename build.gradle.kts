plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room3")
}

val explicitDevelopmentVersionCode =
    providers.environmentVariable("GOREECLOUD_DEV_VERSION_CODE").orNull?.takeIf { it.isNotBlank() }
val ciDevelopmentVersionCode =
    providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.takeIf { it.isNotBlank() }

fun positiveVersionCode(name: String, value: String): Int =
    value.toIntOrNull()?.takeIf { it > 0 }
        ?: throw org.gradle.api.GradleException("$name must be a positive Android versionCode.")

val developmentVersionCode =
    when {
        explicitDevelopmentVersionCode != null ->
            positiveVersionCode("GOREECLOUD_DEV_VERSION_CODE", explicitDevelopmentVersionCode)
        ciDevelopmentVersionCode != null ->
            positiveVersionCode("GITHUB_RUN_NUMBER", ciDevelopmentVersionCode)
        else -> 1
    }

val developmentKeystorePath =
    providers.environmentVariable("GOREECLOUD_DEV_KEYSTORE_PATH").orNull?.takeIf { it.isNotBlank() }
val developmentKeystorePassword =
    providers.environmentVariable("GOREECLOUD_DEV_KEYSTORE_PASSWORD").orNull?.takeIf { it.isNotBlank() }
val developmentKeyAlias =
    providers.environmentVariable("GOREECLOUD_DEV_KEY_ALIAS").orNull?.takeIf { it.isNotBlank() }
val developmentKeyPassword =
    providers.environmentVariable("GOREECLOUD_DEV_KEY_PASSWORD").orNull?.takeIf { it.isNotBlank() }

val developmentSigningValues =
    listOf(
        developmentKeystorePath,
        developmentKeystorePassword,
        developmentKeyAlias,
        developmentKeyPassword,
    )
val developmentSigningRequested = developmentSigningValues.any { it != null }
val developmentSigningConfigured = developmentSigningValues.all { it != null }

if (developmentSigningRequested && !developmentSigningConfigured) {
    throw org.gradle.api.GradleException(
        "Development signing configuration is incomplete. Provide all GOREECLOUD_DEV_KEYSTORE_* " +
            "environment variables or none of them.",
    )
}

android {
    namespace = "com.goreecloud.since"
    compileSdk = 36

    signingConfigs {
        if (developmentSigningConfigured) {
            create("development") {
                storeFile = file(requireNotNull(developmentKeystorePath))
                storePassword = requireNotNull(developmentKeystorePassword)
                keyAlias = requireNotNull(developmentKeyAlias)
                keyPassword = requireNotNull(developmentKeyPassword)
            }
        }
    }

    defaultConfig {
        applicationId = "com.goreecloud.since"
        minSdk = 29
        targetSdk = 36
        versionCode = developmentVersionCode
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            if (developmentSigningConfigured) {
                signingConfig = signingConfigs.getByName("development")
            }
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

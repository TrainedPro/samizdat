import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("${rootProject.projectDir}/detekt.yml"))
    basePath = rootProject.projectDir.absolutePath
}

// Read local.properties for secrets (Firebase keys, etc.)
val localProperties = Properties()
rootProject.file("local.properties").let { f ->
    if (f.exists()) f.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.fyp.resilientp2p"
    compileSdk = 36

    // Allow test_mode property: ./gradlew assembleDebug -Ptest_mode=true
    val isTestMode = project.findProperty("test_mode")?.toString()?.toBoolean() ?: false

    // Firebase config from local.properties (never committed)
    val firebaseProjectId = localProperties.getProperty("FIREBASE_PROJECT_ID", "")
    val firebaseApiKey = localProperties.getProperty("FIREBASE_API_KEY", "")

    defaultConfig {
        applicationId = "com.fyp.resilientp2p"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "TEST_MODE", isTestMode.toString())
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Our core dependency for the P2P communication
    implementation(libs.play.services.nearby)

    // Material (required for Theme.MaterialComponents in themes.xml)
    implementation(libs.material)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coil (image loading for chat thumbnails)
    implementation(libs.coil.compose)

    // WorkManager (periodic telemetry upload)
    implementation(libs.androidx.work.runtime)
}
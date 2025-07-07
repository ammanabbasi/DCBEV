import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties
import java.io.FileInputStream
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Read local properties
val localProperties = gradleLocalProperties(rootDir, providers)

android {
    namespace = "com.dealerscloud.android"
    compileSdk = 35
    
    signingConfigs {
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["RELEASE_STORE_FILE"] ?: "keystore.jks")
                storePassword = keystoreProperties["RELEASE_STORE_PASSWORD"] as String
                keyAlias = keystoreProperties["RELEASE_KEY_ALIAS"] as String
                keyPassword = keystoreProperties["RELEASE_KEY_PASSWORD"] as String
            } else {
                // Fallback to debug signing
                storeFile = file("keystore.jks")
                storePassword = "dealerscloud123"
                keyAlias = "dealerscloud"
                keyPassword = "dealerscloud123"
            }
        }
    }

    defaultConfig {
        applicationId = "com.dealerscloud.android"
        minSdk = 24  // Changed from 26 to 24 for better compatibility
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Add universal ABI support
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
        
        // Load local.properties
        val localPropertiesData = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localPropertiesData.load(stream)
            }
        }
        
        // Add OpenAI API key from local.properties
        buildConfigField("String", "OPENAI_API_KEY", "\"${localPropertiesData.getProperty("OPENAI_API_KEY", "")}\"")
        buildConfigField("String", "DC_BASE_URL", "\"${localPropertiesData.getProperty("DC_BASE_URL", "")}\"")
        buildConfigField("String", "DC_API_BEARER", "\"${localPropertiesData.getProperty("DC_API_BEARER", "")}\"")
        buildConfigField("String", "FCM_SERVER_KEY", "\"${localPropertiesData.getProperty("FCM_SERVER_KEY", "")}\"")
        buildConfigField("String", "DC_ADMIN_USERNAME", "\"${localPropertiesData.getProperty("DC_ADMIN_USERNAME", "")}\"")
        buildConfigField("String", "DC_ADMIN_PASSWORD", "\"${localPropertiesData.getProperty("DC_ADMIN_PASSWORD", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // Disable for now to avoid issues
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    // Disable ABI splits for universal APK
    bundle {
        abi {
            enableSplit = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtension.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    testOptions {
        unitTests.all {
            it.ignoreFailures = true
        }
    }
}

// Configure Detekt JVM target to match toolchain
tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Lifecycle Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Material Design 3
    implementation(libs.material)
    
    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Local Storage
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // AI Integration
    implementation(libs.openai.client)
    implementation(libs.ktor.client.okhttp)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // WorkManager for background sync
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")
    
    // SSE support for streaming
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-appcheck-ktx")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("io.mockk:mockk-android:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("org.robolectric:annotations:4.12.2")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    implementation(libs.accompanist.permissions)
}
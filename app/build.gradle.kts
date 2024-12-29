plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.teledyneflir.androidsdk.sample.flironewireless"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.teledyneflir.androidsdk.sample.flironewireless"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    repositories {
        // default path where thermalsdk AAR is stored
        flatDir {
            dirs = setOf(File("../../../modules/thermalsdk/build/outputs/aar"))
        }
        // default path where androidsdk AAR is stored
        flatDir {
            dirs = setOf(File("../../../modules/androidsdk/build/outputs/aar"))
        }
        // superproject path where all required AARs are stored (for local debug builds only)
        flatDir {
            dirs = setOf(File("../../../MastodonAndroid/prebuilt-aar"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)

    // add Atlas Android SDK 'aar' library located under 'modules/thermalsdk/build/outputs/aar'
    // add Atlas Android SDK 'aar' library located under 'modules/thermalsdk/build/outputs/aar'
    implementation(files("libs/androidsdk-release.aar"))
    implementation(files("libs/thermalsdk-release.aar"))
}
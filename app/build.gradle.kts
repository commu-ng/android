plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("io.sentry.android.gradle") version "5.12.2"
}

android {
    namespace = "ng.commu"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ng.commu"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.activity:activity-ktx:1.11.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-messaging")

    // WebView
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

sentry {
    org.set("limeburst")
    projectName.set("commu-ng-android")
    includeSourceContext.set(true)
}

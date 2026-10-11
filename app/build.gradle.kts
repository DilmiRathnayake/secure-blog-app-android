plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.blogspace.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.blogspace.app"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.core)
    implementation(libs.exifinterface)
    // Google sign-in/authorisation, used only for publishing to Blogger.
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    // Real org.json implementation so JSON code can be unit-tested on the JVM.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)
}
